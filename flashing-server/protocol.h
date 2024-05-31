#ifndef PROTOCOL_H
#define PROTOCOL_H

#include <stdint.h>
#include <stdbool.h>

#define MAX_CHUNK_SIZE 16

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
    uint8_t node_id;
    uint8_t owner_id;
};

struct bitbang_spi_config {
	uint8_t rst, clk, mosi, miso;
	uint32_t clock_rate;
};

struct node_configuration {
	struct {
		bool connected;
		struct bitbang_spi_config spi_config;
		struct node_info info;
	} busses[2][2];
};

struct device_info {
    uint8_t node_count;
    struct node_info nodes[];
};

enum message_type {
	FLASH_BEGIN,
	FLASH_DATA,
	FLASH_DATA_END,
	CONFIGURE_SHUTDOWN,
	CONFIGURE_ENGAGEMENT,
	NODE_STATE,
	SCAN,
	SET_NODE_INFO
};

struct request_msg {
    enum message_type type;

    union {
        struct {
            uint8_t node_id;
            uint8_t total_chunks;
        } flash_begin;

        struct {
        	uint8_t chunk_idx;
        	uint8_t crc;
			uint16_t chunk_offset;
        	uint8_t chunk_size;
			uint8_t chunk[MAX_CHUNK_SIZE];
        } flash_data;

        struct {} flash_data_end;

        struct {
            uint8_t node_id;
            bool shutdown;
        } configure_shutdown;

        struct {
            uint8_t node_id;
            bool engaged;
        } configure_engagement;

        struct {
        	bool include_states;
        } scan;

        struct {
            uint8_t node_id;
        } node_state;

		struct node_configuration set_node_info;
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
        struct {
        	struct node_info info;
        	struct node_state state;
        } info_and_state;
		struct {} set_node_info;
    };
};

#endif