#include "MCP3461.h"
#include <assert.h>
#include <driver/spi_common.h>
#include <driver/spi_master.h>
#include <esp_err.h>
#include <string.h>

#define LOG_TAG "MCP3461"

#define sizeof_member(type, member) (sizeof(((type *)0)->member))

#define BIT_FIELDn(n, x, bits, pos)                                            \
	((((uint##n##_t)(x)) & ((1 << bits) - 1)) << pos)
#define BIT_FIELD8(x, bits, pos) BIT_FIELDn(8, x, bits, pos)

static inline int32_t sign_extend32(uint32_t num, size_t n_bits) {
	size_t n_extra_sign_bits = 32 - n_bits;
	return ((int32_t)(num << n_extra_sign_bits)) >> n_extra_sign_bits;
}

typedef enum : uint8_t {
	COMMAND_TYPE_FAST = 0b00,
	COMMAND_TYPE_STATIC_READ = 0b01,
	COMMAND_TYPE_INC_READ = 0b11,
	COMMAND_TYPE_INC_WRITE = 0b10,
} command_type_t;

typedef enum : uint8_t {
	FAST_COMMAND_ADC_CONV_START = 0b1010,
	FAST_COMMAND_ADC_STANDBY_MODE = 0b1011,
	FAST_COMMAND_ADC_SHUTDOWN_MODE = 0b1100,
	FAST_COMMAND_FULL_SHUTDOWN_MODE = 0b1101,
	FAST_COMMAND_FULL_RESET = 0b1110,
} fast_command_t;

typedef enum : uint8_t {
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

typedef struct __attribute__((packed)) {
	command_type_t type : 2;
	uint8_t reg_addr_or_fast_cmd : 4;
	uint8_t device_addr : 2;
} command_byte_t;
_Static_assert(sizeof(command_byte_t) == 1, "sizeof(command_byte_t)");

esp_err_t mcp3461_create(mcp3461_device_t *h, spi_host_device_t host_id,
						 int cs_io_num, uint8_t device_address) {
	esp_err_t ret = ESP_OK;

	spi_device_interface_config_t dev_config = {
		.command_bits = 8,
		.address_bits = 0,
		.dummy_bits = 0,
		.mode = 0, // (CPOL, CPHA) = (0, 0)
		.clock_source = SPI_CLK_SRC_DEFAULT,
		.clock_speed_hz = SPI_CLOCK_RATE, // FIXME(etw): configurable?
		.spics_io_num = cs_io_num,
		.flags = 0,
		.queue_size = 1,
	};
	if ((ret = spi_bus_add_device(host_id, &dev_config, &h->spidev)) !=
		ESP_OK) {
		goto done;
	}

	h->device_address = device_address;

	done:
	return ret;
}

esp_err_t mcp3461_dispose(mcp3461_device_t *h) {
	esp_err_t ret = ESP_OK;

	if ((ret = spi_bus_remove_device(h->spidev)) != ESP_OK) {
		goto done;
	}

	h->spidev = NULL;

	done:
	return ret;
}

static inline command_byte_t empty_command_byte(const mcp3461_device_t *h) {
	command_byte_t ret = {
			.device_addr = h->device_address,
	};
	return ret;
}

static esp_err_t
_reg_cmd_int(const mcp3461_device_t *h, command_type_t cmd_type, uint8_t reg_addr, uint32_t tx_data, uint32_t *rx_data,
			 unsigned bit_len, mcp3461_status *status) {
	esp_err_t ret = ESP_OK;

	if (bit_len > sizeof_member(spi_transaction_t, rx_buffer) * 8) {
		ret = ESP_ERR_INVALID_SIZE;
		goto done;
	}

	struct __attribute__((packed)) {
		command_byte_t command;
		uint32_t data;
	} tx_buffer = {
			{
					.type = cmd_type,
					.reg_addr_or_fast_cmd = reg_addr,
					.device_addr = h->device_address
			},
			SPI_SWAP_DATA_TX(tx_data, bit_len)
	};

	struct __attribute__((packed)) {
		mcp3461_status status;
		uint32_t raw_data;
	} rx_buffer = {0};

	spi_transaction_ext_t transaction = {
			{
					.length = 8 + bit_len,
					.flags = SPI_TRANS_VARIABLE_CMD,
					.tx_buffer = &tx_buffer,
					.rx_buffer = &rx_buffer,
			},
			.command_bits = 0
	};

	if ((ret = spi_device_transmit(h->spidev, (spi_transaction_t *) &transaction)) != ESP_OK) {
		goto done;
	}

	if (status) {
		*status = rx_buffer.status;
	}

	if (rx_data) {
		*rx_data = SPI_SWAP_DATA_RX(rx_buffer.raw_data, bit_len);
	}

	done:
	return ret;
}

static esp_err_t
read_reg_int(const mcp3461_device_t *h, uint8_t reg_addr, uint32_t *data, unsigned bit_len, mcp3461_status *status) {
	return _reg_cmd_int(h, COMMAND_TYPE_STATIC_READ, reg_addr, 0, data, bit_len, status);
}

static esp_err_t write_reg_int(const mcp3461_device_t *h, uint8_t reg_addr,
							   const uint32_t data, unsigned bit_len, mcp3461_status *status) {
	return _reg_cmd_int(h, COMMAND_TYPE_INC_WRITE, reg_addr, data, NULL, bit_len, status);
}

static esp_err_t do_fast_cmd(const mcp3461_device_t *h,
							 fast_command_t command) {
	esp_err_t ret = ESP_OK;

	command_byte_t command_byte = empty_command_byte(h);
	command_byte.type = COMMAND_TYPE_FAST;
	command_byte.reg_addr_or_fast_cmd = command;
	spi_transaction_t transaction = {
		.length = 0,
		.tx_buffer = NULL,
		.rx_buffer = NULL,
	};
	__builtin_memcpy(&transaction.cmd, &command_byte, sizeof(command_byte));
	if ((ret = spi_device_transmit(h->spidev, &transaction)) != ESP_OK) {
		goto done;
	}

done:
	return ret;
}

// hidden for now since we can't handle the device in its default
// configuration
static esp_err_t mcp3461_reset(mcp3461_device_t *h) {
	return do_fast_cmd(h, FAST_COMMAND_FULL_RESET);
}

esp_err_t mcp3461_init(mcp3461_device_t *h) {
	esp_err_t ret = ESP_OK;

	if ((ret = mcp3461_reset(h)) != ESP_OK) {
		goto done;
	}

	if ((ret = write_reg_int(h, REG_CONFIG0,
							 adc_config0(MCP3461_ADC_MODE_STANDBY, MCP3461_ADC_BIAS_NONE, true,
										 MCP3461_CLK_SEL_INTERNAL, true), 8, NULL)) !=
		ESP_OK) {
		goto done;
	}

	if ((ret = write_reg_int(h, REG_CONFIG3,
							 adc_config3(MCP3461_CONV_CONTINUOUS, 0, 0), 8, NULL)) !=
		ESP_OK) {
		goto done;
	}

	if ((ret = write_reg_int(h, REG_IRQ, 7, 8, NULL)) != ESP_OK) {
		goto done;
	}

	done:
	return ret;
}

esp_err_t mcp3461_set_adc_mode(mcp3461_device_t *h, adc_mode mode) {
	fast_command_t fast_cmd;
	switch (mode) {
	case MCP3461_ADC_MODE_SHUTDOWN:
		fast_cmd = FAST_COMMAND_ADC_SHUTDOWN_MODE;
		break;
	case MCP3461_ADC_MODE_STANDBY:
		fast_cmd = FAST_COMMAND_ADC_STANDBY_MODE;
		break;
	case MCP3461_ADC_MODE_CONVERSION:
		fast_cmd = FAST_COMMAND_ADC_CONV_START;
		break;
	default:
		return ESP_ERR_INVALID_ARG;
	}

	return do_fast_cmd(h, fast_cmd);
}

esp_err_t mcp3461_set_scan(mcp3461_device_t *h, scan_chan channels) {
	return write_reg_int(h, REG_SCAN, channels, 24, NULL);
}

esp_err_t mcp3461_set_mux(mcp3461_device_t *h, mux_pin positive,
						  mux_pin negative) {
	uint8_t reg_value = BIT_FIELD8(positive, 4, 4) | BIT_FIELD8(negative, 4, 0);
	return write_reg_int(h, REG_MUX, reg_value, 8, NULL);
}

esp_err_t mcp3461_read(mcp3461_device_t *h, mcp3461_read_data_t *out, mcp3461_status *status) {
	esp_err_t ret = ESP_OK;

	// TODO: support other data formats (only 17 bit with channel ID currently
	// supported)

	uint32_t raw_data;
	if ((ret = read_reg_int(h, REG_ADCDATA, &raw_data, 32, status)) != ESP_OK) {
		goto done;
	}

	out->value = sign_extend32(raw_data, 17);
	out->channel_id = raw_data >> 28;

	done:
	return ret;
}
