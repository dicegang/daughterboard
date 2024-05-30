//
// Created by alec on 5/29/24.
//

#ifndef ESP_NOW_RELAY_NETWORK_H
#define ESP_NOW_RELAY_NETWORK_H

// Define the structure of your data
#include <stdint.h>
#include <esp_err.h>
#include <esp_now.h>
#include "../protocol.h"

union msg {
	struct request_msg request;
	struct response_msg response;
};

typedef struct {
	uint8_t sender_mac_addr[ESP_NOW_ETH_ALEN];
	union msg data;
} recv_packet_t;

void init_espnow();
esp_err_t send_msg(uint8_t destination_mac[ESP_NOW_ETH_ALEN], union msg *message);
void receive_msg(recv_packet_t *receive_packet);

// Destination MAC address
// The default address is the broadcast address, which will work out of the box, but the slave will assume every tx succeeds.
// Setting to the master's address will allow the slave to determine if sending succeeded or failed.
//   note: with default config, the master's WiFi driver will log this for you. eg. I (721) wifi:mode : sta (12:34:56:78:9a:bc)
#define MY_RECEIVER_MAC {0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF}

#define MY_ESPNOW_PMK "pmk1234567890123"
#define MY_ESPNOW_CHANNEL 1

// #define MY_ESPNOW_ENABLE_LONG_RANGE 1

#define MY_SLAVE_DEEP_SLEEP_TIME_MS 10000


#endif //ESP_NOW_RELAY_NETWORK_H
