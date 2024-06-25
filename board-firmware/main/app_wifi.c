#include <esp_wifi.h>
#include <esp_event.h>
#include <esp_log.h>
#include <esp_system.h>
#include <nvs_flash.h>
#include <sys/param.h>
#include "esp_netif.h"
#include "esp_eth.h"
#include <esp_tls.h>

#include <esp_https_server.h>
#include <soc/gpio_num.h>
#include "esp_tls.h"
#include "sdkconfig.h"
#include "wifi.h"
#include "management.h"
#include "measurements.h"
#include "io.h"

/* A simple example that demonstrates how to create GET and POST
 * handlers and start an HTTPS server.
*/

extern const unsigned char trusted_start[] asm("_binary_operator_crt_start");
extern const unsigned char trusted_end[]   asm("_binary_operator_crt_end");

extern const unsigned char servercert_start[] asm("_binary_tower_crt_start");
extern const unsigned char servercert_end[]   asm("_binary_tower_crt_end");

extern const unsigned char prvtkey_pem_start[] asm("_binary_tower_pem_start");
extern const unsigned char prvtkey_pem_end[]   asm("_binary_tower_pem_end");

static const char *TAG = "main";

static void event_handler(void *arg, esp_event_base_t event_base,
						  int32_t event_id, void *event_data) {
	if (event_base == ESP_HTTPS_SERVER_EVENT) {
		if (event_id == HTTPS_SERVER_EVENT_ERROR) {
			esp_https_server_last_error_t *last_error = (esp_tls_last_error_t *) event_data;
			ESP_LOGE(TAG, "Error event triggered: last_error = %s, last_tls_err = %d, tls_flag = %d",
					 esp_err_to_name(last_error->last_error), last_error->esp_tls_error_code,
					 last_error->esp_tls_flags);
		}
	}
}

static esp_err_t root_get_handler(httpd_req_t *req) {
	httpd_resp_set_type(req, "text/html");
	httpd_resp_send(req, "<h1>Hello Secure World!</h1>", HTTPD_RESP_USE_STRLEN);

	return ESP_OK;
}

static esp_err_t post_handler(httpd_req_t *req) {
	char *body = NULL;

	char content_length_str[6];
	if (httpd_req_get_hdr_value_str(req, "Content-Length", content_length_str, 6) != ESP_OK) {
		httpd_resp_send_err(req, HTTPD_400_BAD_REQUEST, "Content-Length needed");
		goto done;
	}

	int content_length = strtol(content_length_str, NULL, 10);
	if (content_length < 0) {
		httpd_resp_send_err(req, HTTPD_400_BAD_REQUEST, "Invalid Content-Length");
		goto done;
	}

	body = malloc(content_length + 1);
	if (body == NULL) {
		httpd_resp_send_err(req, HTTPD_500_INTERNAL_SERVER_ERROR, "Failed to allocate memory");
		goto done;
	}

	if (httpd_req_recv(req, body, content_length) != content_length) {
		httpd_resp_send_err(req, HTTPD_500_INTERNAL_SERVER_ERROR, "Failed to receive body");
		goto done;
	}
	body[content_length] = '\0';

	char side[2] = {0};
	if (httpd_query_key_value(body, "side", side, 2) != ESP_OK || (side[0] != '1' && side[0] != '2')) {
		httpd_resp_send_err(req, HTTPD_400_BAD_REQUEST, "Invalid query: side");
		goto done;
	}

	char target[7] = {0};
	if (httpd_query_key_value(body, "target", target, 7) != ESP_OK ||
		(strcmp(target, "source") != 0 && strcmp(target, "load") != 0)) {
		httpd_resp_send_err(req, HTTPD_400_BAD_REQUEST, "Invalid query: target");
		goto done;
	}

	char action[20] = {0};
	if (httpd_query_key_value(body, "action", action, 20) != ESP_OK) {
		httpd_resp_send_err(req, HTTPD_400_BAD_REQUEST, "Invalid query: action");
		goto done;
	}

	bool is_side_2 = side[0] == '2';
	bool is_load = strcmp(target, "load") == 0;

	esp_err_t err;

	if (!strcmp(action, "trip")) {
		err = trip(TRIP_REASON_MANUAL, is_side_2, is_load);
	} else if (!strcmp(action, "untrip")) {
		err = untrip(is_side_2, is_load);
	} else if (!strcmp(action, "engage")) {
		err = set_engaged(is_side_2, is_load, true);
	} else if (!strcmp(action, "disengage")) {
		err = set_engaged(is_side_2, is_load, false);
	} else {
		ESP_LOGE(TAG, "Invalid action: %s", action);
		httpd_resp_send_err(req, HTTPD_400_BAD_REQUEST, "Invalid query: action 2");
		goto done;
	}

	if (err != ESP_OK) {
		httpd_resp_send_err(req, HTTPD_500_INTERNAL_SERVER_ERROR, "Failed to complete action");
		goto done;
	}

	httpd_resp_sendstr(req, "Success");

	done:
	free(body);
	return ESP_OK;
}

