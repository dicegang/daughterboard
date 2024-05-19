#include <stdint.h>

typedef uint8_t mac_address_t[6];

struct comms_request {
    enum {
        COMMS_REQ_UNICAST,
        COMMS_REQ_BROADCAST
     } type;

     mac_address_t recipient;
     uint32_t message_size;
     uint8_t message[];
};

struct comms_response {
    uint32_t total_size;
    uint16_t message_count;

    struct {
        mac_address_t sender;
        uint32_t message_size;

        uint8_t message[];
    } messages[];
};