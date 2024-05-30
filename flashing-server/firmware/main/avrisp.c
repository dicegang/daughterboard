/*
AVR In-System Programming over WiFi for ESP
Copyright (c) Kiril Zyapkov <kiril@robotev.com>
Converted for ESP32 Larry Bernstone <lbernstone@gmail.com>

Original version:
    ArduinoISP version 04m3
    Copyright (c) 2008-2011 Randall Bohn
    If you require a license, see
        http://www.opensource.org/licenses/bsd-license.php
*/


#include <driver/gpio.h>
#include <freertos/task.h>
#include <portmacro.h>
#include <driver/spi_master.h>
#include <esp_log.h>
#include <string.h>
#include "avrisp.h"
#include "command.h"
#include "../../protocol.h"

static void bb_spi_begin(struct bitbang_spi_config const *config) {
	gpio_set_direction(config->clk, GPIO_MODE_OUTPUT);
	gpio_set_direction(config->mosi, GPIO_MODE_OUTPUT);
	gpio_set_direction(config->miso, GPIO_MODE_INPUT);
}

static uint8_t bb_spi_transfer(struct bitbang_spi_config const *config, uint8_t b) {
	uint32_t pulseWidth = (500000 + config->clock_rate - 1) / config->clock_rate;
	for (unsigned int i = 0; i < 8; ++i) {
		gpio_set_level(config->mosi, (b & 0x80) ? 1 : 0);
		gpio_set_level(config->clk, 1);
		esp_rom_delay_us(pulseWidth);
		b = (b << 1) | gpio_get_level(config->miso);
		gpio_set_level(config->clk, 0);
		esp_rom_delay_us(pulseWidth);
	}
	return b;
}

static uint8_t bb_spi_write4(struct bitbang_spi_config const *config, uint8_t a, uint8_t b, uint8_t c, uint8_t d) {
	bb_spi_transfer(config, a);
	bb_spi_transfer(config, b);
	bb_spi_transfer(config, c);
	return 	bb_spi_transfer(config, d);
}

static void bb_spi_flash(bool high, struct bitbang_spi_config const *config, size_t addr, uint8_t data) {
	bb_spi_write4(config, 0x40 + 8 * high, addr >> 8 & 0xFF, addr & 0xFF, data);
}

static void bb_spi_commit(struct bitbang_spi_config const *config, size_t addr) {
	bb_spi_write4(config, 0x4C, (addr >> 8) & 0xFF, addr & 0xFF, 0);
	vTaskDelay(30 / portTICK_PERIOD_MS);
}

static uint8_t bb_spi_read(struct bitbang_spi_config const *config, bool high, size_t addr) {
	return bb_spi_write4(config, 0x20 + high * 8, (addr >> 8) & 0xFF, addr & 0xFF, 0);
}

static void bb_spi_read_bulk(struct bitbang_spi_config const *config, size_t address, uint8_t *buffer, size_t length) {
	for (size_t x = 0; x < length; x += 2) {
		buffer[x] = bb_spi_read(config, false, address);
		buffer[x + 1] = bb_spi_read(config, true, address);

		address++;
	}
}

esp_err_t program(struct bitbang_spi_config *spi_config, uint8_t const *data, size_t size) {
	bb_spi_begin(spi_config);

	for (;;) {
		gpio_set_level(spi_config->rst, 0);

		gpio_set_level(spi_config->clk, 0);
		vTaskDelay(20 / portTICK_PERIOD_MS);
		gpio_set_level(spi_config->rst, 1);
		// Pulse must be minimum 2 target CPU clock cycles so 100 usec is ok for CPU
		// speeds above 20 KHz
		esp_rom_delay_us(100);
		gpio_set_level(spi_config->rst, 0);

		vTaskDelay(50 / portTICK_PERIOD_MS);
		uint8_t result = bb_spi_write4(spi_config, 0xAC, 0x53, 0x00, 0x00);
		if (result == 0x53) {
			break;
		}
		ESP_LOGD("avrisp", "Not in synch");
	}

	size_t current_word = 0;
	size_t previous_page = 0;

	for (size_t i = 0; i < size; ) {
		size_t current_page = current_word / 32;

		if (current_page != previous_page) {
			bb_spi_commit(spi_config, previous_page);
			previous_page = current_page;
			vTaskDelay(30 / portTICK_PERIOD_MS);
		}

		bb_spi_flash(false, spi_config, current_word, data[i++]);
		bb_spi_flash(true, spi_config, current_word, data[i++]);

		current_word++;
	}

	uint8_t *verif_buffer = malloc(size);
	bb_spi_read_bulk(spi_config, 0, verif_buffer, size);

	esp_err_t result = ESP_OK;
	if (memcmp(data, verif_buffer, size) != 0) {
		result = ESP_ERR_INVALID_CRC;
	}

	free(verif_buffer);
	return result;
}