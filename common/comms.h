#include <stdint.h>

typedef uint8_t mac_address_t[6];

struct comms_request {
    enum {
        COMMS_REQ_UNICAST,
        COMMS_REQ_BROADCAST,
        COMMS_REQ_HELLO
     } type;

     mac_address_t recipient;
     uint32_t message_size;
     uint8_t message[];
};

struct __attribute__((__packed__)) comms_response {
    uint32_t total_size;
    uint16_t message_count;

    struct __attribute__((__packed__)) comms_message {
        mac_address_t sender;
        uint32_t message_size;

        uint8_t message[];
    } messages[];
};