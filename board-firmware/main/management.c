////
//// Created by alec on 6/2/24.
////

#include <freertos/FreeRTOS.h>
#include <freertos/semphr.h>

#include <driver/gpio.h>
#include <delay.h>
#include "management.h"
#include "io.h"

#define OPCODE(is_read) (0b01000000 | (SHIFT_REGISTER_ADDRESS << 1) | (is_read ? 1 : 0))
#define REG_IODIR 0x00
#define REG_GPIO 0x09

static TaskHandle_t thd_calculation_task, watchdog_polling_task, team_communicator_task;

struct {
	gpio_num_t pin;
	uint32_t level;
} default_pins[] = {
		{SRC_CS_1, 1},
		{SRC_RST_1, 1},
		{LOAD_CS_1, 1},
		{LOAD_RST_1, 1},
		{SRC_EN_1, 0},
		{LOAD_EN_1, 0},
		{SUPPLY_EN_1, 1},
		{SRC_FAULT_1, 0},

		{SRC_CS_2, 1},
		{SRC_RST_2, 1},
		{LOAD_CS_2, 1},
		{LOAD_RST_2, 1},
		{SRC_EN_2, 0},
		{LOAD_EN_2, 0},
		{SUPPLY_EN_2, 1},
		{SRC_FAULT_2, 0},

		{ADC_CS, 1}
};

void device_init() {
	for (size_t i = 0; i < sizeof(default_pins) / sizeof(default_pins[0]); i++) {
		gpio_set_direction(default_pins[i].pin, GPIO_MODE_OUTPUT);
		gpio_set_level(default_pins[i].pin, default_pins[i].level);
	}

	spi_bus_config_t bus_cfg_1 = {
			.mosi_io_num = MOSI_1,
			.miso_io_num = MISO_1,
			.sclk_io_num = SCLK_1,

			.quadwp_io_num = -1,
			.quadhd_io_num = -1,
			.data4_io_num = -1,
			.data5_io_num = -1,
			.data6_io_num = -1,
			.data7_io_num = -1,

			.flags = SPICOMMON_BUSFLAG_GPIO_PINS | SPICOMMON_BUSFLAG_MASTER
	};

	spi_bus_config_t bus_cfg_2 = {
			.mosi_io_num = MOSI_2,
			.miso_io_num = MISO_2,
			.sclk_io_num = SCLK_2,

			.quadwp_io_num = -1,
			.quadhd_io_num = -1,
			.data4_io_num = -1,
			.data5_io_num = -1,
			.data6_io_num = -1,
			.data7_io_num = -1,

			.flags = SPICOMMON_BUSFLAG_GPIO_PINS | SPICOMMON_BUSFLAG_MASTER
	};

	ESP_ERROR_CHECK(spi_bus_initialize(SPI2_HOST, &bus_cfg_1, SPI_DMA_DISABLED));
	ESP_ERROR_CHECK(spi_bus_initialize(SPI3_HOST, &bus_cfg_2, SPI_DMA_DISABLED));
}

static struct node_state states[2][2] = { 0 };

static gpio_num_t const engage_pins[2][2] = {
		{SRC_EN_1, LOAD_EN_1},
		{SRC_EN_2, LOAD_EN_2}
};

static gpio_num_t const fault_pins[2][2] = {
		{SRC_FAULT_1, LOAD_FAULT_1},
		{SRC_FAULT_2, LOAD_FAULT_2}
};


static gpio_num_t const supply_pins[2][2] = {
		{SUPPLY_EN_1, GPIO_NUM_NC},
		{SUPPLY_EN_2, GPIO_NUM_NC}
};

static enum trip_reason trip_reasons[2][2] = {
		{TRIP_REASON_NONE, TRIP_REASON_NONE},
		{TRIP_REASON_NONE, TRIP_REASON_NONE}
};

enum trip_reason get_trip_reason(bool side, bool load) {
	return trip_reasons[side][load];
}

static bool watchdog_untrip_safe(bool side, bool load) {
	return true; // can't really do anything here to check if it's safe...
}

static bool watchdog_engagement_safe(bool side, bool load) {
	return true;
	// check phase angle and voltage
}

esp_err_t trip(enum trip_reason reason, bool side, bool load) {
	if (gpio_get_level(fault_pins[side][load]) != 1) {
		trip_reasons[side][load] = reason;
	}

	gpio_set_level(fault_pins[side][load], 1);
	gpio_set_level(supply_pins[side][load], 0);

	gpio_set_level(engage_pins[side][load], 0);

	return ESP_OK;
}

esp_err_t untrip(bool side, bool load) {
	if (!watchdog_untrip_safe(side, load)) {
		return ESP_FAIL;
	}

	gpio_set_level(supply_pins[side][load], 1);
	gpio_set_level(fault_pins[side][load], 0);
	trip_reasons[side][load] = TRIP_REASON_NONE;
	return ESP_OK;
}

esp_err_t set_engaged(bool side, bool load, bool engaged) {
	if (!engaged) {
		gpio_set_level(engage_pins[side][load], 0);
		return ESP_OK;
	} else {
		if (get_trip_reason(side, load) != TRIP_REASON_NONE) {
			return ESP_FAIL;
		}

		if (!watchdog_engagement_safe(side, load)) {
			return ESP_FAIL;
		}
		gpio_set_level(engage_pins[side][load], 1);
		return ESP_OK;
	}
}

esp_err_t read_state(bool side, bool load, struct node_state *state) {
	*state = states[side][load];
	return ESP_OK;
}