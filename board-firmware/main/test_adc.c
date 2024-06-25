#include <soc/gpio_num.h>
#include <driver/gpio.h>
#include "MCP3461.h"

void app_main(void);

void app_main(void) {
	gpio_set_direction(GPIO_NUM_38, GPIO_MODE_OUTPUT);
	gpio_set_direction(GPIO_NUM_36, GPIO_MODE_OUTPUT);
	gpio_set_direction(GPIO_NUM_35, GPIO_MODE_OUTPUT);
	gpio_set_direction(GPIO_NUM_34, GPIO_MODE_OUTPUT);
	gpio_set_level(GPIO_NUM_38, 1);
	gpio_set_level(GPIO_NUM_36, 1);
	gpio_set_level(GPIO_NUM_35, 1);
	gpio_set_level(GPIO_NUM_34, 1);


	spi_bus_config_t bus_cfg = {
			.mosi_io_num = GPIO_NUM_20,
			.miso_io_num = GPIO_NUM_19,
			.sclk_io_num = GPIO_NUM_21,

			.quadwp_io_num = -1,
			.quadhd_io_num = -1,
			.data4_io_num = -1,
			.data5_io_num = -1,
			.data6_io_num = -1,
			.data7_io_num = -1,

			.flags = SPICOMMON_BUSFLAG_GPIO_PINS | SPICOMMON_BUSFLAG_MASTER
	};
	ESP_ERROR_CHECK(spi_bus_initialize(SPI3_HOST, &bus_cfg, SPI_DMA_DISABLED));

	mcp3461_device_t h;
	ESP_ERROR_CHECK(mcp3461_create(&h, SPI3_HOST, GPIO_NUM_18, 1));

	ESP_ERROR_CHECK(mcp3461_init(&h));
	ESP_ERROR_CHECK(mcp3461_set_adc_mode(&h, MCP3461_ADC_MODE_CONVERSION));
	ESP_ERROR_CHECK(mcp3461_set_scan(&h,MCP3461_CHAN_AVDD));

	mcp3461_read_data_t data;
	int32_t channels[8] = { 0 };

	for (int i = 0; i < 8; i++) {
		mcp3461_status status;

		ESP_ERROR_CHECK(mcp3461_read(&h, &data, &status));
		channels[data.channel_id] = data.value;

		printf("Channel %d: %d.", data.channel_id, data.value);
		printf("  DR: %d, POR: %d, addr: %02x, reserved 0: %d\n", !status.dr_status_n, !status.por_status_n, status.dev_addr, status.reserved_0);
		vTaskDelay(100 / portTICK_PERIOD_MS);
	}
}