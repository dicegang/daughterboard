#include <driver/gpio.h>
#include <freertos/FreeRTOS.h>
#include <freertos/task.h>
#include <portmacro.h>
#include <driver/spi_master.h>
#include <esp_log.h>
#include <string.h>
#include "avrisp.h"
#include "command.h"
#include "../../protocol.h"

#define TAG "avrisp"

#define PAGE_SIZE_WORDS 32
#define PAGE_SIZE_BYTES (PAGE_SIZE_WORDS*2)

static esp_err_t verify_chunk(const struct bitbang_spi_config *config, const struct chunk *chunk);

static void write_chunk_(const struct bitbang_spi_config *config, const struct chunk *chunk);

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

static void bb_spi_flash(struct bitbang_spi_config const *config, size_t addr, uint8_t data) {
	size_t addr_words = addr / 2;
	bool high = addr & 1;

	bb_spi_write4(config, 0x40 + 8 * high, addr_words >> 8 & 0xFF, addr_words & 0xFF, data);
}

static void bb_spi_commit(struct bitbang_spi_config const *config, size_t addr) {
	size_t addr_words = addr / 2;

	bb_spi_write4(config, 0x4C, (addr_words >> 8) & 0xFF, addr_words & 0xFF, 0);
	vTaskDelay(30 / portTICK_PERIOD_MS);
}

static uint8_t bb_spi_read(struct bitbang_spi_config const *config, size_t addr) {
	size_t addr_words = addr / 2;
	bool high = addr & 1;

	return bb_spi_write4(config, 0x20 + high * 8, (addr_words >> 8) & 0xFF, addr_words & 0xFF, 0);
}

static void bb_spi_read_bulk(struct bitbang_spi_config const *config, size_t address, uint8_t *buffer, size_t length) {
	for (size_t x = 0; x < length; x ++) {
		buffer[x] = bb_spi_read(config, address + x);
	}
}

static void write_chunk_(const struct bitbang_spi_config *config, const struct chunk *chunk) {
	size_t prev_pg_idx = chunk->start_offset / PAGE_SIZE_BYTES;
	for (int c_off = 0; c_off < chunk->size; ) {
		size_t curr_addr = chunk->start_offset + c_off;
		size_t curr_pg_idx = curr_addr / PAGE_SIZE_BYTES;

		if (curr_pg_idx != prev_pg_idx) {
			bb_spi_commit(config, prev_pg_idx*PAGE_SIZE_WORDS);
			prev_pg_idx = curr_pg_idx;
		}

		bb_spi_flash(config, curr_addr, chunk->data[c_off++]);
	}

	bb_spi_commit(config, prev_pg_idx*PAGE_SIZE_WORDS);
}

static esp_err_t write_verify_chunk(struct bitbang_spi_config const* config, struct chunk const *chunk) {
	write_chunk_(config, chunk);

	return verify_chunk(config, chunk);
}

static esp_err_t verify_chunk(const struct bitbang_spi_config *config, const struct chunk *chunk) {
	uint8_t *verif_buffer = malloc(chunk->size);
	bb_spi_read_bulk(config, chunk->start_offset / 2, verif_buffer, chunk->size);

	esp_err_t result;
	if (memcmp(chunk->data, verif_buffer, chunk->size) != 0) {
		result = ESP_ERR_INVALID_CRC;
	} else {
		result = ESP_OK;
	}

	free(verif_buffer);
	return result;
}

esp_err_t program(struct bitbang_spi_config *spi_config, size_t chunks, struct chunk const *data) {
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
		ESP_LOGD(TAG, "Not in synch");
	}

	for (size_t c = 0; c < chunks; c++) {
		esp_err_t result;
		for (int i = 0; i < 2; i++) {
			result = write_verify_chunk(spi_config, &data[c]);
			if (result == ESP_OK)
				break;
			ESP_LOGW(TAG, "failed to write chunk; retrying");
		}

		if (result != ESP_OK)
			return result;
	}

	return ESP_OK;
}