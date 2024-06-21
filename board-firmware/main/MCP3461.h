//
// Created by alec on 6/2/24.
//

#ifndef ESP_NOW_RELAY_MCP3461_H
#define ESP_NOW_RELAY_MCP3461_H

#include <stdint.h>
#include <stdbool.h>

#define SPI_CLOCK_RATE 1000000

typedef enum {
	REG_ADCDATA = 0x00,
	REG_CONFIG0 = 0x01,
	REG_CONFIG1 = 0x02,
	REG_CONFIG2 = 0x03,
	REG_CONFIG3 = 0x04,
	REG_IRQ = 0x05,
	REG_MUX = 0x06,
	REG_SCAN = 0x07,
	REG_TIMER = 0x08,
	REG_OFFSETCAL = 0x09,
	REG_GAINCAL = 0x0A
} mcp3461_register;

typedef enum {
	ADC_MODE_SHUTDOWN = 0b00,
	ADC_MODE_STANDBY = 0b10,
	ADC_MODE_CONVERSION = 0b11
} adc_mode;

typedef enum {
	ADC_BIAS_NONE,
	ADC_BIAS_0p9uA,
	ADC_BIAS_3p7uA,
	ADC_BIAS_15p0uA
} adc_bias;

typedef enum {
	CLK_SEL_EXTERNAL = 0b00,
	CLK_SEL_INTERNAL = 0b10,
	CLK_SEL_INTERNAL_AMCLK = 0b11
} clk_sel;

typedef enum {
	OSR_32 = 0b0000,
	OSR_64 = 0b0001,
	OSR_128 = 0b0010,
	OSR_256 = 0b0011,
	OSR_512 = 0b0100,
	OSR_1024 = 0b0101,
	OSR_2048 = 0b0110,
	OSR_4096 = 0b0111,
	OSR_8192 = 0b1000,
	OSR_16384 = 0b1001,
	OSR_20480 = 0b1010,
	OSR_24576 = 0b1011,
	OSR_40960 = 0b1100,
	OSR_49152 = 0b1101,
	OSR_81920 = 0b1110,
	OSR_98304 = 0b1111
} adc_osr;

typedef enum {
	GAIN_THIRD = 0b000,
	GAIN_1 = 0b001,
	GAIN_2 = 0b010,
	GAIN_4 = 0b011,
	GAIN_8 = 0b100,
	GAIN_16 = 0b101,
	GAIN_32 = 0b110,
	GAIN_64 = 0b111
} adc_gain;

typedef enum {
	CONV_ONE_SHOT_SHUTDOWN = 0b00,
	CONV_ONE_SHOT_STANDBY = 0b10,
	CONV_CONTINUOUS = 0b11
} conv_mode;

typedef enum {
	MUX_CH0 = 0b0000,
	MUX_CH1 = 0b0001,
	MUX_CH2 = 0b0010,
	MUX_CH3 = 0b0011,
	MUX_CH4 = 0b0100,
	MUX_CH5 = 0b0101,
	MUX_CH6 = 0b0110,
	MUX_CH7 = 0b0111,

	MUX_AGND = 0b1000,
	MUX_AVDD = 0b1001,
	MUX_REFINP = 0b1011,
	MUX_REFINM = 0b1100,

	MUX_TEMP_P = 0b1101,
	MUX_TEMP_M = 0b1110,

	MUX_VCM = 0b1111
} mux_pin;

#define __BIT(n) (1 << (n))
typedef enum {
	CHAN_CH0 = __BIT(0b0000),
	CHAN_CH1 = __BIT(0b0001),
	CHAN_CH2 = __BIT(0b0010),
	CHAN_CH3 = __BIT(0b0011),
	CHAN_CH4 = __BIT(0b0100),
	CHAN_CH5 = __BIT(0b0101),
	CHAN_CH6 = __BIT(0b0110),
	CHAN_CH7 = __BIT(0b0111),

	CHAN_0_1 = __BIT(0b1000),
	CHAN_2_3 = __BIT(0b1001),
	CHAN_4_5 = __BIT(0b1010),
	CHAN_6_7 = __BIT(0b1011),

	CHAN_TEMP = __BIT(0b1100),
	CHAN_AVDD = __BIT(0b1101),
CHAN_VCM = __BIT(0b1110),
CHAN_OFF = __BIT(0b1111)
} scan_chan;
#undef __BIT

inline uint8_t adc_config0(adc_mode mode, adc_bias bias, bool partial_shutdown, clk_sel clk, bool internal_vref) {
	return (internal_vref << 7) | (partial_shutdown << 6) | (clk << 4) | (bias << 2) | mode;
}

inline uint8_t adc_config1(adc_osr osr) {
	return (0b00 << 6) | (osr << 2) | 0b00;
}

inline uint8_t adc_config2(adc_gain gain) {
	return (0b10 << 6) | (gain << 3) | (0b0 << 2) | (0b0 << 1) | 0b1;
}

inline uint8_t adc_config3(conv_mode conv_mode, bool offset_cal, bool gain_cal) {
	return (conv_mode << 7) | (0b11 << 5) | (offset_cal << 1) | gain_cal;
}

inline uint8_t adc_mux(mux_pin minus, mux_pin plus) {
	return (minus << 4) | plus;
}

inline uint32_t adc_scan(uint16_t channels) {
	return channels;
}

typedef struct {
	spi_host_device_t spi_host;
} mcp3461_handle_t;

// (32768 * gain) * offsetcal_volts / V_ref = offsetcal_bits
// (32768 * 1) * -1.65 / 3.3

#endif //ESP_NOW_RELAY_MCP3461_H
