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
	if (len % 2 != 0) {
		return NULL;
	}

	size_t size = len / 2;
	uint8_t *data = malloc(size);
	if (data == NULL) {
		return NULL;
	}

	for (size_t i = 0; i < len; i += 2) {
		char hex[3] = {hex_string[i], hex_string[i + 1], '\0'};
		data[i / 2] = strtol(hex, NULL, 16);
	}

	return data;
}
void app_main(void) {
	esp_log_level_set("avrisp", ESP_LOG_DEBUG);

	struct bitbang_spi_config config = (struct bitbang_spi_config) {
		.mosi = 19, .miso = 22, .clk = 23, .rst = 4, .clock_rate = 1000000 / 6
	};

	uint8_t test_page[128];
	for (size_t i = 0; i < 128; i++) {
		test_page[i] = 0xaa;
	}
	// :100000000EC015C014C013C012C011C010C00FC064
	//:100010000EC00DC00CC00BC00AC009C008C011241E
	//:100020001FBECFE5D2E0DEBFCDBF02D011C0E8CF0A
	//:1000300081E087BB91E088B3892788BB2FEF34E349
	//:100040008CE0215030408040E1F700C00000F3CF49
	//:04005000F894FFCF52
	//:00000001FF
	struct chunk chunks[] = {
			{
					.size = 16,
					.start_offset = 0,
					.data = parse_hex_string("0EC015C014C013C012C011C010C00FC064"),
			}, {
					.size = 0x10,
					.start_offset = 0x0010,
					.data = parse_hex_string("0EC00DC00CC00BC00AC009C008C01124"),
			}, {
					.size = 0x10,
					.start_offset = 0x0020,
					.data = parse_hex_string("1FBECFE5D2E0DEBFCDBF02D011C0E8CF"),
			}, {
					.size = 0x10,
					.start_offset = 0x0030,
					.data = parse_hex_string("81E087BB91E088B3892788BB2FEF34E3"),
			}, {
					.size = 0x10,
					.start_offset = 0x0040,
					.data = parse_hex_string("8CE0215030408040E1F700C00000F3CF"),
			}, {
					.size = 0x04,
					.start_offset = 0x0050,
					.data = parse_hex_string("F894FFCF"),
			}
	};

	esp_err_t  status = program(&config, 1, chunks);
	ESP_LOGI("main", "Programming status: %s", esp_err_to_name(status));
	while (1) {
		vTaskDelay(portMAX_DELAY);
	}
}