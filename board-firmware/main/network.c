#include <network.h>

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

#define MY_ESPNOW_WIFI_MODE WIFI_MODE_STA
#define MY_ESPNOW_WIFI_IF   ESP_IF_WIFI_STA

QueueHandle_t s_recv_queue = NULL;
EventGroupHandle_t s_evt_group;
esp_interface_t my_interface = MY_ESPNOW_WIFI_IF;

StaticQueue_t s_recv_queue_buffer;
uint8_t queue_storage[4 * sizeof(recv_packet_t *)];

void init_espnow() {
	s_recv_queue = xQueueCreateStatic(4, sizeof(recv_packet_t*), &queue_storage[0], &s_recv_queue_buffer);
	s_evt_group = xEventGroupCreate();

	assert(s_recv_queue);

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
