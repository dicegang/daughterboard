//
// Created by alec on 6/2/24.
//

#ifndef ESP_NOW_RELAY_NODE_CONFIG_H
#define ESP_NOW_RELAY_NODE_CONFIG_H

#include "esp_err.h"
#include "../protocol.h"

esp_err_t write_device_config(struct device_configuration *config);
esp_err_t load_device_config(struct device_configuration *config);
esp_err_t load_node_hw(uint8_t id, struct node_spi_config *cfg);
esp_err_t load_node_info(uint8_t id, struct node_info *info);

#endif //ESP_NOW_RELAY_NODE_CONFIG_H
