/// @file MCP3461.h
/// @brief Driver for the MCP3461/2/4-R family of ADCs

#ifndef ESP_NOW_RELAY_MCP3461_H
#define ESP_NOW_RELAY_MCP3461_H

#include <stdbool.h>
#include <stdint.h>

#include <driver/spi_master.h>
#include <esp_err.h>

#ifdef __cplusplus
extern "C" {
#endif

// XXX(etw): avrisp runs at 1/6 this speed
#define SPI_CLOCK_RATE 1000000

typedef enum {
	MCP3461_ADC_MODE_SHUTDOWN = 0b00,
	MCP3461_ADC_MODE_STANDBY = 0b10,
	MCP3461_ADC_MODE_CONVERSION = 0b11
} adc_mode;

typedef enum {
	MCP3461_ADC_BIAS_NONE,
	MCP3461_ADC_BIAS_0p9uA,
	MCP3461_ADC_BIAS_3p7uA,
	MCP3461_ADC_BIAS_15p0uA
} adc_bias;

typedef enum {
	MCP3461_CLK_SEL_EXTERNAL = 0b00,
	MCP3461_CLK_SEL_INTERNAL = 0b10,
	MCP3461_CLK_SEL_INTERNAL_AMCLK = 0b11
} clk_sel;

typedef enum {
	MCP3461_OSR_32 = 0b0000,
	MCP3461_OSR_64 = 0b0001,
	MCP3461_OSR_128 = 0b0010,
	MCP3461_OSR_256 = 0b0011,
	MCP3461_OSR_512 = 0b0100,
	MCP3461_OSR_1024 = 0b0101,
	MCP3461_OSR_2048 = 0b0110,
	MCP3461_OSR_4096 = 0b0111,
	MCP3461_OSR_8192 = 0b1000,
	MCP3461_OSR_16384 = 0b1001,
	MCP3461_OSR_20480 = 0b1010,
	MCP3461_OSR_24576 = 0b1011,
	MCP3461_OSR_40960 = 0b1100,
	MCP3461_OSR_49152 = 0b1101,
	MCP3461_OSR_81920 = 0b1110,
	MCP3461_OSR_98304 = 0b1111
} adc_osr;

typedef enum {
	MCP3461_GAIN_THIRD = 0b000,
	MCP3461_GAIN_1 = 0b001,
	MCP3461_GAIN_2 = 0b010,
	MCP3461_GAIN_4 = 0b011,
	MCP3461_GAIN_8 = 0b100,
	MCP3461_GAIN_16 = 0b101,
	MCP3461_GAIN_32 = 0b110,
	MCP3461_GAIN_64 = 0b111
} adc_gain;

typedef enum {
	MCP3461_CONV_ONE_SHOT_SHUTDOWN = 0b00,
	MCP3461_CONV_ONE_SHOT_STANDBY = 0b10,
	MCP3461_CONV_CONTINUOUS = 0b11
} conv_mode;

typedef enum {
	MCP3461_MUX_CH0 = 0b0000,
	MCP3461_MUX_CH1 = 0b0001,
	MCP3461_MUX_CH2 = 0b0010,
	MCP3461_MUX_CH3 = 0b0011,
	MCP3461_MUX_CH4 = 0b0100,
	MCP3461_MUX_CH5 = 0b0101,
	MCP3461_MUX_CH6 = 0b0110,
	MCP3461_MUX_CH7 = 0b0111,

	MCP3461_MUX_AGND = 0b1000,
	MCP3461_MUX_AVDD = 0b1001,
	MCP3461_MUX_REFINP = 0b1011,
	MCP3461_MUX_REFINM = 0b1100,

	MCP3461_MUX_TEMP_P = 0b1101,
	MCP3461_MUX_TEMP_M = 0b1110,

	MCP3461_MUX_VCM = 0b1111
} mux_pin;

#define __BIT(n) (1 << (n))
typedef enum {
	MCP3461_CHAN_CH0 = __BIT(0b0000),
	MCP3461_CHAN_CH1 = __BIT(0b0001),
	MCP3461_CHAN_CH2 = __BIT(0b0010),
	MCP3461_CHAN_CH3 = __BIT(0b0011),
	MCP3461_CHAN_CH4 = __BIT(0b0100),
	MCP3461_CHAN_CH5 = __BIT(0b0101),
	MCP3461_CHAN_CH6 = __BIT(0b0110),
	MCP3461_CHAN_CH7 = __BIT(0b0111),

	MCP3461_CHAN_0_1 = __BIT(0b1000),
	MCP3461_CHAN_2_3 = __BIT(0b1001),
	MCP3461_CHAN_4_5 = __BIT(0b1010),
	MCP3461_CHAN_6_7 = __BIT(0b1011),

	MCP3461_CHAN_TEMP = __BIT(0b1100),
	MCP3461_CHAN_AVDD = __BIT(0b1101),
	MCP3461_CHAN_VCM = __BIT(0b1110),
	MCP3461_CHAN_OFF = __BIT(0b1111)
} scan_chan;
#undef __BIT

typedef struct __attribute__((packed)) {
	adc_mode mode : 2;
	adc_bias bias : 2;
	clk_sel clk_sel : 2;
	bool partial_shutdown_n : 1;
	bool vref_sel : 1;
} mcp3461_config0;
static_assert(sizeof(mcp3461_config0) == 1, "CONFIG0 register size");

inline uint8_t adc_config0(adc_mode mode, adc_bias bias, bool partial_shutdown,
                           clk_sel clk, bool internal_vref) {
	return (internal_vref << 7) | (partial_shutdown << 6) | (clk << 4) |
	       (bias << 2) | mode;
}

inline uint8_t adc_config1(adc_osr osr) {
	return (0b00 << 6) | (osr << 2) | 0b00;
}

inline uint8_t adc_config2(adc_gain gain) {
	return (0b10 << 6) | (gain << 3) | (0b0 << 2) | (0b0 << 1) | 0b1;
}

// (32768 * gain) * offsetcal_volts / V_ref = offsetcal_bits
// (32768 * 1) * -1.65 / 3.3
inline uint8_t adc_config3(conv_mode conv_mode, bool offset_cal,
                           bool gain_cal) {
	// DATA_FORMAT set to 0b11 (17 bits + channel ID)
	return (conv_mode << 7) | (0b11 << 5) | (offset_cal << 1) | gain_cal;
}

inline uint8_t adc_mux(mux_pin plus, mux_pin minus) {
	return (plus << 4) | minus;
}

inline uint32_t adc_scan(uint16_t channels) { return channels; }

typedef struct {
	spi_device_handle_t spidev;
	uint8_t device_address;
} mcp3461_device_t;

/// @brief Create a new MCP346xR device
///
/// This function opens a device, and configures the driver state for as if the
/// device had just been reset. However, no reset or initialization is
/// performed. `mcp3461_init` should be called to reset and initialize the
/// device
///
/// @param[out] h Device handle to initialize
/// @param[in]  host_id SPI host peripheral ID that the device is on
/// @param[in]  cs_io_num CS GPIO pin of the device, or -1 if not used
/// @param[in]  device_address Address of the device (normal-order parts are 1)
esp_err_t mcp3461_create(mcp3461_device_t *h, spi_host_device_t host_id,
                         int cs_io_num, uint8_t device_address);

/// @brief Reset and initialize the device
///
/// Performs a device reset via a fast command, and then configures the device
/// to a default state.
///
/// @param h Device handle
esp_err_t mcp3461_init(mcp3461_device_t *h);

/// @brief Tear down the device
///
/// Disposes of driver resources associated with the device handle.
///
/// @param h Device handle
esp_err_t mcp3461_dispose(mcp3461_device_t *h);

/// @brief Set the ADC mode
///
/// Issues a fast command to set the desired ADC mode. Use for starting
/// conversions, or placing the device in standby or shutdown.
///
/// @param h Device handle
/// @param mode The mode to set
esp_err_t mcp3461_set_adc_mode(mcp3461_device_t *h, adc_mode mode);

/// @brief Set the channels enabled in scan
///
/// Sets the SCAN register to select which channels are enabled in the ADC's
/// conversion scan. The `channels` parameter is a bitmask obtained by
/// bitwise-OR-ing the desired MCP3461_CHAN_* constants for the desired channels
/// to enable. When `channels` is 0, the device is in mux mode.
///
/// @param h Device handle
/// @param channels A bitmask of enabled channels obtained by |'ing together
///                 MCP3461_CHAN_* constants
esp_err_t mcp3461_set_scan(mcp3461_device_t *h, scan_chan channels);

/// @brief Set the mux inputs for mux mode
///
/// Sets the MUX register to select mux inputs. Does not bring device into mux
/// mode if is not already in mux mode. To bring into mux mode, use
/// `mcp3461_set_scan`.
///
/// @param h Device handle
/// @param positive Pin for VIN+
/// @param negative Pin for VIN-
esp_err_t mcp3461_set_mux(mcp3461_device_t *h, mux_pin positive,
                          mux_pin negative);

// This struct does not necessarily match any register layout, it is just for
// returning data through the driver API
typedef struct {
	int32_t value : 28;
	unsigned channel_id : 4;
} mcp3461_read_data_t;
static_assert(sizeof(mcp3461_read_data_t) == 4, "packing");

/// @brief Read the ADC data
///
/// Reads the current data value in the device's output register.
///
/// @param h Device handle
esp_err_t mcp3461_read(mcp3461_device_t *h, mcp3461_read_data_t *out);

#ifdef __cplusplus
} // extern "C"
#endif

#endif // ESP_NOW_RELAY_MCP3461_H
