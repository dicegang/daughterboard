//
// Created by alec on 5/29/24.
//

#ifndef ESP_NOW_RELAY_NETWORK_H
#define ESP_NOW_RELAY_NETWORK_H

// Define the structure of your data
#include <stdint.h>
#include <esp_err.h>
#include <esp_now.h>
#include <protocol.h>
#include <freertos/FreeRTOS.h>
#include <freertos/event_groups.h>
#include <string.h>

extern QueueHandle_t s_recv_queue;
extern EventGroupHandle_t s_evt_group;
extern esp_interface_t my_interface;

union msg {
	struct request_msg request;
	struct response_msg response;
};

typedef struct {
	uint8_t sender_mac_addr[ESP_NOW_ETH_ALEN];
	union msg data;
} recv_packet_t;

void init_espnow(void);
esp_err_t send_msg(bool encrypt, uint8_t destination_mac[ESP_NOW_ETH_ALEN], union msg *message);
void receive_msg(recv_packet_t **receive_packet);
bool receive_msg_until(recv_packet_t **receive_packet, TickType_t duration);
void send_cb(const uint8_t *mac_addr, esp_now_send_status_t status);
void recv_cb(struct esp_now_recv_info const *info, const uint8_t *data, int len);

// Destination MAC address
// The default address is the broadcast address, which will work out of the box, but the slave will assume every tx succeeds.
// Setting to the master's address will allow the slave to determine if sending succeeded or failed.
//   note: with default config, the master's WiFi driver will log this for you. eg. I (721) wifi:mode : sta (12:34:56:78:9a:bc)
#define BROADCAST_ADDRESS {0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF}

inline bool is_broadcast(uint8_t const mac[ESP_NOW_ETH_ALEN]) {
	static uint8_t const broadcast_address[ESP_NOW_ETH_ALEN] = BROADCAST_ADDRESS;
	return memcmp(mac, broadcast_address, ESP_NOW_ETH_ALEN) == 0;
}

#define MY_ESPNOW_PMK "pmk1234567890123"
#define MY_ESPNOW_CHANNEL 1

// #define MY_ESPNOW_ENABLE_LONG_RANGE 1

#define MY_SLAVE_DEEP_SLEEP_TIME_MS 10000


#endif //ESP_NOW_RELAY_NETWORK_H
