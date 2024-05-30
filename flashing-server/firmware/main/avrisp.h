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

esp_err_t program(struct bitbang_spi_config *spi_config, uint8_t const *data, size_t size);

#endif // _ESP_AVRISP_H