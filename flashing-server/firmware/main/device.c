////
//// Created by alec on 6/2/24.
////
//
//#include <freertos/FreeRTOS.h>
//#include <freertos/semphr.h>
//
//#include <driver/gpio.h>
//#include "device.h"
//
//#define OPCODE(is_read) (0b01000000 | (SHIFT_REGISTER_ADDRESS << 1) | (is_read ? 1 : 0))
//#define REG_IODIR 0x00
//#define REG_GPIO 0x09
//
//SemaphoreHandle_t pin_lock = NULL;
//static StaticSemaphore_t pin_lock_;
//
//static bool initialised[GPIO_NUM_MAX] = {false};
//
//static void write_shift_register(struct node_spi_config const *config, uint8_t reg, uint8_t value) {
//	gpio_set_level(config->ss_sr, 0);
////	spi_transmit(config->spi_sr, (uint8_t[]){OPCODE(false), reg, value}, 3, NULL, 0);
//	gpio_set_level(config->ss_sr, 1);
//}
//
//static uint8_t read_shift_register(struct node_spi_config const *config, uint8_t reg) {
//	uint8_t data[3];
//
//	gpio_set_level(config->ss_sr, 0);
////	spi_transmit(config->spi_sr, (uint8_t[]){OPCODE(true), reg}, 2, data, 3);
//	gpio_set_level(config->ss_sr, 1);
//	return data[2];
//}
//
//static void set_pin_shift_register(struct node_spi_config const *config, uint8_t pin, bool value) {
//	static uint8_t cache[GPIO_NUM_MAX] = {0};
//
//	xSemaphoreTake(pin_lock, portMAX_DELAY);
//	if (!initialised[config->ss_sr]) {
//		gpio_set_direction(config->ss_sr, GPIO_MODE_OUTPUT);
//		write_shift_register(config, REG_IODIR, 0xff);
//		initialised[config->ss_sr] = true;
//	}
//
//	if (value) {
//		cache[config->ss_sr] |= 1 << pin;
//	} else {
//		cache[config->ss_sr] &= ~(1 << pin);
//	}
//	write_shift_register(config, REG_GPIO, cache[config->ss_sr], value);
//	xSemaphoreGive(&pin_lock);
//}
//
//void device_init() {
//	pin_lock = xSemaphoreCreateMutexStatic(&pin_lock_);
//}
//
//esp_err_t set_shutdown(struct node_spi_config const *config, bool shutdown) {
//	set_pin_shift_register(config, config->shutdown_pin_sr, shutdown);
//	return ESP_OK;
//}
//
//esp_err_t set_engaged(struct node_spi_config const *config, bool engaged) {
//	set_pin_shift_register(config, config->engaged_pin_sr, engaged);
//	return ESP_OK;
//}
//
//esp_err_t read_state(struct node_spi_config const *config, struct node_state *state) {
//	return ESP_OK;
//}
