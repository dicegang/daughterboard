//
// Created by alec on 5/30/24.
//

#include "avrisp.h"

void app_main(void) {
	struct bitbang_spi_config config = (struct bitbang_spi_config) {
		.rst = 1, .clk = 14,.mosi = 13, .miso = 12, 1000000 / 6
	};
	program(&config, );
}