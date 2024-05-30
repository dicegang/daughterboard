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
#include "../protocol.h"

#define TAG "network"

#define MY_ESPNOW_WIFI_MODE WIFI_MODE_STA
#define MY_ESPNOW_WIFI_IF   ESP_IF_WIFI_STA

static QueueHandle_t s_recv_queue;
static EventGroupHandle_t s_evt_group;

#define MSG_SIZE (sizeof(recv_packet_t ) - sizeof(((recv_packet_t*)NULL)->sender_mac_addr))

static void send_cb(const uint8_t *mac_addr, esp_now_send_status_t status)
{
	if (mac_addr == NULL) {
		ESP_LOGE(TAG, "Send cb arg error");
		return;
	}

	assert(status == ESP_NOW_SEND_SUCCESS || status == ESP_NOW_SEND_FAIL);
	xEventGroupSetBits(s_evt_group, BIT(status));
}

static void recv_cb(struct esp_now_recv_info const *info, const uint8_t *data, int len) {
	static recv_packet_t recv_packet;

	ESP_LOGI(TAG, "%d bytes incoming from " MACSTR, len, MAC2STR(info->src_addr));

	if (len != MSG_SIZE) {
		ESP_LOGE(TAG, "Unexpected data length: %d != %u", len, MSG_SIZE);
		return;
	}

	memcpy(&recv_packet.sender_mac_addr, info->src_addr, sizeof(recv_packet.sender_mac_addr));
	memcpy(&recv_packet.data, data, len);
	if (xQueueSend(s_recv_queue, &recv_packet, 0) != pdTRUE) {
		ESP_LOGW(TAG, "Queue full, discarded");
		return;
	}
}

esp_err_t send_msg(uint8_t destination_mac[ESP_NOW_ETH_ALEN], union msg *message)
{
	// Send it
	ESP_LOGI(TAG, "Sending %u bytes to " MACSTR, sizeof(*message), MAC2STR(destination_mac));
	esp_err_t err = esp_now_send(destination_mac, (uint8_t*)message, sizeof(*message));
	if(err != ESP_OK)
	{
		ESP_LOGE(TAG, "Send error (%d)", err);
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

void receive_msg(recv_packet_t *receive_packet) {
	for(;;) {
		if (xQueueReceive(s_recv_queue, &receive_packet, portMAX_DELAY) != pdTRUE) {
			continue;
		}

		return;
	}
}

void init_espnow() {
	s_evt_group = xEventGroupCreate();

	const wifi_init_config_t cfg = WIFI_INIT_CONFIG_DEFAULT();
	esp_err_t ret = nvs_flash_init();
	if (ret == ESP_ERR_NVS_NO_FREE_PAGES || ret == ESP_ERR_NVS_NEW_VERSION_FOUND) {
		ESP_ERROR_CHECK(nvs_flash_erase());
		ret = nvs_flash_init();
	}
	ESP_ERROR_CHECK(ret);
	ESP_ERROR_CHECK(esp_netif_init());
	ESP_ERROR_CHECK(esp_event_loop_create_default());
	ESP_ERROR_CHECK(esp_wifi_init(&cfg));
	ESP_ERROR_CHECK(esp_wifi_set_storage(WIFI_STORAGE_RAM));
	ESP_ERROR_CHECK(esp_wifi_set_mode(MY_ESPNOW_WIFI_MODE));
	ESP_ERROR_CHECK(esp_wifi_start());
#if MY_ESPNOW_ENABLE_LONG_RANGE
	ESP_ERROR_CHECK( esp_wifi_set_protocol(MY_ESPNOW_WIFI_IF, WIFI_PROTOCOL_11B|WIFI_PROTOCOL_11G|WIFI_PROTOCOL_11N|WIFI_PROTOCOL_LR) );
#endif
	ESP_ERROR_CHECK(esp_now_init());

	ESP_ERROR_CHECK(esp_now_register_recv_cb(recv_cb));
	ESP_ERROR_CHECK(esp_now_register_send_cb(send_cb));

	ESP_ERROR_CHECK(esp_now_set_pmk((const uint8_t *) MY_ESPNOW_PMK));
}
