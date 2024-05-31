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

typedef struct flash_list_node {
	struct flash_list_node *next;
	uint8_t page_idx;
	uint8_t data[PAGE_SIZE_BYTES];
} flash_list_node;

static void page_list_free(flash_list_node **list) {
	for (struct flash_list_node *node = *list; node;) {
		struct flash_list_node *next = node->next;
		free(node);
		node = next;
	}
}

static uint8_t* page_list_get_or_append(flash_list_node **list, uint8_t page_idx) {
	for (struct flash_list_node *node = *list; node; node = node->next) {
		if (node->page_idx == page_idx) {
			return node->data;
		}
	}

	struct flash_list_node *node = calloc(sizeof(struct flash_list_node), 1);
	node->page_idx = page_idx;
	node->next = *list;
	*list = node;
	return node->data;
}

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


static bool bb_spi_rdy(struct bitbang_spi_config const *config) {
	uint8_t output[4];
	bb_spi_write4(config, 0xf0, 0x00, 0x00, 0, output);
	return output[3];
}

static void wait_for_rdy(struct bitbang_spi_config const *config) {
//	do {
//		esp_rom_delay_us(100);
//	} while (!bb_spi_rdy(config));
	esp_rom_delay_us(5000);
}

static void bb_spi_flash(struct bitbang_spi_config const *config, size_t addr, uint8_t data) {
	size_t addr_words = addr / 2;
	bool high = addr & 1;

	wait_for_rdy(config);
	bb_spi_write4(config, 0x40 + 8 * high, (addr_words >> 8) & 0xFF, addr_words & 0xFF, data, NULL);
	wait_for_rdy(config);
}

static void bb_spi_commit(struct bitbang_spi_config const *config, size_t addr) {
	size_t addr_words = addr / 2;

	wait_for_rdy(config);
	bb_spi_write4(config, 0x4C, (addr_words >> 8) & 0xFF, addr_words & 0xFF, 0, NULL);
	wait_for_rdy(config);
}

static uint8_t bb_spi_read(struct bitbang_spi_config const *config, size_t addr) {
	wait_for_rdy(config);
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

static void write_page(const struct bitbang_spi_config *config, uint8_t page, uint8_t *data) {
	for (size_t i = 0; i < PAGE_SIZE_BYTES; i++) {
		bb_spi_flash(config, i + page*PAGE_SIZE_BYTES, data[i]);
	}

	bb_spi_commit(config, page*PAGE_SIZE_BYTES);
}


static esp_err_t verify_page(const struct bitbang_spi_config *config, uint8_t page, uint8_t *data) {
	uint8_t *verif_buffer = malloc(PAGE_SIZE_BYTES);
	bb_spi_read_bulk(config, page*PAGE_SIZE_BYTES, verif_buffer, PAGE_SIZE_BYTES);

	// print it out
	for (int i = 0; i < PAGE_SIZE_BYTES; i++) {
		ESP_LOGD(TAG, "%02x ", verif_buffer[i]);
	}

	esp_err_t result;
	int diff = memcmp(data, verif_buffer, PAGE_SIZE_BYTES);
	if (diff != 0) {

		// find where mismatch is
		for (int i = 0; i < PAGE_SIZE_BYTES; i++) {
			if (data[i] != verif_buffer[i]) {
				ESP_LOGW(TAG, "Mismatch at %d: %02x != %02x", i + PAGE_SIZE_BYTES*page, data[i], verif_buffer[i]);
			}
		}
		result = ESP_ERR_INVALID_CRC;
	} else {
		result = ESP_OK;
	}

	free(verif_buffer);
	return result;
}

static esp_err_t write_verify_page(struct bitbang_spi_config const* config, uint8_t page, uint8_t *data) {
	write_page(config, page, data);
	esp_rom_delay_us(4500);
	wait_for_rdy(config);
	return verify_page(config, page, data);
}

static void create_list_from_chunks(flash_list_node **head, struct chunk const *chunk, size_t chunk_count) {
	for (size_t i = 0; i < chunk_count; i++) {
		// split chunk into pages
		size_t current_address = chunk[i].start_offset;
		while (current_address < (chunk[i].start_offset + chunk[i].size)) {
			size_t page_offset = current_address % PAGE_SIZE_BYTES;
			size_t page_index = current_address / PAGE_SIZE_BYTES;

			uint8_t *page = page_list_get_or_append(head, page_index);
			memcpy(page + page_offset, chunk[i].data + (current_address - chunk[i].start_offset), PAGE_SIZE_BYTES - page_offset);

			current_address += PAGE_SIZE_BYTES - page_offset;
		}
	}
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
		esp_rom_delay_us(1000);
		gpio_set_level(spi_config->rst, 0);

		esp_rom_delay_us(50 * 1000);

		uint8_t response[4];
		bb_spi_write4(spi_config, 0xAC, 0x53, 0x00, 0x00, response);
		if (response[2] == 0x53) {
			break;
		}
		ESP_LOGW(TAG, "Not in synch");
	}
	esp_rom_delay_us(18 * 1000);
	bb_spi_write4(spi_config, 0x30, 0x00, 0x00, 0x00, NULL);
	esp_rom_delay_us(8 * 1000);
	bb_spi_write4(spi_config, 0x30, 0x00, 0x01, 0x00, NULL);
	esp_rom_delay_us(8 * 1000);
	bb_spi_write4(spi_config, 0x30, 0x00, 0x02, 0x00, NULL);
	esp_rom_delay_us(8 * 1000);
	bb_spi_write4(spi_config, 0xAC, 0x80, 0x00, 0x00, NULL);
	esp_rom_delay_us(56 * 1000);

	ESP_LOGI(TAG, "entered programming mode");

	flash_list_node *list = NULL;
	create_list_from_chunks(&list, data, chunks);
	ESP_LOGI(TAG, "Created list of %d pages", list ? list->page_idx + 1 : 0);
	for (struct flash_list_node *node = list; node; node = node->next) {
		esp_err_t result;
		ESP_LOGI(TAG, "Writing page %d", node->page_idx);
		for (int i = 0; i < 2; i++) {
			result = write_verify_page(spi_config, node->page_idx, node->data);
			if (result == ESP_OK)
				break;
			ESP_LOGW(TAG, "failed to write chunk; retrying");
		}

		if (result != ESP_OK) {
			page_list_free(&list);
			return result;
		}
	}

	gpio_set_level(spi_config->rst, 1);

	page_list_free(&list);
	return ESP_OK;
}