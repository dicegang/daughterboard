#include <network.h>
#include <delay.h>
#include <esp_log.h>

#include <comms.h>
#include <esp_mac.h>
#include <driver/gpio.h>
#include "host_communications.h"

#define TAG "relay-main"

esp_err_t net_send(struct comms_request *request) {
	esp_err_t err = esp_now_send(request->recipient, request->message, request->message_size);
	if (err != ESP_OK) {
		ESP_LOGE(TAG, "Failed to send message: %s, size %lu", esp_err_to_name(err), request->message_size);
	}

	return err;
}

void outbound_loop(void) {
	struct comms_request *request = calloc(sizeof(struct comms_request) + sizeof(struct request_msg), 1);
	if (request == NULL) {
		ESP_LOGE(TAG, "Failed to allocate memory for request");
		return;
	}

	for (;;) {
		ESP_ERROR_CHECK(host_receive_message(request));
		ESP_LOGI(TAG, "Received request %d", request->type);

		if (request->type == COMMS_REQ_HELLO) {
			struct comms_message m = {.data_size = 0, .sender = {0}};
			ESP_ERROR_CHECK(host_send_message(&m));
		} else {
			ESP_ERROR_CHECK(net_send(request));
		}
	}
}

void inbound_loop(void) {
	recv_packet_t *recv_packet = NULL;
	struct comms_message *response = calloc(sizeof(struct comms_message) + sizeof(recv_packet->data), 1);

	for (;;) {
		receive_msg(&recv_packet);

		assert(recv_packet != NULL);
		ESP_LOGD(TAG, "Received packet from " MACSTR, MAC2STR(recv_packet->sender_mac_addr));

		memcpy(response->sender, recv_packet->sender_mac_addr, ESP_NOW_ETH_ALEN);
		response->data_size = sizeof(recv_packet->data);
		memcpy(response->data, &recv_packet->data, sizeof(recv_packet->data));

		ESP_ERROR_CHECK(host_send_message(response));

		free(recv_packet);
	}
}

void app_main() {
	init_espnow();
	host_communications_init();

	xTaskCreate((TaskFunction_t) inbound_loop, "inbound_loop", 4096, NULL, 5, NULL);
	xTaskCreate((TaskFunction_t) outbound_loop, "outbound_loop", 4096, NULL, 5, NULL);

	for (;;) {
		vTaskDelay(1);
	}
}