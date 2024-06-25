//
// Created by alec on 6/24/24.
//

#include <esp_log.h>
#include <delay.h>
#include "measurements.h"
#include "io.h"

static mcp3461_device_t adc;

#define TAG "measurements"

static int32_t channel_offsets[8] = {0};

static esp_err_t read_all_channels(int32_t data[8]) {
	esp_err_t result = ESP_OK;

	mcp3461_read_data_t read_data;
	mcp3461_status status;

	int prev_channel = 0;
	for (int i = 0; i < 8;) {
		if ((result = mcp3461_read(&adc, &read_data, &status)) != ESP_OK) {
			goto done;
		}

		if ((i == 0 || (read_data.channel_id != prev_channel)) && !status.dr_status_n) {
			data[read_data.channel_id] = read_data.value;
			prev_channel = read_data.channel_id;
			i++;
		}
	}

	done:
	return result;
}

static void convert_raw_data(int32_t const raw_data[8], struct adc_output *output) {
#define RAW_V(channel) ((raw_data[(channel)] - 32768./2 + channel_offsets[channel]) * 1.65 / 32768.)
#define V(channel) (RAW_V(channel) / 0.02)
#define I(channel) (RAW_V(channel))

	*output = (struct adc_output) {
		.grid_voltage = V(CH_V_GRID),
		.source_voltage = {V(CH_V_SOURCE_1), V(CH_V_SOURCE_2)},
		.source_current = {I(CH_I_SOURCE_1), I(CH_I_SOURCE_2)},
		.load_current = {I(CH_I_LOAD_1), I(CH_I_LOAD_2)},
	};

#undef RAW_V
#undef V
#undef I
}


static void measurements_init(void) {
	ESP_ERROR_CHECK(mcp3461_create(&adc, ADC_SPI, ADC_CS, 1));
	ESP_ERROR_CHECK(mcp3461_init(&adc));
	ESP_ERROR_CHECK(mcp3461_set_scan(&adc, 0b11111111));
	ESP_ERROR_CHECK(mcp3461_set_adc_mode(&adc, MCP3461_ADC_MODE_CONVERSION));

	DELAY_MS(10);

	int32_t calibration_data[8];
	ESP_ERROR_CHECK(read_all_channels(calibration_data));
	channel_offsets[CH_I_SOURCE_1] =  32768/2- calibration_data[CH_I_SOURCE_1];
	channel_offsets[CH_I_SOURCE_2] =32768/2 -calibration_data[CH_I_SOURCE_2];
	channel_offsets[CH_I_LOAD_1] = 32768/2-calibration_data[CH_I_LOAD_1];
	channel_offsets[CH_I_LOAD_2] = 32768/2-calibration_data[CH_I_LOAD_2];

	ESP_LOGI(TAG, "Calibration data:  %ld, %ld, %ld, %ld", calibration_data[CH_I_SOURCE_1], calibration_data[CH_I_SOURCE_2], calibration_data[CH_I_LOAD_1], calibration_data[CH_I_LOAD_2]);

	ESP_LOGI(TAG, "ADC initialized");
}

void measurements_loop(void) {
	ESP_LOGI(TAG, "starting measurements thread");

	int32_t raw_data[8];
	mcp3461_status status;

	measurements_init();
	for (;;) {
		esp_err_t err = read_all_channels(raw_data);
		if (err != ESP_OK) {
			ESP_LOGE(TAG, "error reading ADC: %s", esp_err_to_name(err));
		}

		DELAY_MS(2);

		struct adc_output output;
		convert_raw_data(raw_data, &output);
		ESP_LOGI(TAG, "grid voltage: %.3f", output.grid_voltage);
		ESP_LOGI(TAG, "source voltages: %.3f, %.3f", output.source_voltage[0], output.source_voltage[1]);
		ESP_LOGI(TAG, "source currents: %.3f, %.3f", output.source_current[0], output.source_current[1]);
		ESP_LOGI(TAG, "load currents: %.3f, %.3f", output.load_current[0], output.load_current[1]);
	}
}