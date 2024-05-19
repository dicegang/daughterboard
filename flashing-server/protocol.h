#include <stdint.h>
#include <stdbool.h>

struct node_state {
    bool shutdown;
    bool engaged;

    double current_rms_inner;
    double current_rms_outer;
    double voltage_rms;

    double current_freq_inner;
    double current_freq_outer;
    double voltage_freq;

    double phase_angle;
    double currents_angle;

    double current_thd_inner;
    double current_thd_outer;
    double voltage_thd;
};

struct node_info {
    enum { 
        NODE_TYPE_SOURCE,
        NODE_TYPE_LOAD
    } type;
    uint8_t owner_id;
};

struct device_info {
    uint8_t node_count;
    struct node_info nodes[];
};

struct request_msg {
    enum {
        REQ_FLASH_ATTINY,
        REQ_CONFIGURE_SHUTDOWN,
        REQ_CONFIGURE_ENGAGEMENT,
        REQ_NODE_STATE,
        REQ_SCAN
    } type;

    union {
        struct {
            uint8_t node_id;
            uint16_t size;
            uint8_t rom[];
        } flash_attiny;

        struct {
            uint8_t node_id;
            bool shutdown;
        } configure_shutdown;

        struct {
            uint8_t node_id;
            bool engaged;
        } configure_engagement;

        struct {} scan;

        struct {
            uint8_t node_id;
        } node_state;
    };
};

struct response_msg {
    enum {
        RES_FLASH_ATTINY,
        RES_CONFIGURE_SHUTDOWN,
        RES_CONFIGURE_ENGAGEMENT,
        RES_SCAN,
        RES_NODE_STATE
    } type;
    bool ok;

    union {
        struct {} flash_attiny;
        struct {} configure_shutdown;
        struct {} configure_engagement;
        struct device_info scan;
        struct node_state node_state;
    };
};