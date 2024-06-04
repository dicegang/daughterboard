#include <driver/gpio.h>
#include <freertos/FreeRTOS.h>
#include <freertos/task.h>
#include "driver/spi_master.h"

#define LOG_LOCAL_LEVEL ESP_LOG_VERBOSE

#include <esp_log.h>
#include <string.h>
#include <errno.h>

#include "delay.h"
#include "avrisp.h"
#include "command.h"

#define TAG "avrisp"

#define PAGE_SIZE_WORDS 32
#define PAGE_SIZE_BYTES (PAGE_SIZE_WORDS*2)

typedef struct flash_list_node {
	struct flash_list_node *fd;
	uint8_t page_idx;
	uint8_t data[PAGE_SIZE_BYTES];
} flash_list_node;

typedef struct {
	flash_list_node *head;
	size_t size;
} flash_list;

typedef struct {
	spi_device_handle_t spi_attiny;
} spi_session;

static void flash_list_create(flash_list *list) {
	list->head = NULL;
	list->size = 0;
}

static void flash_list_free(flash_list *list) {
	for (struct flash_list_node *node = list->head; node;) {
		struct flash_list_node *fd = node->fd;
		node->fd = NULL;

		free(node);
		node = fd;
	}
}

static uint8_t *flash_list_lookup(flash_list *list, uint8_t page_idx) {
	for (struct flash_list_node *node = list->head; node; node = node->fd) {
		if (node->page_idx == page_idx) {
			return node->data;
		}
	}

	return NULL;
}

static esp_err_t flash_list_insert(flash_list *list, uint8_t page_idx) {
	struct flash_list_node *node = calloc(sizeof(struct flash_list_node), 1);
	if (!node)
		return ESP_ERR_NO_MEM;

	node->page_idx = page_idx;
	node->fd = list->head;
	list->head = node;
	list->size++;

	return ESP_OK;
}

static size_t flash_list_size(flash_list *list) {
	return list->size;
}

static esp_err_t isp_spi_begin(struct node_spi_config const *config, spi_session *session) {
	spi_device_interface_config_t dev_cfg = {
			.command_bits = 8,
			.address_bits = 16,
			.dummy_bits = 0,
			.mode = 0,
			.clock_source = SPI_CLK_SRC_DEFAULT,
			.clock_speed_hz = 1000000 / 6,
			.spics_io_num = config->ss_attiny,
			.queue_size = 1,
	};

	esp_err_t result = spi_bus_add_device(config->spi_attiny, &dev_cfg, &session->spi_attiny);
	if (result != ESP_OK)
		return result;

	if ((result = spi_device_acquire_bus(session->spi_attiny, portMAX_DELAY)) != ESP_OK) {
		spi_bus_remove_device(session->spi_attiny);
		return result;
	}

	return ESP_OK;
}

static void isp_spi_end(spi_session *session) {
	spi_device_release_bus(session->spi_attiny);
	spi_bus_remove_device(session->spi_attiny);
	session->spi_attiny = NULL;
}


static bool isp_spi_rdy(spi_session const *session) {
	spi_transaction_t transaction = {
			.flags = SPI_TRANS_CS_KEEP_ACTIVE | SPI_TRANS_USE_TXDATA | SPI_TRANS_USE_RXDATA,
			.cmd = 0xf0,
			.length = 8,
	};

	esp_err_t result = spi_device_transmit(session->spi_attiny, &transaction);
	if (result != ESP_OK) {
		ESP_LOGE(TAG, "isp_spi_rdy: Could not transmit to device: %s", esp_err_to_name(result));
		return false;
	}

	return transaction.rx_data[0];
}

static void isp_spi_flash(spi_session const *session, size_t addr, uint8_t data) {
	size_t addr_words = addr / 2;
	bool high = addr & 1;

	spi_transaction_t transaction = {
			.flags = SPI_TRANS_CS_KEEP_ACTIVE | SPI_TRANS_USE_TXDATA | SPI_TRANS_USE_RXDATA,
			.cmd = high ? 0x48 : 0x40,
			.addr = addr_words,
			.length = 8,
			.tx_data = {data},
	};

	esp_err_t result = spi_device_transmit(session->spi_attiny, &transaction);
	if (result != ESP_OK) {
		ESP_LOGE(TAG, "isp_spi_flash: could not transmit to device: %s", esp_err_to_name(result));
	}
}

