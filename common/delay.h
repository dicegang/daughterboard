#ifndef DELAY_H
#define DELAY_H

#include <freertos/FreeRTOS.h>
#include <freertos/task.h>
#include <esp_rom_sys.h>

#define DELAY_US(us) do { \
	if (pdMS_TO_TICKS((us) / 1000) > 0) vTaskDelay(pdMS_TO_TICKS((us) / 1000)); \
	esp_rom_delay_us((us) - pdTICKS_TO_MS(pdMS_TO_TICKS((us) / 1000)) * 1000); \
} while (0)

#define DELAY_MS(ms) do { \
	if (pdMS_TO_TICKS(ms) > 0) vTaskDelay(pdMS_TO_TICKS(ms)); \
	esp_rom_delay_us(1000 * ((ms) - pdTICKS_TO_MS(pdMS_TO_TICKS(ms)))); \
} while (0)

#endif //DELAY_H