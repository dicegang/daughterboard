#ifndef ESP_NOW_RELAY_COMMS_H
#define ESP_NOW_RELAY_COMMS_H

#include <stdint.h>

typedef uint8_t mac_address_t[6];

struct __attribute__((__packed__)) comms_request {
    enum comms_request_type {
        COMMS_REQ_UNICAST,
        COMMS_REQ_BROADCAST,
        COMMS_REQ_HELLO
     } type;

     mac_address_t recipient;
     uint32_t message_size;
     uint8_t message[];
};

struct __attribute__((__packed__)) comms_message {
	mac_address_t sender;
	uint32_t data_size;

	uint8_t data[];
};

#endif //ESP_NOW_RELAY_COMMS_H