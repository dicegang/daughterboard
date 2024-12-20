#ifndef PROTOCOL_H
#define PROTOCOL_H

#include <stdint.h>
#include <stdbool.h>

#define MAX_CHUNK_SIZE 16
#define MAX_NODES 4

struct node_state {
    bool shutdown;
    bool engaged;

    enum {
    	TRIP_REASON_NONE,
    	TRIP_REASON_OVERCURRENT,
    	TRIP_REASON_OVERVOLTAGE,
    	TRIP_REASON_ANGLE,
    	TRIP_REASON_THD,
    	TRIP_REASON_MANUAL
    } trip_reason;

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

struct node_spi_config {
	enum { NODE_HSPI = 1, NODE_VSPI = 2 } spi_attiny, spi_adc, spi_sr;
	uint8_t ss_attiny, ss_adc, ss_sr;

	uint8_t shutdown_pin_sr, engaged_pin_sr;

	enum {
		ADC_CHAN_01, ADC_CHAN_23, ADC_CHAN_45, ADC_CHAN_67
	} adc_chan;
};

struct device_configuration {
	struct {
		bool connected;
		struct node_spi_config spi_config;
		struct node_info info;
	} nodes[MAX_NODES];
};

struct device_info {
    uint8_t node_count;
    struct node_info nodes[MAX_NODES];
};

typedef enum message_type {
	FLASH_BEGIN,
	FLASH_DATA,
	FLASH_DATA_END,
	CONFIGURE_SHUTDOWN,
	CONFIGURE_ENGAGEMENT,
	NODE_STATE,
	SCAN,
	SET_NODE_INFO
} message_type;

struct request_msg {
    enum message_type type;

    union {
        struct flash_begin_req {
            uint8_t node_id;
            uint16_t total_chunks;
        } flash_begin;

        struct flash_data_req {
        	uint8_t chunk_idx;
        	uint8_t crc;
			uint16_t chunk_offset;
        	uint8_t chunk_size;
			uint8_t chunk[MAX_CHUNK_SIZE];
        } flash_data;

        struct {} flash_data_end;

        struct cfg_shtdn_req {
            uint8_t node_id;
            bool shutdown;
        } configure_shutdown;

        struct cfg_engage_req {
            uint8_t node_id;
            bool engaged;
        } configure_engagement;

        struct scan_req {
        	bool include_states;
        } scan;

        struct node_state_req {
            uint8_t node_id;
        } node_state;

		struct device_configuration set_node_info;
    };
};

struct response_msg {
    enum message_type type;
    bool ok;

    union {
        struct {} flash_begin;
        struct {} flash_data;
        struct {} flash_data_end;
        struct {} configure_shutdown;
        struct {} configure_engagement;
        struct device_info scan;
        struct info_state {
			uint8_t node_idx;
        	struct node_info info;
        	struct node_state state;
        } info_and_state;
		struct {} set_node_info;
    };
};

char* response_msg_to_string(struct response_msg *msg);

#endif
