//
// Created by alec on 5/30/24.
//

#include <stddef.h>
#include "avrisp.h"
#include <string.h>
#include <esp_log.h>
#include <freertos/FreeRTOS.h>
#include <freertos/task.h>

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

static uint32_t btog(uint32_t num)
{
	return (num>>1) ^ num;
}

void app_main_actual(void*) {
	esp_log_level_set("avrisp", ESP_LOG_DEBUG);

	struct bitbang_spi_config config = (struct bitbang_spi_config) {
		.mosi = 0, .miso = 2, .clk = 4, .rst = 23, .clock_rate = 1000000 / 30
	};

//	test_page[0] = 0b11111110;

	struct chunk chunks[] = {
			{
					.size = 16,
					.start_offset = 0,
					.data = parse_hex_string("0EC015C014C013C012C011C010C00FC064"),
			},
			{
					.size = 0x10,
					.start_offset = 0x0010,
					.data = parse_hex_string("0EC00DC00CC00BC00AC009C008C01124"),
			},
			{
					.size = 0x10,
					.start_offset = 0x0020,
					.data = parse_hex_string("1FBECFE5D2E0DEBFCDBF02D011C0E8CF"),
			},
			{
					.size = 0x10,
					.start_offset = 0x0030,
					.data = parse_hex_string("81E087BB91E088B3892788BB2FEF34E3"),
			},
			{
					.size = 0x10,
					.start_offset = 0x0040,
					.data = parse_hex_string("8CE0215030408040E1F700C00000F3CF"),
			},
			{
					.size = 0x04,
					.start_offset = 0x0050,
					.data = parse_hex_string("F894FFCF"),
			}
	};
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
	esp_err_t  status;
//	esp_err_t  status = program(&config, 1, &test_chunk);
//	ESP_LOGI("main", "Programming status: %s", esp_err_to_name(status));
	  status = program(&config, 6, chunks);
	ESP_LOGI("main", "Programming status: %s", esp_err_to_name(status));

	while (1) {
		vTaskDelay(portMAX_DELAY);
	}
}

void app_main(void) {
	// run the actual main function with top priority
	xTaskCreate(app_main_actual, "app_main_actual", 4096, NULL, tskIDLE_PRIORITY, NULL);
}
