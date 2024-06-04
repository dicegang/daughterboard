//
// Created by alec on 6/2/24.
//

#ifndef ESP_NOW_RELAY_DEVICE_H
#define ESP_NOW_RELAY_DEVICE_H

#include <stdint.h>
#include <stdbool.h>
#include "esp_err.h"

#include "../protocol.h"

#define SHIFT_REGISTER_ADDRESS 0x00

void device_init();
esp_err_t set_shutdown(struct node_spi_config const *config, bool shutdown);
esp_err_t set_engaged(struct node_spi_config const *config, bool engaged);
esp_err_t read_state(struct node_spi_config const *config, struct node_state *state);

#endif //ESP_NOW_RELAY_DEVICE_H
