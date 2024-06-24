////
//// Created by alec on 6/2/24.
////

#include <freertos/FreeRTOS.h>
#include <freertos/semphr.h>

#include <driver/gpio.h>
#include "device.h"

#define OPCODE(is_read) (0b01000000 | (SHIFT_REGISTER_ADDRESS << 1) | (is_read ? 1 : 0))
#define REG_IODIR 0x00
#define REG_GPIO 0x09


void device_init() {
//	pin_lock = xSemaphoreCreateMutexStatic(&pin_lock_);
}

esp_err_t set_shutdown(struct node_spi_config const *config, bool shutdown) {
//	set_pin_shift_register(config, config->shutdown_pin_sr, shutdown);
	return ESP_OK;
}

esp_err_t set_engaged(struct node_spi_config const *config, bool engaged) {
//	set_pin_shift_register(config, config->engaged_pin_sr, engaged);
	return ESP_OK;
}

esp_err_t read_state(struct node_spi_config const *config, struct node_state *state) {
	return ESP_OK;
}
