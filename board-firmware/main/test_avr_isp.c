//
// Created by alec on 5/30/24.
//

#include <stddef.h>
#include "avrisp.h"
#include "io.h"
#include <delay.h>
#include <string.h>
#include <esp_log.h>
#include <freertos/FreeRTOS.h>
#include <freertos/task.h>
#include <driver/spi_master.h>
#include <driver/gpio.h>
#include "../../attiny84-spi-test/attiny_firmware.h"

static uint8_t *parse_hex_string(char const *hex_string) {
	size_t len = strlen(hex_string);
	assert(!(len % 2));

	size_t size = len / 2;
	uint8_t *data = calloc(size, 1);
	assert(data);

	for (size_t i = 0; i < len; i += 2) {
		char hex[3] = {hex_string[i], hex_string[i + 1], '\0'};
		data[i / 2] = strtol(hex, NULL, 16);
	}

	return data;
}

static uint32_t btog(uint32_t num) {
	return (num >> 1) ^ num;
}

struct {
	gpio_num_t pin;
	uint32_t level;
} default_pins[] = {
		{SRC_CS_1, 1},
		{SRC_RST_1, 1},
		{LOAD_CS_1, 1},
		{LOAD_RST_1, 1},
		{SRC_EN_1, 1},
		{LOAD_EN_1, 1},
		{SUPPLY_EN_1, 1},
		{SRC_FAULT_1, 0},

		{SRC_CS_2, 1},
		{SRC_RST_2, 1},
		{LOAD_CS_2, 1},
		{LOAD_RST_2, 1},
		{SRC_EN_2, 1},
		{LOAD_EN_2, 1},
		{SUPPLY_EN_2, 1},
		{SRC_FAULT_2, 0},

		{ADC_CS, 1}
};

static struct chunk* parse_hex(uint8_t const* hex_contents, size_t hex_contents_size, size_t* num_chunks) {
	char *hex_contents_copy = malloc(hex_contents_size + 1);
	assert(hex_contents_copy);

	memcpy(hex_contents_copy, hex_contents, hex_contents_size);
	hex_contents_copy[hex_contents_size] = '\0';

	char *line = strtok(hex_contents_copy, ":");
	size_t num_lines = 0;
	while (line) {
		num_lines++;
		line = strtok(NULL, ":");
	}
	memcpy(hex_contents_copy, hex_contents, hex_contents_size);
	hex_contents_copy[hex_contents_size] = '\0';

	struct chunk *chunks = calloc(num_lines, sizeof(struct chunk));
	assert(chunks);

	line = strtok(hex_contents_copy, ":");
	size_t i = 0;
	while (line) {
		char size_str[3] = {line[0], line[1], '\0'};
		size_t size = strtol(size_str, NULL, 16);
		line += 2;

		char offset_str[5] = {line[0], line[1], line[2], line[3], '\0'};
		size_t offset = strtol(offset_str, NULL, 16);
		line += 4;

		char type_str[3] = {line[0], line[1], '\0'};
		size_t type = strtol(type_str, NULL, 16);
		line += 2;

		if (type == 0) {

			char *data = calloc(size * 2 + 1, 1);
			assert(data);
			memcpy(data, line, size * 2);
			data[size * 2] = '\0';

			uint8_t *data_parsed = parse_hex_string(data);
			free(data);
			struct chunk chunk = {.start_offset = offset, .size = size, .data = data_parsed};
			chunks[i++] = chunk;
		}

		line = strtok(NULL, ":");
	}

	*num_chunks = i;
	free(hex_contents_copy);
	return chunks;
}

void app_main(void) {
	for (size_t i = 0; i < sizeof(default_pins) / sizeof(default_pins[0]); i++) {
		gpio_set_direction(default_pins[i].pin, GPIO_MODE_OUTPUT);
		gpio_set_level(default_pins[i].pin, default_pins[i].level);
	}

	spi_bus_config_t bus_1_cfg = {
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

	spi_bus_config_t bus_2_cfg = {
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

	ESP_ERROR_CHECK(spi_bus_initialize(SIDE1_SPI, &bus_1_cfg, SPI_DMA_DISABLED));
	ESP_ERROR_CHECK(spi_bus_initialize(SIDE2_SPI, &bus_2_cfg, SPI_DMA_DISABLED));

	uint8_t spi_sides[] = {SPI_SIDE_1, SPI_SIDE_1, SPI_SIDE_2, SPI_SIDE_2};
	uint8_t ss_pins[] = {SRC_CS_1, LOAD_CS_1, SRC_CS_2, LOAD_CS_2};
	uint8_t rst_pins[] = {SRC_RST_1, LOAD_RST_1, SRC_RST_2, LOAD_RST_2};

	for (int i = 0; i < 4; i++) {
		struct node_spi_config config = (struct node_spi_config) {
				.spi_attiny = spi_sides[i], .ss_attiny = ss_pins[i], .rst_attiny = rst_pins[i],
		};

		size_t chunk_count;
		ESP_LOGV("main", "Firmware is %s", attiny_firmware);
		struct chunk *chunks = parse_hex(attiny_firmware, attiny_firmware_len, &chunk_count);

		ESP_LOGI("main", "Programming %d chunks", chunk_count);
		esp_err_t status;
		status = program(&config, chunk_count, chunks);
		ESP_LOGI("main", "Programming status: %s", esp_err_to_name(status));

		spi_device_interface_config_t dev_cfg = {
				.command_bits = 0,
				.address_bits = 0,
				.dummy_bits = 0,
				.mode = 0,
				.clock_source = SPI_CLK_SRC_DEFAULT,
				.clock_speed_hz = 1000000 / 2,
				.spics_io_num = config.ss_attiny,
				.queue_size = 1,
		};

		spi_device_handle_t spi;
		ESP_ERROR_CHECK(spi_bus_add_device(config.spi_attiny, &dev_cfg, &spi));
		ESP_ERROR_CHECK(spi_device_acquire_bus(spi, portMAX_DELAY));

		spi_transaction_t transaction = {
				.flags = SPI_TRANS_USE_TXDATA | SPI_TRANS_USE_RXDATA,
				.length = 16,
				.tx_data = {0xac},
				.rx_data = { 0 }
		};

		ESP_ERROR_CHECK(spi_device_transmit(spi, &transaction));
		spi_device_release_bus(spi);
		spi_bus_remove_device(spi);

		ESP_LOGI("main", "Attiny for side %d (%s) says [%02x %02x]", i / 2, (i % 2) ? "LOAD" : "SOURCE", transaction.rx_data[0], transaction.rx_data[1]);

		assert(transaction.rx_data[0] == 0xcc);
		assert(transaction.rx_data[1] == transaction.tx_data[0]);
		ESP_LOGI("main", "Attiny for side %d (%s) passed test", i / 2, (i % 2) ? "LOAD" : "SOURCE");
	}

	while (1) {
		vTaskDelay(portMAX_DELAY);
	}
}
