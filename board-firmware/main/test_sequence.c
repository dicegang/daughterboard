//
// Created by alec on 5/30/24.
//

#include <stddef.h>
#include "avrisp.h"
#include <string.h>
#include <esp_log.h>
#include <freertos/FreeRTOS.h>
#include <freertos/task.h>
#include <driver/spi_master.h>
#include <driver/gpio.h>
#include <delay.h>

#include "io.h"

void blink_gpio(uint8_t pin, bool value) {
	gpio_set_direction(pin, GPIO_MODE_OUTPUT);
	gpio_set_level(pin, value);
	DELAY_MS(500);
	gpio_set_level(pin, !value);
}

void app_main(void) {
	puts("Test sequence started");
	puts("Source disable LED");
	blink_gpio(SRC_EN_1, false);
	puts("Source enable LED");
	blink_gpio(SRC_EN_1, true);
	puts("Source fault LED");
	blink_gpio(SRC_FAULT_1, true);
	puts("Source supply indicators");
	blink_gpio(SUPPLY_EN_1, true);

	puts("Load disable LED");
	blink_gpio(LOAD_EN_1, false);
	puts("Load enable LED");
	blink_gpio(LOAD_EN_1, true);
	puts("Load fault LED");
	blink_gpio(LOAD_FAULT_1, true);
}