static void isp_spi_commit(spi_session const *session, size_t addr) {
	size_t addr_words = addr / 2;

	spi_transaction_t transaction = {
			.flags = SPI_TRANS_CS_KEEP_ACTIVE | SPI_TRANS_USE_TXDATA | SPI_TRANS_USE_RXDATA,
			.cmd = 0x4C,
			.addr = addr_words,
			.length = 8,
	};

	esp_err_t result = spi_device_transmit(session->spi_attiny, &transaction);
	if (result != ESP_OK) {
		ESP_LOGE(TAG, "isp_spi_commit: could not transmit to device: %s", esp_err_to_name(result));
	}

	DELAY_US(5000);
}

static uint8_t isp_spi_read(spi_session const *session, size_t addr) {
	size_t addr_words = addr / 2;
	bool high = addr & 1;

	spi_transaction_t transaction = {
			.flags = SPI_TRANS_CS_KEEP_ACTIVE | SPI_TRANS_USE_TXDATA | SPI_TRANS_USE_RXDATA,
			.cmd = high ? 0x28 : 0x20,
			.addr = addr_words,
			.length = 8,
			.tx_data = {},
	};

	esp_err_t result = spi_device_transmit(session->spi_attiny, &transaction);
	if (result != ESP_OK) {
		ESP_LOGE(TAG, "isp_spi_read: could not transmit to device: %s", esp_err_to_name(result));
		return 0;
	}

	return transaction.rx_data[0];
} //gedagedigedagedi-oh

static bool isp_spi_program(spi_session const *session) {
	spi_transaction_ext_t transaction = {
			{
					.flags = SPI_TRANS_CS_KEEP_ACTIVE | SPI_TRANS_USE_TXDATA | SPI_TRANS_USE_RXDATA |
							 SPI_TRANS_VARIABLE_ADDR |
							 SPI_TRANS_VARIABLE_CMD,
					.cmd = 0xAC,
					.addr = 0x53,
					.length = 16,
					.tx_data = {0xaa, 0xaa},
			},
			.command_bits = 8,
			.address_bits = 8,
			.dummy_bits = 0
	};

	esp_err_t result = spi_device_transmit(session->spi_attiny, (spi_transaction_t *) &transaction);
	if (result != ESP_OK) {
		ESP_LOGE(TAG, "isp_spi_program: could not transmit to device: %s", esp_err_to_name(result));
		return false;
	}
	ESP_LOGI(TAG, "rx_data[0]: %02x", transaction.base.rx_data[0]);
	ESP_LOGI(TAG, "rx_data[1]: %02x", transaction.base.rx_data[1]);

	return transaction.base.rx_data[0] == 0x53;
}

static void isp_spi_read_bulk(spi_session const *session, size_t address, uint8_t *buffer, size_t length) {
	for (size_t x = 0; x < length; x++) {
		buffer[x] = isp_spi_read(session, address + x);
	}
}

static void write_page(spi_session const *session, uint8_t page, uint8_t *data) {
	for (size_t i = 0; i < PAGE_SIZE_BYTES; i++) {
		isp_spi_flash(session, i + page * PAGE_SIZE_BYTES, data[i]);
	}

	isp_spi_commit(session, page * PAGE_SIZE_BYTES);
}


