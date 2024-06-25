#ifndef IO_H
#define IO_H

#include <driver/gpio.h>
#include <driver/spi_master.h>

#define SRC_EN_1 GPIO_NUM_0
#define LOAD_EN_1 GPIO_NUM_1
#define SUPPLY_EN_1 GPIO_NUM_2
#define SRC_FAULT_1 GPIO_NUM_4
#define LOAD_FAULT_1 GPIO_NUM_3

#define SRC_EN_2 GPIO_NUM_38
#define LOAD_EN_2 GPIO_NUM_37
#define SUPPLY_EN_2 GPIO_NUM_36
#define SRC_FAULT_2 GPIO_NUM_14
#define LOAD_FAULT_2 GPIO_NUM_15

#define MISO_1 GPIO_NUM_6
#define SCLK_1 GPIO_NUM_8
#define MOSI_1 GPIO_NUM_9
#define SRC_CS_1 GPIO_NUM_7
#define LOAD_CS_1 GPIO_NUM_5
#define SRC_RST_1 GPIO_NUM_11
#define LOAD_RST_1 GPIO_NUM_10

#define MISO_2 GPIO_NUM_19
#define SCLK_2 GPIO_NUM_21
#define MOSI_2 GPIO_NUM_20
#define SRC_CS_2 GPIO_NUM_35
#define LOAD_CS_2 GPIO_NUM_34
#define SRC_RST_2 GPIO_NUM_26
#define LOAD_RST_2 GPIO_NUM_33

#define ADC_CS GPIO_NUM_18

#define SIDE1_SPI SPI2_HOST
#define SIDE2_SPI SPI3_HOST
#define ADC_SPI SPI3_HOST

#endif