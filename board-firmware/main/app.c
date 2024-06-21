#include <sys/cdefs.h>
#include <stdlib.h>
#include <string.h>
#include <rom/ets_sys.h>
#include <esp_log.h>

#include "network.h"
#include "crc8.h"
#include "avrisp.h"
#include "node_config.h"
#include "device.h"

#define TAG "device.c"

struct flashing_info {
	bool is_active;
	uint8_t target_node;
	uint16_t chunk_count;
	struct chunk *chunks;
} flashing_info;


static esp_err_t handle_flash_begin(struct flash_begin_req *flash_begin, void *_) {
	esp_err_t result = ESP_OK;

	if ((result = load_node_hw(flash_begin->node_id, NULL)) != ESP_OK) {
		return result;
	}

	struct chunk *chunks_list = calloc(sizeof(*flashing_info.chunks), flash_begin->total_chunks);
	if (!chunks_list) {
		return ESP_ERR_NO_MEM;
	}

	if (flashing_info.is_active) {
		free(chunks_list);
		return ESP_ERR_INVALID_STATE;
	}

	flashing_info = (struct flashing_info) {
			.is_active = true,
			.target_node = flash_begin->node_id,
			.chunk_count = flash_begin->total_chunks,
			.chunks = chunks_list
	};

	return ESP_OK;
}

static esp_err_t handle_flash_data(struct flash_data_req *flash_data, void *_) {
	if (!flashing_info.is_active) {
		return ESP_ERR_INVALID_STATE;
	}

	if (flash_data->chunk_idx >= flashing_info.chunk_count) {
		return ESP_ERR_INVALID_ARG;
	}

	if (crc8(flash_data->chunk, flash_data->chunk_size) != flash_data->crc) {
		return ESP_ERR_INVALID_CRC;
	}

	struct chunk *chunk = &flashing_info.chunks[flash_data->chunk_idx];
	if (chunk->data) {
		return ESP_ERR_INVALID_STATE;
	}

	uint8_t *data = calloc(flash_data->chunk_size, 1);
	if (!data) {
		return ESP_ERR_NO_MEM;
	}

	memcpy(data, flash_data->chunk, flash_data->chunk_size);
	*chunk = (struct chunk) {
			.size = flash_data->chunk_size,
			.start_offset = flash_data->chunk_offset,
			.data = data
	};

	return ESP_OK;
}

static esp_err_t handle_flash_data_end(void *_, void * __) {
	esp_err_t error;

	if (!flashing_info.is_active) {
		return ESP_ERR_INVALID_STATE;
	}

	flashing_info.is_active = false;
	struct node_spi_config target;
	if ((error = load_node_hw(flashing_info.target_node, &target)) != ESP_OK)
		return error;

	if ((error = program(&target, flashing_info.chunk_count, flashing_info.chunks)) != ESP_OK)
		return error;

	return ESP_OK;
}

static esp_err_t handle_configure_shutdown(struct cfg_shtdn_req *req, void *_) {
	struct node_spi_config config;
	esp_err_t error = load_node_hw(req->node_id, &config);
	if (error != ESP_OK) {
		return error;
	}

	return set_shutdown(&config, req->shutdown);
}

static esp_err_t handle_configure_engagement(struct cfg_engage_req *req, void *_) {
	struct node_spi_config config;
	esp_err_t error = load_node_hw(req->node_id, &config);
	if (error != ESP_OK) {
		return error;
	}

	return set_engaged(&config, req->engaged);
}

static esp_err_t handle_node_state(struct node_state_req *req, struct info_state *res) {
	struct node_spi_config config;
	esp_err_t error = load_node_hw(req->node_id, &config);
	if (error != ESP_OK) {
		return error;
	}

	error = read_state(&config, &res->state);
	if (error != ESP_OK) {
		return error;
	}

	error = load_node_info(req->node_id, &res->info);
	if (error != ESP_OK) {
		return error;
	}

	return ESP_OK;
}

static esp_err_t handle_scan(struct scan_req const *req, struct response_msg *main_resp, recv_packet_t *inbound) {
	union msg response;

	struct device_configuration config;
	esp_err_t error = load_device_config(&config);
	if (error != ESP_OK) {
		return error;
	}

	main_resp->scan.node_count = 0;
	for (size_t i = 0; i < (sizeof(config.nodes) / sizeof(*config.nodes)); i++) {
		struct info_state state;
		state.node_idx = i;
		if (!config.nodes[i].connected) {
			continue;
		}

		if (req->include_states) {
			if (load_node_info(i, &state.info) != ESP_OK) {
				continue;
			}
		}

		main_resp->scan.nodes[main_resp->scan.node_count++] = state.info;

		response.response = (struct response_msg) {
				.type = NODE_STATE,
				.info_and_state = state,
		};

		send_msg(false, inbound->sender_mac_addr, &response);
	}

	return ESP_OK;
}

void app_main(void) {
	init_espnow();

	static recv_packet_t *inbound;

	static union msg response_msg;
	struct response_msg *response = &response_msg.response;

	for (;;) {
		receive_msg(&inbound);
		if (!inbound) {
			ESP_LOGW(TAG, "Received NULL packet");
			continue;
		}

		ESP_LOGI(TAG, "Received packet from %02x:%02x:%02x:%02x:%02x:%02x",
			   inbound->sender_mac_addr[0], inbound->sender_mac_addr[1], inbound->sender_mac_addr[2],
			   inbound->sender_mac_addr[3], inbound->sender_mac_addr[4], inbound->sender_mac_addr[5]);

		struct request_msg *req = &inbound->data.request;

		esp_err_t result = ESP_OK;

		switch (req->type) {
			case FLASH_BEGIN:
				result = handle_flash_begin(&req->flash_begin, &response->flash_begin);
				break;
			case FLASH_DATA:
				result = handle_flash_data(&req->flash_data, &response->flash_data);
				break;
			case FLASH_DATA_END:
				result = handle_flash_data_end(&req->flash_data_end, &response->flash_data_end);
				break;
			case CONFIGURE_SHUTDOWN:
				result = handle_configure_shutdown(&req->configure_shutdown, &response->configure_shutdown);
				break;
			case CONFIGURE_ENGAGEMENT:
				result = handle_configure_engagement(&req->configure_engagement, &response->configure_engagement);
				break;
			case NODE_STATE:
				result = handle_node_state(&req->node_state, &response->info_and_state);
				break;

			case SCAN: result = handle_scan(&req->scan, response, inbound); break;
			case SET_NODE_INFO: result = write_device_config(&req->set_node_info); break;
			default: result = ESP_ERR_NOT_SUPPORTED;
		}

		response->type = req->type;
		response->ok = (result == ESP_OK) ? 1 : 0;
		if (result != ESP_OK) {
			ESP_LOGE(TAG, "Error handling request: %s", esp_err_to_name(result));
		}

		send_msg(response->type != SCAN, inbound->sender_mac_addr, &response_msg);
		free(inbound);
	}
}