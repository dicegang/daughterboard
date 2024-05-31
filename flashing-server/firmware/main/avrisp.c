#include <driver/gpio.h>
#include <freertos/FreeRTOS.h>
#include <freertos/task.h>
#include <portmacro.h>
#include <driver/spi_master.h>

#define LOG_LOCAL_LEVEL ESP_LOG_VERBOSE
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
	gpio_set_direction(config->rst, GPIO_MODE_OUTPUT);
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


static void bb_spi_write4(struct bitbang_spi_config const *config, uint8_t a, uint8_t b, uint8_t c, uint8_t d, uint8_t *out) {
	uint8_t a_response = bb_spi_transfer(config, a);
	uint8_t b_response = bb_spi_transfer(config, b);
	uint8_t c_response = bb_spi_transfer(config, c);
	uint8_t d_response = bb_spi_transfer(config, d);

	if (out != NULL) {
		out[0] = a_response;
		out[1] = b_response;
		out[2] = c_response;
		out[3] = d_response;
	}
}

static void bb_spi_flash(struct bitbang_spi_config const *config, size_t addr, uint8_t data) {
	size_t addr_words = addr / 2;
	bool high = addr & 1;

	bb_spi_write4(config, 0x40 + 8 * high, (addr_words >> 8) & 0xFF, addr_words & 0xFF, data, NULL);
}

static void bb_spi_commit(struct bitbang_spi_config const *config, size_t addr) {
	size_t addr_words = addr / 2;

	bb_spi_write4(config, 0x4C, (addr_words >> 8) & 0xFF, addr_words & 0xFF, 0, NULL);
}

static uint8_t bb_spi_read(struct bitbang_spi_config const *config, size_t addr) {
	size_t addr_words = addr / 2;
	bool high = addr & 1;

	uint8_t output[4];
	bb_spi_write4(config, 0x20 + high * 8, (addr_words >> 8) & 0xFF, addr_words & 0xFF, 0, output);
	return output[3];
}

static void bb_spi_read_bulk(struct bitbang_spi_config const *config, size_t address, uint8_t *buffer, size_t length) {
	for (size_t x = 0; x < length; x ++) {
		buffer[x] = bb_spi_read(config, address + x);
	}
}

static void write_chunk_(const struct bitbang_spi_config *config, const struct chunk *chunk) {
	size_t prev_pg_idx = chunk->start_offset / PAGE_SIZE_BYTES;

	size_t curr_addr = prev_pg_idx * PAGE_SIZE_BYTES;
	while (curr_addr < chunk->start_offset) {
		bb_spi_flash(config, curr_addr, 0xFF);
		curr_addr++;
	}

	while (curr_addr < (chunk->start_offset + chunk->size)) {
		size_t curr_pg_idx = curr_addr / PAGE_SIZE_BYTES;

		if (curr_pg_idx != prev_pg_idx) {
			bb_spi_commit(config, prev_pg_idx*PAGE_SIZE_BYTES);
			prev_pg_idx = curr_pg_idx;
		}

		bb_spi_flash(config, curr_addr, chunk->data[curr_addr - chunk->start_offset]);
		curr_addr++;
	}

	while (curr_addr % PAGE_SIZE_BYTES) {
		bb_spi_flash(config, curr_addr, 0xFF);
		curr_addr++;
	}

	bb_spi_commit(config, prev_pg_idx*PAGE_SIZE_BYTES);
}

static esp_err_t write_verify_chunk(struct bitbang_spi_config const* config, struct chunk const *chunk) {
	write_chunk_(config, chunk);
	esp_rom_delay_us(4500);
	return verify_chunk(config, chunk);
}

static esp_err_t verify_chunk(const struct bitbang_spi_config *config, const struct chunk *chunk) {
	if (chunk->size == 0) {
		return ESP_OK;
	}

	uint8_t *verif_buffer = malloc(chunk->size);
	if (verif_buffer == NULL) {
		return ESP_ERR_NO_MEM;
	}

	bb_spi_read_bulk(config, chunk->start_offset, verif_buffer, chunk->size);

	// print it out
	for (int i = 0; i < chunk->size; i++) {
		ESP_LOGI(TAG, "%02x ", verif_buffer[i]);
	}

	esp_err_t result;
	int diff = memcmp(chunk->data, verif_buffer, chunk->size);
	if (diff != 0) {
		// find where mismatch is
		for (int i = 0; i < chunk->size; i++) {
			if (chunk->data[i] != verif_buffer[i]) {
				ESP_LOGW(TAG, "Mismatch at %d: %02x != %02x", i + chunk->start_offset, chunk->data[i], verif_buffer[i]);
			}
		}
		result = ESP_ERR_INVALID_CRC;
	} else {
		result = ESP_OK;
	}

	free(verif_buffer);

	ESP_LOGD(TAG, "Verified chunk at %04x", chunk->start_offset);
	return result;
}

esp_err_t program(struct bitbang_spi_config *spi_config, size_t chunks, struct chunk const *data) {
	ESP_LOGI(TAG, "Programming %d chunks", chunks);
	bb_spi_begin(spi_config);

	for (;;) {
		gpio_set_level(spi_config->rst, 0);

		gpio_set_level(spi_config->clk, 0);
		esp_rom_delay_us(20 * 1000);
		gpio_set_level(spi_config->rst, 1);
		// Pulse must be minimum 2 target CPU clock cycles so 100 usec is ok for CPU
		// speeds above 20 KHz
		esp_rom_delay_us(100);
		gpio_set_level(spi_config->rst, 0);

		esp_rom_delay_us(50 * 1000);
		bb_spi_write4(spi_config, 0xAC, 0x80, 0x00, 0x00, NULL);
		esp_rom_delay_us(50 * 1000);

		uint8_t response[4];
		bb_spi_write4(spi_config, 0xAC, 0x53, 0x00, 0x00, response);
		if (response[2] == 0x53) {
			break;
		}
		ESP_LOGW(TAG, "Not in synch");
	}
	esp_rom_delay_us(50 * 1000);

	ESP_LOGD(TAG, "entered programming mode");

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