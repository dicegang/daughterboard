//
// Created by alec on 6/2/24.
//

#include <nvs.h>
#include <esp_log.h>
#include <string.h>
#include "node_config.h"

#define TAG "device.c"

static bool initialised = false;

static esp_err_t _write_spi_config(struct device_configuration *config) {
	nvs_handle_t handle;

	esp_err_t error = nvs_open("storage", NVS_READWRITE, &handle);
	if (error != ESP_OK) {
		perror("nvs_open");
		return error;
	}

	error = nvs_set_blob(handle, "device_configuration", config, sizeof(*config));
	if (error != ESP_OK) {
		perror("nvs_set_blob");
		goto done;
	}

	error = nvs_commit(handle);
	if (error != ESP_OK) {
		perror("nvs_commit");
		goto done;
	}

	done:
	nvs_close(handle);
	return error;
}

static esp_err_t _load_spi_config(struct device_configuration *config) {
	nvs_handle_t handle;

	esp_err_t error = nvs_open("storage", NVS_READWRITE, &handle);
	if (error != ESP_OK) {
		perror("nvs_open");
		return error;
	}

	size_t length;
	error = nvs_get_blob(handle, "device_configuration", config, &length);
	if (error != ESP_OK) {
		perror("nvs_get_blob");
	} else if (length != sizeof(*config)) {
		ESP_LOGE(TAG, "invalid size of spi bus config in nvs");
		error = ESP_ERR_INVALID_SIZE;
	}

	nvs_close(handle);
	return error;
}

static void init_spi_config(void) {
	struct device_configuration config;
	esp_err_t err = _load_spi_config(&config);
	switch (err) {
		case ESP_OK:
			return;
		case ESP_ERR_NOT_FOUND:
			memset(&config, '\x0', sizeof(config));
			ESP_ERROR_CHECK(_write_spi_config(&config));
			ESP_ERROR_CHECK(_load_spi_config(&config));
			break;
		default:
			ESP_ERROR_CHECK(_load_spi_config(&config));
	}
}


esp_err_t write_device_config(struct device_configuration *config) {
	if (!initialised) {
		init_spi_config();
		initialised = true;
	}

	return _write_spi_config(config);
}

esp_err_t load_device_config(struct device_configuration *config) {
	if (!initialised) {
		init_spi_config();
		initialised = true;
	}

	return _load_spi_config(config);
}

esp_err_t load_node_hw(uint8_t id, struct node_spi_config *cfg) {
	struct device_configuration config;
	esp_err_t error = load_device_config(&config);
	if (error != ESP_OK) {
		return error;
	}

	if (id >= (sizeof(config.nodes) / sizeof(config.nodes[0]))) {
		return ESP_ERR_INVALID_ARG;
	}

	if (!config.nodes[id].connected)
		return ESP_ERR_INVALID_STATE;

	if (cfg)
		*cfg = config.nodes[id].spi_config;
	return ESP_OK;
}

esp_err_t load_node_info(uint8_t id, struct node_info *info) {
	struct device_configuration config;
	esp_err_t error = load_device_config(&config);
	if (error != ESP_OK) {
		return error;
	}

	if (id >= (sizeof(config.nodes) / sizeof(config.nodes[0]))) {
		return ESP_ERR_INVALID_ARG;
	}

	if (!config.nodes[id].connected)
		return ESP_ERR_INVALID_STATE;

	if (info)
		*info = config.nodes[id].info;
	return ESP_OK;
}