static int https_server_cert_select_callback(mbedtls_ssl_context *ctx) {
	mbedtls_ssl_set_hs_authmode(ctx, MBEDTLS_SSL_VERIFY_OPTIONAL);

	static mbedtls_x509_crt chain;
	static bool chain_init = false;
	if (!chain_init) {
		mbedtls_x509_crt_init(&chain);
		mbedtls_x509_crt_parse(&chain, trusted_start, trusted_end - trusted_start);
		chain_init = true;
	}

	mbedtls_ssl_set_hs_ca_chain(ctx, &chain, NULL);

	return 0;
}

static httpd_uri_t const root = {
		.uri       = "/",
		.method    = HTTP_GET,
		.handler   = root_get_handler
};

static httpd_uri_t const action = {
		.uri       = "/",
		.method    = HTTP_POST,
		.handler   = post_handler
};

static httpd_handle_t start_webserver(void) {
	httpd_handle_t server = NULL;

	ESP_LOGI(TAG, "Starting server");

	httpd_ssl_config_t conf = HTTPD_SSL_CONFIG_DEFAULT();

	conf.servercert = servercert_start;
	conf.servercert_len = servercert_end - servercert_start;

	conf.prvtkey_pem = prvtkey_pem_start;
	conf.prvtkey_len = prvtkey_pem_end - prvtkey_pem_start;

	conf.cert_select_cb = https_server_cert_select_callback;
	esp_err_t ret = httpd_ssl_start(&server, &conf);
	if (ESP_OK != ret) {
		ESP_LOGI(TAG, "Error starting server!");
		return NULL;
	}

	ESP_LOGI(TAG, "Registering URI handlers");

	httpd_register_uri_handler(server, &root);
	httpd_register_uri_handler(server, &action);

	return server;
}

static esp_err_t stop_webserver(httpd_handle_t server) {
	return httpd_ssl_stop(server);
}

static void disconnect_handler(void *arg, esp_event_base_t event_base,
							   int32_t event_id, void *event_data) {
	httpd_handle_t *server = (httpd_handle_t *) arg;
	if (*server) {
		if (stop_webserver(*server) == ESP_OK) {
			*server = NULL;
		} else {
			ESP_LOGE(TAG, "Failed to stop https server");
		}
	}
}

static void connect_handler(void *arg, esp_event_base_t event_base,
							int32_t event_id, void *event_data) {
	httpd_handle_t *server = (httpd_handle_t *) arg;
	if (*server == NULL) {
		*server = start_webserver();
	}
}

void app_main(void);

void app_main(void) {
	device_init();

	static httpd_handle_t server = NULL;

	ESP_ERROR_CHECK(nvs_flash_init());
	ESP_ERROR_CHECK(esp_netif_init());
	ESP_ERROR_CHECK(esp_event_loop_create_default());

	ESP_ERROR_CHECK(esp_event_handler_register(IP_EVENT, IP_EVENT_STA_GOT_IP, &connect_handler, &server));
	ESP_ERROR_CHECK(esp_event_handler_register(WIFI_EVENT, WIFI_EVENT_STA_DISCONNECTED, &disconnect_handler, &server));
	ESP_ERROR_CHECK(esp_event_handler_register(ESP_HTTPS_SERVER_EVENT, ESP_EVENT_ANY_ID, &event_handler, NULL));
	example_connect();

	xTaskCreate(
			(void (*)(void *)) measurements_loop,
			"measurements",
			4096,
			NULL,
			0,
			NULL
	);
}