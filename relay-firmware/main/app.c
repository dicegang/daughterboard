#include <network.h>
#include <delay.h>
#include <esp_log.h>
#include <driver/spi_slave.h>

#include <comms.h>
#include <esp_mac.h>
#include <driver/gpio.h>

#define HANDSHAKE_PIN 13

#define RESPONSE_TIME_TICKS pdMS_TO_TICKS(500)
#define TAG "relay-main"

QueueHandle_t comms_recv_queue;

struct pkt_recv_node {
	struct pkt_recv_node *next;
	recv_packet_t *packet;
};

struct pkt_recv_list {
	struct pkt_recv_node *head;
};


void net_receive_collate(struct pkt_recv_list *recv_list, size_t *recv_count, size_t *recv_size) {
	TickType_t start_time = xTaskGetTickCount();
	TickType_t remaining_time = RESPONSE_TIME_TICKS;

	while ((remaining_time = (xTaskGetTickCount() - start_time)) < RESPONSE_TIME_TICKS) {
		recv_packet_t *recv_packet = NULL;
		if (receive_msg_until(&recv_packet, remaining_time)) {
			assert(recv_packet != NULL);
			ESP_LOGD(TAG, "Received packet from " MACSTR, MAC2STR(recv_packet->sender_mac_addr));

			struct pkt_recv_node *node = malloc(sizeof(struct pkt_recv_node));
			if (node == NULL) {
				ESP_LOGE(TAG, "Failed to allocate memory for pkt_recv_node");
				break;
			}

			node->packet = recv_packet;
			node->next = (*recv_list).head;
			(*recv_list).head = node;

			(*recv_count)++;
			(*recv_size) += sizeof(node->packet->data);
		}
	}
}

esp_err_t net_send(struct comms_request *request) {
	esp_err_t err = esp_now_send(request->recipient, request->message, request->message_size);
	if (err != ESP_OK) {
		ESP_LOGE(TAG, "Failed to send message: %s, size %lu", esp_err_to_name(err), request->message_size);
	}

	return err;
}

struct comms_response* construct_comms_response(struct pkt_recv_list *recv_list, size_t recv_count, size_t recv_size) {
	size_t response_size = sizeof(struct comms_response) + sizeof(struct comms_message) * recv_count + recv_size;
	struct comms_response *response = malloc(response_size);
	if (response == NULL) {
		ESP_LOGE(TAG, "Failed to allocate memory for response");
		return NULL;
	}

	response->total_size = response_size;
	response->message_count = recv_count;

	struct comms_message *message = response->messages;
	while ((*recv_list).head != NULL) {
		struct pkt_recv_node *node = (*recv_list).head;

		message->message_size = sizeof(node->packet->data);
		memcpy(message->sender, node->packet->sender_mac_addr, ESP_NOW_ETH_ALEN);
		memcpy(message->message, &node->packet->data, message->message_size);

		(*recv_list).head = node->next;
		free(node->packet);
		free(node);

		// probably UB
		message = (struct comms_message*)((uint8_t*)message + sizeof(struct comms_message) + message->message_size);
	}

	return response;
}

static QueueHandle_t spi_transmit_queue;

