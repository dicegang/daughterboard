#include "host_communications.h"
#include "comms.h"
#include "protocol.h"

#include <driver/spi_slave.h>
#include <string.h>
#include <esp_log.h>
#include "driver/uart.h"
#include "driver/gpio.h"

#define TAG "HOST_COMMS"

#define BUF_SIZE (1024)

#define UART_PORT UART_NUM_1
#define BAUD_RATE 115200

SemaphoreHandle_t rx_mutex, tx_mutex;

void host_communications_init() {
	rx_mutex = xSemaphoreCreateMutex();
	tx_mutex = xSemaphoreCreateMutex();

	uart_config_t uart_config = {
			.baud_rate = BAUD_RATE,
			.data_bits = UART_DATA_8_BITS,
			.parity    = UART_PARITY_DISABLE,
			.stop_bits = UART_STOP_BITS_1,
			.flow_ctrl = UART_HW_FLOWCTRL_DISABLE,
			.source_clk = UART_SCLK_DEFAULT,
	};
	int intr_alloc_flags = 0;

	ESP_ERROR_CHECK(uart_driver_install(UART_PORT, BUF_SIZE * 2, 2048, 0, NULL, intr_alloc_flags));
	ESP_ERROR_CHECK(uart_param_config(UART_PORT, &uart_config));
	ESP_ERROR_CHECK(uart_set_pin(UART_PORT, 26, 27, GPIO_NUM_NC, GPIO_NUM_NC));
}


esp_err_t host_receive_message(struct comms_request *request) {
	esp_err_t err = ESP_OK;
	if (xSemaphoreTake(rx_mutex, portMAX_DELAY) == pdTRUE) {
		if (uart_read_bytes(UART_PORT, &request->type, sizeof(request->type), portMAX_DELAY) != sizeof(request->type)) {
			err = ESP_ERR_INVALID_ARG;
			goto done_semaphore;
		}

		if (uart_read_bytes(UART_PORT, &request->recipient, sizeof(request->recipient), 20 / portTICK_PERIOD_MS) != sizeof(request->recipient)) {
			err = ESP_ERR_INVALID_ARG;
			goto done_semaphore;
		}

		if (uart_read_bytes(UART_PORT, &request->message_size, sizeof(request->message_size), 20 / portTICK_PERIOD_MS) != sizeof(request->message_size)) {
			err = ESP_ERR_INVALID_ARG;
			goto done_semaphore;
		}

		if (uart_read_bytes(UART_PORT, &request->message, request->message_size, 20 / portTICK_PERIOD_MS) != request->message_size) {
			err = ESP_ERR_INVALID_ARG;
			goto done_semaphore;
		}

		done_semaphore:
		xSemaphoreGive(rx_mutex);
	} else {
		ESP_LOGE(TAG, "Failed to take host_comms_mutex");
		err = ESP_FAIL;
	}

	return err;
}

esp_err_t host_send_message(struct comms_message const *msg) {
	esp_err_t err = ESP_OK;
	if (xSemaphoreTake(tx_mutex, portMAX_DELAY) == pdTRUE) {
		if (uart_write_bytes(UART_PORT, (const char *) msg, sizeof(struct comms_message) + msg->data_size)
				!= (sizeof(struct comms_message) + msg->data_size))
			err = ESP_ERR_INVALID_ARG;

		xSemaphoreGive(tx_mutex);
	} else {
		ESP_LOGE(TAG, "Failed to take host_comms_mutex");
		err = ESP_FAIL;
	}

	return err;
}