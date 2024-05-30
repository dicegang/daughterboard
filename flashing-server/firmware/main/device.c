#include <sys/cdefs.h>
#include <stdlib.h>
#include <string.h>
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/queue.h"
#include "nvs_flash.h"
#include "esp_event.h"
#include "esp_netif.h"
#include "esp_wifi.h"
#include "esp_log.h"
#include "esp_system.h"
#include "esp_now.h"
#include <rom/ets_sys.h>

#include "sdkconfig.h"

#include "sdkconfig.h"

#include "network.h"
#include "../../espnow.h"
#include "crc8.h"
#include "crc32.h"
#include "avrisp.h"

#define TAG "device.c"

struct flashing_info {
	bool is_active;
	uint8_t target_node;
	uint16_t total_size;
	uint8_t total_chunks;
	uint8_t data[8192];
} flashing_info;


static esp_err_t send_blank_response(uint8_t destination_mac[ESP_NOW_ETH_ALEN], enum message_type type, bool ok) {
	static union msg msg;
	msg.response.ok = ok;
	msg.response.type = type;

	return send_msg(destination_mac, &msg);
}

static esp_err_t write_spi_config(struct node_configuration *config) {
	nvs_handle_t handle;

	esp_err_t error = nvs_open("storage", NVS_READWRITE, &handle);
	if (error != ESP_OK) {
		perror("nvs_open");
		return error;
	}

	error = nvs_set_blob(handle, "node_configuration", config, sizeof(*config));
	if (error != ESP_OK) {
		perror("nvs_set_blob");
		goto done;
	}

	error = nvs_commit(handle);
	if (error != ESP_OK) {
		perror("nvs_commit");
		goto done;
	}

	done:
	nvs_close(handle);
	return error;
}

static esp_err_t load_spi_config(struct node_configuration *config) {
	nvs_handle_t handle;

	esp_err_t error = nvs_open("storage", NVS_READWRITE, &handle);
	if (error != ESP_OK) {
		perror("nvs_open");
		return error;
	}

	size_t length;
	error = nvs_get_blob(handle, "node_configuration", config, &length);
	if (error != ESP_OK) {
		perror("nvs_get_blob");
	} else if (length != sizeof(*config)) {
		ESP_LOGE(TAG, "invalid size of spi bus config in nvs");
		error = ESP_ERR_INVALID_SIZE;
	}

	nvs_close(handle);
	return error;
}

void init_spi_config(void) {
	struct node_configuration config;
	esp_err_t err = load_spi_config(&config);
	switch (err) {
		case ESP_OK:
			return;
		case ESP_ERR_NOT_FOUND:
			memset(&config, '\x0', sizeof(config));
			ESP_ERROR_CHECK(write_spi_config(&config));
			ESP_ERROR_CHECK(load_spi_config(&config));
			break;
		default:
			ESP_ERROR_CHECK(load_spi_config(&config));
	}
}

void app_main(void) {
	init_espnow();
	init_spi_config();

	recv_packet_t inbound;
	struct request_msg *req = &inbound.data.request;

	static union msg response_msg;
	struct response_msg *response = &response_msg.response;

	for (;;) {
		receive_msg(&inbound);

		switch (req->type) {
			case FLASH_BEGIN:
				flashing_info = (struct flashing_info) {
						.is_active = true,
						.target_node = req->flash_begin.node_id,
						.total_size = req->flash_begin.total_size,
						.total_chunks = req->flash_begin.total_chunks
				};

				uint8_t expected_total_chunks = flashing_info.total_size / CHUNK_SIZE;
				if (flashing_info.total_size % CHUNK_SIZE)
					expected_total_chunks += 1;

				if (expected_total_chunks != flashing_info.total_chunks)
					goto error;

				memset(flashing_info.data, '\x0', sizeof(flashing_info.data));
				goto ok;
			case FLASH_DATA:
				if (!flashing_info.is_active || (req->flash_data.chunk_idx > flashing_info.total_chunks)) {
					goto error;
				}

				size_t chunk_size;
				if ((flashing_info.total_size % CHUNK_SIZE) &&
					(req->flash_data.chunk_idx == (flashing_info.total_chunks - 1)))
					chunk_size = flashing_info.total_size % CHUNK_SIZE;
				else
					chunk_size = CHUNK_SIZE;

				if (crc8(req->flash_data.data, chunk_size) != req->flash_data.crc) {
					goto error;
				}

				memcpy(flashing_info.data + (CHUNK_SIZE * req->flash_data.chunk_idx), req->flash_data.data, CHUNK_SIZE);
				goto ok;
			case FLASH_DATA_END:
				if (!flashing_info.is_active)
					goto error;

				flashing_info.is_active = false;
				if (crc32(flashing_info.data, flashing_info.total_size) != req->flash_data_end.crc)
					goto error;

				struct node_configuration c;
				if (load_spi_config(&c) != ESP_OK)
					goto error;

				int bus, slave;
			    for (bus = 0; bus < 2; bus++) {
					for (slave = 0; slave < 2; slave++) {
						if (c.busses[bus][slave].connected && (c.busses[bus][slave].info.node_id == flashing_info.target_node)) {
							goto loopend;
						}
					}
				}
				goto error;

				loopend:
				if (program(&c.busses[bus][slave].spi_config, flashing_info.data, flashing_info.total_size) != ESP_OK)
					goto error;

				goto ok;
			case CONFIGURE_SHUTDOWN:
				break;
			case CONFIGURE_ENGAGEMENT:
				break;
			case NODE_STATE:
				break;
			case SCAN:
				break;

			case SET_NODE_INFO:
				if (write_spi_config(&req->set_node_info) != ESP_OK)
					goto error;

				goto ok;

			ok:
				response->type = req->type;
				response->ok = true;
				break;

			error:
				response->type = req->type;
				response->ok = false;
		}

		send_msg(inbound.sender_mac_addr, &response_msg);
	}
}