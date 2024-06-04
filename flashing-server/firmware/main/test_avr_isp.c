//
// Created by alec on 5/30/24.
//

#include <stddef.h>
#include "avrisp.h"
#include <string.h>
#include <esp_log.h>
#include <freertos/FreeRTOS.h>
#include <freertos/task.h>
#include <driver/spi_master.h>
#include <driver/gpio.h>

uint8_t *parse_hex_string(char const *hex_string) {
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

void app_main(void) {
//	esp_log_level_set("avrisp", ESP_LOG_VERBOSE);

//	struct node_spi_config config = (struct node_spi_config) {
//			.mosi = 0, .miso = 2, .clk = 4, .rst = 23, .clock_rate = 1000000 / 6
//	};

	struct node_spi_config config = (struct node_spi_config) {
		.spi_attiny = NODE_VSPI, .ss_attiny = 23
	};


	spi_bus_config_t bus_cfg = {
			.mosi_io_num = 0,
			.miso_io_num = 2,
			.sclk_io_num = 4,

			.quadwp_io_num = -1,
			.quadhd_io_num = -1,
			.data4_io_num = -1,
			.data5_io_num = -1,
			.data6_io_num = -1,
			.data7_io_num = -1,

			.flags = SPICOMMON_BUSFLAG_GPIO_PINS | SPICOMMON_BUSFLAG_MASTER
	};

	gpio_set_direction(config.ss_attiny, GPIO_MODE_OUTPUT);
	gpio_set_direction(bus_cfg.mosi_io_num, GPIO_MODE_OUTPUT);
	gpio_set_direction(bus_cfg.miso_io_num, GPIO_MODE_INPUT);
	gpio_set_direction(bus_cfg.sclk_io_num, GPIO_MODE_OUTPUT);


	ESP_ERROR_CHECK(spi_bus_initialize(NODE_VSPI, &bus_cfg, SPI_DMA_DISABLED));
	srand(0);
	static struct chunk chunks[512];
	for (int i = 0; i < 512; i++) {
		size_t size = 16;
		uint8_t *data = calloc(size, 1);
		assert (data);
		for (size_t j = 0; j < size; j++) {
			data[j] = rand();
		}

		struct chunk chunk = {.start_offset = i * 16, .size = 16, .data = data};
		chunks[i] = chunk;
	}

//	struct chunk chunks[] = {{0x10, 0x0000, parse_hex_string("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA") },
//							 {0x10, 0x0010, parse_hex_string("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA") },
//							 {0x10, 0x0020, parse_hex_string("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA") },
//							 {0x10, 0x0030, parse_hex_string("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA") },
//							 {0x10, 0x0040, parse_hex_string("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA") },
//							 {0x10, 0x0050, parse_hex_string("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA") },
//							 {0x02, 0x0060, parse_hex_string("AAAA") },
//	};

//		struct chunk test_chunk = {0, 96 + 2, test_page};
//	struct chunk test_chunk2 = {0, 128, test_page};
	esp_err_t status;
//	esp_err_t  status = program(&config, 1, &test_chunk);
//	ESP_LOGI("main", "Programming status: %s", esp_err_to_name(status));
	status = program(&config, 512, chunks);
	ESP_LOGI("main", "Programming status: %s", esp_err_to_name(status));

	while (1) {
		vTaskDelay(portMAX_DELAY);
	}
}
