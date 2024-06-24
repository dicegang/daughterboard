//
// Created by alec on 5/29/24.
//

#include "network.h"

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
#include <protocol.h>

#define TAG "network"

#define MSG_SIZE (sizeof(union msg ))

void send_cb(const uint8_t *mac_addr, esp_now_send_status_t status)
{
	if (mac_addr == NULL) {
		ESP_LOGE(TAG, "Send cb arg error");
		return;
	}

	assert(status == ESP_NOW_SEND_SUCCESS || status == ESP_NOW_SEND_FAIL);
	xEventGroupSetBits(s_evt_group, BIT(status));
}

void recv_cb(struct esp_now_recv_info const *info, const uint8_t *data, int len) {

	ESP_LOGI(TAG, "%d bytes incoming from " MACSTR, len, MAC2STR(info->src_addr));

	if (len != MSG_SIZE) {
		ESP_LOGE(TAG, "Unexpected data length: %d != %u", len, MSG_SIZE);
		return;
	}

	if (!esp_now_is_peer_exist(info->src_addr)) {
		esp_now_peer_info_t peer_info = {
			.ifidx = my_interface,
			.lmk = MY_ESPNOW_PMK,
			.channel = info->rx_ctrl->channel,
			.encrypt = true,
		};

		memcpy(peer_info.peer_addr, info->src_addr, 6);
		esp_now_add_peer(&peer_info);
	}

	recv_packet_t *recv_packet = calloc(sizeof(recv_packet_t), 1);
	if (recv_packet == NULL) {
		ESP_LOGE(TAG, "Failed to allocate memory for recv_packet");
		return;
	}

	memcpy(recv_packet->sender_mac_addr, info->src_addr, ESP_NOW_ETH_ALEN);
	memcpy(&recv_packet->data, data, len);
	if (xQueueSendToBack(s_recv_queue, &recv_packet, 0) != pdPASS) {
		ESP_LOGW(TAG, "Queue full, discarded");
		return;
	}

	ESP_LOGI(TAG, "[recv_cb]: queue addr: %p", s_recv_queue);
}

esp_err_t send_msg(bool encrypt, uint8_t destination_mac[ESP_NOW_ETH_ALEN], union msg *message)
{
	esp_err_t err;

	// Send it
	ESP_LOGI(TAG, "Sending %u bytes to " MACSTR, sizeof(*message), MAC2STR(destination_mac));

	esp_now_peer_info_t  info;

	if (esp_now_get_peer(destination_mac, &info) != ESP_OK) {
		info = (esp_now_peer_info_t) {
			.channel = MY_ESPNOW_CHANNEL,
			.ifidx = my_interface,
		};
		memcpy(info.peer_addr, destination_mac, 6);

		err = esp_now_add_peer(&info);
		if (err != ESP_OK) {
			ESP_LOGE(TAG, "Add peer error (%s)", esp_err_to_name(err));
			return ESP_FAIL;
		}
	}

	info.encrypt = !is_broadcast(destination_mac) && encrypt;
	memcpy(info.lmk, MY_ESPNOW_PMK, ESP_NOW_KEY_LEN);

	esp_now_mod_peer(&info);

	err = esp_now_send(destination_mac, (uint8_t*)message, sizeof(*message));
	if(err != ESP_OK)
	{
		ESP_LOGE(TAG, "Send error (%s)", esp_err_to_name(err));
		return ESP_FAIL;
	}

	// Wait for callback function to set status bit
	EventBits_t bits = xEventGroupWaitBits(s_evt_group, BIT(ESP_NOW_SEND_SUCCESS) | BIT(ESP_NOW_SEND_FAIL), pdTRUE, pdFALSE, 2000 / portTICK_PERIOD_MS);
	if ( !(bits & BIT(ESP_NOW_SEND_SUCCESS)) )
	{
		if (bits & BIT(ESP_NOW_SEND_FAIL))
		{
			ESP_LOGE(TAG, "Send error");
			return ESP_FAIL;
		}
		ESP_LOGE(TAG, "Send timed out");
		return ESP_ERR_TIMEOUT;
	}

	ESP_LOGI(TAG, "Sent!");
	return ESP_OK;
}

void receive_msg(recv_packet_t **receive_packet) {
	for(;;) {
		ESP_LOGI(TAG, "[receive_msg]: queue addr: %p", s_recv_queue);
		if (xQueueReceive(s_recv_queue, receive_packet, portMAX_DELAY) != pdTRUE) {
			continue;
		}

		return;
	}
}

bool receive_msg_until(recv_packet_t **receive_packet, TickType_t duration) {
	TickType_t start = xTaskGetTickCount();
	TickType_t remaining = duration;

	while ((remaining = (xTaskGetTickCount() - start)) < duration) {
		if (xQueueReceive(s_recv_queue, receive_packet, remaining) == pdTRUE) {
			return true;
		}
	}

	return false;
}