static esp_err_t verify_page(spi_session const *session, uint8_t page, uint8_t *data) {
	esp_err_t result = ESP_OK;

	uint8_t *verif_buffer = malloc(PAGE_SIZE_BYTES);
	if (!verif_buffer) {
		result = ESP_ERR_NO_MEM;
		goto done;
	}

	isp_spi_read_bulk(session, page * PAGE_SIZE_BYTES, verif_buffer, PAGE_SIZE_BYTES);

	// print it out
	for (int i = 0; i < PAGE_SIZE_BYTES; i++) {
		ESP_LOGD(TAG, "%02x ", verif_buffer[i]);
	}

	int diff = memcmp(data, verif_buffer, PAGE_SIZE_BYTES);
	if (diff != 0) {
		// find where mismatch is
		for (int i = 0; i < PAGE_SIZE_BYTES; i++) {
			if (data[i] != verif_buffer[i]) {
				ESP_LOGI(TAG, "Mismatch at %d: %02x != %02x", i + PAGE_SIZE_BYTES * page, data[i], verif_buffer[i]);
			}
		}
		result = ESP_ERR_INVALID_CRC;
	}

	done:
	free(verif_buffer);
	return result;
}

static esp_err_t write_verify_page(spi_session const *session, uint8_t page, uint8_t *data) {
	write_page(session, page, data);
	return verify_page(session, page, data);
}

static esp_err_t create_list_from_chunks(flash_list *list, struct chunk const *chunk, size_t chunk_count) {
	for (size_t i = 0; i < chunk_count; i++) {
		// split chunk into pages
		size_t current_address = chunk[i].start_offset;
		while (current_address < (chunk[i].start_offset + chunk[i].size)) {
			size_t page_offset = current_address % PAGE_SIZE_BYTES;
			size_t page_index = current_address / PAGE_SIZE_BYTES;

			uint8_t *page = flash_list_lookup(list, page_index);
			if (!page) {
				if (flash_list_insert(list, page_index) != ESP_OK) {
					return ESP_ERR_NO_MEM;
				}

				page = flash_list_lookup(list, page_index);
				assert(page);
			}

			size_t write_size = configMIN(PAGE_SIZE_BYTES - page_offset,
										  chunk[i].size - (current_address - chunk[i].start_offset));
			memcpy(page + page_offset, chunk[i].data + (current_address - chunk[i].start_offset), write_size);

			current_address += PAGE_SIZE_BYTES - page_offset;
		}
	}

	return ESP_OK;
}

static esp_err_t write_pages(spi_session const *session, flash_list *list) {
	esp_err_t result = ESP_OK;

	for (struct flash_list_node *node = list->head; node; node = node->fd) {
		ESP_LOGD(TAG, "Writing page %d", node->page_idx);

		for (int i = 0; i < 2; i++) {
			taskYIELD();

			if (i != 0)
				ESP_LOGW(TAG, "failed to write page; retrying");

			if ((result = write_verify_page(session, node->page_idx, node->data)) == ESP_OK)
				break;
		}

		if (result != ESP_OK) {
			goto done;
		}

		ESP_LOGD(TAG, "Page %d written and verified", node->page_idx);
	}

	done:
	return result;
}


esp_err_t program(struct node_spi_config *spi_config, size_t chunks, struct chunk const *data) {
	esp_err_t result = ESP_OK;
	ESP_LOGD(TAG, "Programming %d chunks", chunks);

	spi_session session;
	if ((result = isp_spi_begin(spi_config, &session)) != ESP_OK) {
		return result;
	}

	for (;;) {
		gpio_set_level(spi_config->ss_attiny, 0);
		DELAY_MS(20);
		gpio_set_level(spi_config->ss_attiny, 1);
		// Pulse must be minimum 2 target CPU clock cycles so 100 usec is ok for CPU
		// speeds above 20 KHz
		DELAY_US(100);
		gpio_set_level(spi_config->ss_attiny, 0);
		DELAY_MS(50);

		if (isp_spi_program(&session)) {
			break;
		}

		ESP_LOGW(TAG, "Not in synch");
	}

	DELAY_MS(50);

	flash_list list;
	flash_list_create(&list);
	if ((result = create_list_from_chunks(&list, data, chunks)) != ESP_OK)
		goto done;

	ESP_LOGV(TAG, "Created list of %d pages", flash_list_size(&list));
	if ((result = write_pages(&session, &list)) != ESP_OK)
		goto done;


	done:
	isp_spi_end(&session);
	flash_list_free(&list);
	return result;
}