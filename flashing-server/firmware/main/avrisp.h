/*
AVR In-System Programming over WiFi for ESP8266
Copyright (c) Kiril Zyapkov <kiril@robotev.com>

Original version:
    ArduinoISP version 04m3
    Copyright (c) 2008-2011 Randall Bohn
    If you require a license, see
        http://www.opensource.org/licenses/bsd-license.php
*/

#ifndef _ESP_AVRISP_H
#define _ESP_AVRISP_H

#include <stdint.h>
#include "../protocol.h"
#include <esp_err.h>

struct chunk {
	uint16_t start_offset;
	uint8_t size;
	uint8_t *data;
};

esp_err_t program(struct node_spi_config *spi_config, size_t chunks, struct chunk const *data);

#endif // _ESP_AVRISP_H