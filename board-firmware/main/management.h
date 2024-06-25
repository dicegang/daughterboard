//
// Created by alec on 6/2/24.
//

#ifndef ESP_NOW_RELAY_DEVICE_H
#define ESP_NOW_RELAY_DEVICE_H

#include <stdint.h>
#include <stdbool.h>
#include "esp_err.h"

#include <protocol.h>

void device_init(void);

enum trip_reason get_trip_reason(bool side, bool load);

esp_err_t trip(enum trip_reason reason, bool side, bool load);
esp_err_t untrip(bool side, bool load);

esp_err_t set_engaged(bool side, bool load, bool engaged);
esp_err_t read_state(bool side, bool load, struct node_state *state);

#endif //ESP_NOW_RELAY_DEVICE_H
