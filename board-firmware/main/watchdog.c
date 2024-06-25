#include "watchdog.h"

#include "MCP3461.h"
#include "io.h"

#include <freertos/FreeRTOS.h>
#include <freertos/task.h>
#include <freertos/semphr.h>
#include <freertos/queue.h>

//static mcp3461_device_t adc;
//
//static void watchdog_init(void) {
//	ESP_ERROR_CHECK(mcp3461_create(&adc, ADC_SPI, ADC_CS, 1));
//	ESP_ERROR_CHECK(mcp3461_init(&adc));
//}
//
//void watchdog_loop(void) {
//	watchdog_init();
//
//	for (;;) {
//
//		xTaskNotifyWait(0x00, ULONG_MAX, )
//	}
//}