void net_recv_loop(void) {
	struct comms_request *request = calloc(sizeof(struct comms_request) + sizeof(struct request_msg), 1);
	if (request == NULL) {
		ESP_LOGE(TAG, "Failed to allocate memory for request");
		return;
	}

	spi_slave_transaction_t transaction = {
			.length = sizeof(struct comms_request) + sizeof(struct request_msg),
			.tx_buffer = NULL,
			.trans_len = sizeof(struct comms_request) + sizeof(struct request_msg),
			.rx_buffer = request
	};

	ESP_ERROR_CHECK(spi_slave_transmit(HSPI_HOST, &transaction, portMAX_DELAY));
	ESP_LOGI(TAG, "Received request %d, size %d", request->type, transaction.length);
	gpio_set_level(HANDSHAKE_PIN, 1);

	// print bytes
	for (int i = 0; i < transaction.trans_len; i++) {
		ESP_LOGI(TAG, "Byte %d: %x", i, ((uint8_t*)request)[i]);
	}

	if (request->type == COMMS_REQ_HELLO) {
		struct comms_response resp;
		resp.message_count = 0;
		resp.total_size = 0;

		transaction = (spi_slave_transaction_t) {
				.length = sizeof(resp),
				.tx_buffer = &resp,
				.trans_len = sizeof(resp),
				.rx_buffer = NULL
		};

		ESP_ERROR_CHECK(spi_slave_transmit(HSPI_HOST, &transaction, pdMS_TO_TICKS(10)));
		goto done;
	}

	esp_err_t result = net_send(request);
	free(request);
	if (result != ESP_OK) {
		return;
	}

	struct pkt_recv_list recv_list = {0};
	size_t recv_count = 0;
	size_t recv_size = 0;

	net_receive_collate(&recv_list, &recv_count, &recv_size);
	struct comms_response *response = construct_comms_response(&recv_list, recv_count, recv_size);
	if (response == NULL) {
		ESP_LOGE(TAG, "Failed to construct response");
		return;
	}

	transaction = (spi_slave_transaction_t) {
			.length = response->total_size * 8,
			.tx_buffer = response,
			.trans_len = response->total_size * 8,
			.rx_buffer = NULL
	};

	ESP_ERROR_CHECK(spi_slave_transmit(HSPI_HOST, &transaction, 0));
	free(response);

	done:
	gpio_set_level(HANDSHAKE_PIN, 0);
}

void app_main() {
	init_espnow();

	gpio_set_direction(HANDSHAKE_PIN, GPIO_MODE_OUTPUT);
	gpio_set_level(HANDSHAKE_PIN, 0);

	spi_bus_config_t bus_cfg = {
			.mosi_io_num = 14,
			.miso_io_num = 27,
			.sclk_io_num = 26,

			.quadwp_io_num = -1,
			.quadhd_io_num = -1,
			.data4_io_num = -1,
			.data5_io_num = -1,
			.data6_io_num = -1,
			.data7_io_num = -1,

			.flags = SPICOMMON_BUSFLAG_GPIO_PINS | SPICOMMON_BUSFLAG_SLAVE
	};




	spi_slave_interface_config_t slave_cfg = {
			.queue_size = 1,
			.spics_io_num = 25,
			.mode = 0,
	};

	ESP_ERROR_CHECK(spi_slave_initialize(HSPI_HOST, &bus_cfg, &slave_cfg, SPI_DMA_DISABLED));

	for (;;) {
		net_recv_loop();
	}

//	union msg msg;
//
//	msg.request.type = SCAN;
//
//	for (;;) {
//		uint8_t bcast[6] = {0xff, 0xff, 0xff, 0xff, 0xff, 0xff};
//		send_msg(false, bcast, &msg);
//
//		recv_packet_t *recv_packet;
//		receive_msg(&recv_packet);
//
//		char* str = response_msg_to_string(&recv_packet->data.response);
//		ESP_LOGI("app", "response: %s", str);
//		free(str);
//
//		if (recv_packet->data.response.type == SCAN) {
//			struct device_info *scan = &recv_packet->data.response.scan;
//			ESP_LOGI(TAG, "Scan response: %d nodes", scan->node_count);
//			for (int i = 0; i < scan->node_count; i++) {
//				struct node_info *node = &scan->nodes[i];
//				char* type;
//
//				switch (node->type) {
//					case NODE_TYPE_LOAD:
//						type = "load";
//						break;
//					case NODE_TYPE_SOURCE:
//						type = "source";
//						break;
//					default:
//						type = "unknown";
//						break;
//				}
//				ESP_LOGI(TAG, "Node %d: owner %d, type %s", i, node->owner_id, type);
//			}
//		}
//
//		DELAY_MS(1000);
//	}
}