//
// Created by alec on 6/23/24.
//

#include "wifi.h"
#include <string.h>
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/event_groups.h"
#include "esp_system.h"
#include "esp_wifi.h"
#include "esp_event.h"
#include "esp_log.h"
#include "nvs_flash.h"

#include "lwip/err.h"
#include "lwip/sys.h"

#define EXAMPLE_ESP_WIFI_SSID      CONFIG_ESP_WIFI_SSID
#define EXAMPLE_ESP_WIFI_PASS      CONFIG_ESP_WIFI_PASSWORD
#define EXAMPLE_ESP_MAXIMUM_RETRY  CONFIG_ESP_MAXIMUM_RETRY

#if CONFIG_ESP_WPA3_SAE_PWE_HUNT_AND_PECK
#define ESP_WIFI_SAE_MODE WPA3_SAE_PWE_HUNT_AND_PECK
#define EXAMPLE_H2E_IDENTIFIER ""
#elif CONFIG_ESP_WPA3_SAE_PWE_HASH_TO_ELEMENT
#define ESP_WIFI_SAE_MODE WPA3_SAE_PWE_HASH_TO_ELEMENT
#define EXAMPLE_H2E_IDENTIFIER CONFIG_ESP_WIFI_PW_ID
#elif CONFIG_ESP_WPA3_SAE_PWE_BOTH
#define ESP_WIFI_SAE_MODE WPA3_SAE_PWE_BOTH
#define EXAMPLE_H2E_IDENTIFIER CONFIG_ESP_WIFI_PW_ID
#endif
#if CONFIG_ESP_WIFI_AUTH_OPEN
#define ESP_WIFI_SCAN_AUTH_MODE_THRESHOLD WIFI_AUTH_OPEN
#elif CONFIG_ESP_WIFI_AUTH_WEP
#define ESP_WIFI_SCAN_AUTH_MODE_THRESHOLD WIFI_AUTH_WEP
#elif CONFIG_ESP_WIFI_AUTH_WPA_PSK
#define ESP_WIFI_SCAN_AUTH_MODE_THRESHOLD WIFI_AUTH_WPA_PSK
#elif CONFIG_ESP_WIFI_AUTH_WPA2_PSK
#define ESP_WIFI_SCAN_AUTH_MODE_THRESHOLD WIFI_AUTH_WPA2_PSK
#elif CONFIG_ESP_WIFI_AUTH_WPA_WPA2_PSK
#define ESP_WIFI_SCAN_AUTH_MODE_THRESHOLD WIFI_AUTH_WPA_WPA2_PSK
#elif CONFIG_ESP_WIFI_AUTH_WPA3_PSK
#define ESP_WIFI_SCAN_AUTH_MODE_THRESHOLD WIFI_AUTH_WPA3_PSK
#elif CONFIG_ESP_WIFI_AUTH_WPA2_WPA3_PSK
#define ESP_WIFI_SCAN_AUTH_MODE_THRESHOLD WIFI_AUTH_WPA2_WPA3_PSK
#elif CONFIG_ESP_WIFI_AUTH_WAPI_PSK
#define ESP_WIFI_SCAN_AUTH_MODE_THRESHOLD WIFI_AUTH_WAPI_PSK
#endif

/* FreeRTOS event group to signal when we are connected*/
static EventGroupHandle_t s_wifi_event_group;

/* The event group allows multiple bits for each event, but we only care about two events:
 * - we are connected to the AP with an IP
 * - we failed to connect after the maximum amount of retries */
#define WIFI_CONNECTED_BIT BIT0
#define WIFI_FAIL_BIT      BIT1

static const char *TAG = "wifi station";

static int s_retry_num = 0;


static void event_handler(void* arg, esp_event_base_t event_base,
						  int32_t event_id, void* event_data)
{
	if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_START) {
		esp_wifi_connect();
	} else if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_DISCONNECTED) {
		if (s_retry_num < EXAMPLE_ESP_MAXIMUM_RETRY) {
			esp_wifi_connect();
			s_retry_num++;
			ESP_LOGI(TAG, "retry to connect to the AP");
		} else {
			xEventGroupSetBits(s_wifi_event_group, WIFI_FAIL_BIT);
		}
		ESP_LOGI(TAG,"connect to the AP fail");
	} else if (event_base == IP_EVENT && event_id == IP_EVENT_STA_GOT_IP) {
		ip_event_got_ip_t* event = (ip_event_got_ip_t*) event_data;
		ESP_LOGI(TAG, "got ip:" IPSTR, IP2STR(&event->ip_info.ip));
		s_retry_num = 0;
		xEventGroupSetBits(s_wifi_event_group, WIFI_CONNECTED_BIT);
	}
}

void example_connect(void)
{
	s_wifi_event_group = xEventGroupCreate();

	esp_netif_create_default_wifi_sta();

	wifi_init_config_t cfg = WIFI_INIT_CONFIG_DEFAULT();
	ESP_ERROR_CHECK(esp_wifi_init(&cfg));

	esp_event_handler_instance_t instance_any_id;
	esp_event_handler_instance_t instance_got_ip;
	ESP_ERROR_CHECK(esp_event_handler_instance_register(WIFI_EVENT,
														ESP_EVENT_ANY_ID,
														&event_handler,
														NULL,
														&instance_any_id));
	ESP_ERROR_CHECK(esp_event_handler_instance_register(IP_EVENT,
														IP_EVENT_STA_GOT_IP,
														&event_handler,
														NULL,
														&instance_got_ip));

	wifi_config_t wifi_config = {
			.sta = {
					.ssid = EXAMPLE_ESP_WIFI_SSID,
					.password = EXAMPLE_ESP_WIFI_PASS,
					/* Authmode threshold resets to WPA2 as default if password matches WPA2 standards (pasword len => 8).
					 * If you want to connect the device to deprecated WEP/WPA networks, Please set the threshold value
					 * to WIFI_AUTH_WEP/WIFI_AUTH_WPA_PSK and set the password with length and format matching to
					 * WIFI_AUTH_WEP/WIFI_AUTH_WPA_PSK standards.
					 */
					.threshold.authmode = ESP_WIFI_SCAN_AUTH_MODE_THRESHOLD,
					.sae_pwe_h2e = ESP_WIFI_SAE_MODE,
					.sae_h2e_identifier = EXAMPLE_H2E_IDENTIFIER,
			},
	};
	ESP_ERROR_CHECK(esp_wifi_set_mode(WIFI_MODE_STA) );
	ESP_ERROR_CHECK(esp_wifi_set_config(WIFI_IF_STA, &wifi_config) );
	ESP_ERROR_CHECK(esp_wifi_start() );

	ESP_LOGI(TAG, "wifi_init_sta finished.");

	/* Waiting until either the connection is established (WIFI_CONNECTED_BIT) or connection failed for the maximum
	 * number of re-tries (WIFI_FAIL_BIT). The bits are set by event_handler() (see above) */
	EventBits_t bits = xEventGroupWaitBits(s_wifi_event_group,
										   WIFI_CONNECTED_BIT | WIFI_FAIL_BIT,
										   pdFALSE,
										   pdFALSE,
										   portMAX_DELAY);

	/* xEventGroupWaitBits() returns the bits before the call returned, hence we can test which event actually
	 * happened. */
	if (bits & WIFI_CONNECTED_BIT) {
		ESP_LOGI(TAG, "connected to ap SSID:%s password:%s",
				 EXAMPLE_ESP_WIFI_SSID, EXAMPLE_ESP_WIFI_PASS);
	} else if (bits & WIFI_FAIL_BIT) {
		ESP_LOGI(TAG, "Failed to connect to SSID:%s, password:%s",
				 EXAMPLE_ESP_WIFI_SSID, EXAMPLE_ESP_WIFI_PASS);
	} else {
		ESP_LOGE(TAG, "UNEXPECTED EVENT");
	}
}


/* Type of Escape algorithms to be used */
#define NGX_ESCAPE_URI            (0)
#define NGX_ESCAPE_ARGS           (1)
#define NGX_ESCAPE_URI_COMPONENT  (2)
#define NGX_ESCAPE_HTML           (3)
#define NGX_ESCAPE_REFRESH        (4)
#define NGX_ESCAPE_MEMCACHED      (5)
#define NGX_ESCAPE_MAIL_AUTH      (6)

/* Type of Unescape algorithms to be used */
#define NGX_UNESCAPE_URI          (1)
#define NGX_UNESCAPE_REDIRECT     (2)


uintptr_t ngx_escape_uri(u_char *dst, u_char *src, size_t size, unsigned int type)
{
	unsigned int      n;
	uint32_t       *escape;
	static u_char   hex[] = "0123456789ABCDEF";

	/*
	 * Per RFC 3986 only the following chars are allowed in URIs unescaped:
	 *
	 * unreserved    = ALPHA / DIGIT / "-" / "." / "_" / "~"
	 * gen-delims    = ":" / "/" / "?" / "#" / "[" / "]" / "@"
	 * sub-delims    = "!" / "$" / "&" / "'" / "(" / ")"
	 *               / "*" / "+" / "," / ";" / "="
	 *
	 * And "%" can appear as a part of escaping itself.  The following
	 * characters are not allowed and need to be escaped: %00-%1F, %7F-%FF,
	 * " ", """, "<", ">", "\", "^", "`", "{", "|", "}".
	 */

	/* " ", "#", "%", "?", not allowed */

	static uint32_t   uri[] = {
			0xffffffff, /* 1111 1111 1111 1111  1111 1111 1111 1111 */

			/* ?>=< ;:98 7654 3210  /.-, +*)( '&%$ #"!  */
			0xd000002d, /* 1101 0000 0000 0000  0000 0000 0010 1101 */

			/* _^]\ [ZYX WVUT SRQP  ONML KJIH GFED CBA@ */
			0x50000000, /* 0101 0000 0000 0000  0000 0000 0000 0000 */

			/*  ~}| {zyx wvut srqp  onml kjih gfed cba` */
			0xb8000001, /* 1011 1000 0000 0000  0000 0000 0000 0001 */

			0xffffffff, /* 1111 1111 1111 1111  1111 1111 1111 1111 */
			0xffffffff, /* 1111 1111 1111 1111  1111 1111 1111 1111 */
			0xffffffff, /* 1111 1111 1111 1111  1111 1111 1111 1111 */
			0xffffffff  /* 1111 1111 1111 1111  1111 1111 1111 1111 */
	};

	/* " ", "#", "%", "&", "+", ";", "?", not allowed */

	static uint32_t   args[] = {
			0xffffffff, /* 1111 1111 1111 1111  1111 1111 1111 1111 */

			/* ?>=< ;:98 7654 3210  /.-, +*)( '&%$ #"!  */
			0xd800086d, /* 1101 1000 0000 0000  0000 1000 0110 1101 */

			/* _^]\ [ZYX WVUT SRQP  ONML KJIH GFED CBA@ */
			0x50000000, /* 0101 0000 0000 0000  0000 0000 0000 0000 */

			/*  ~}| {zyx wvut srqp  onml kjih gfed cba` */
			0xb8000001, /* 1011 1000 0000 0000  0000 0000 0000 0001 */

			0xffffffff, /* 1111 1111 1111 1111  1111 1111 1111 1111 */
			0xffffffff, /* 1111 1111 1111 1111  1111 1111 1111 1111 */
			0xffffffff, /* 1111 1111 1111 1111  1111 1111 1111 1111 */
			0xffffffff  /* 1111 1111 1111 1111  1111 1111 1111 1111 */
	};

	/* not ALPHA, DIGIT, "-", ".", "_", "~" */

	static uint32_t   uri_component[] = {
			0xffffffff, /* 1111 1111 1111 1111  1111 1111 1111 1111 */

			/* ?>=< ;:98 7654 3210  /.-, +*)( '&%$ #"!  */
			0xfc009fff, /* 1111 1100 0000 0000  1001 1111 1111 1111 */

			/* _^]\ [ZYX WVUT SRQP  ONML KJIH GFED CBA@ */
			0x78000001, /* 0111 1000 0000 0000  0000 0000 0000 0001 */

			/*  ~}| {zyx wvut srqp  onml kjih gfed cba` */
			0xb8000001, /* 1011 1000 0000 0000  0000 0000 0000 0001 */

			0xffffffff, /* 1111 1111 1111 1111  1111 1111 1111 1111 */
			0xffffffff, /* 1111 1111 1111 1111  1111 1111 1111 1111 */
			0xffffffff, /* 1111 1111 1111 1111  1111 1111 1111 1111 */
			0xffffffff  /* 1111 1111 1111 1111  1111 1111 1111 1111 */
	};

	/* " ", "#", """, "%", "'", not allowed */

	static uint32_t   html[] = {
			0xffffffff, /* 1111 1111 1111 1111  1111 1111 1111 1111 */

			/* ?>=< ;:98 7654 3210  /.-, +*)( '&%$ #"!  */
			0x500000ad, /* 0101 0000 0000 0000  0000 0000 1010 1101 */

			/* _^]\ [ZYX WVUT SRQP  ONML KJIH GFED CBA@ */
			0x50000000, /* 0101 0000 0000 0000  0000 0000 0000 0000 */

			/*  ~}| {zyx wvut srqp  onml kjih gfed cba` */
			0xb8000001, /* 1011 1000 0000 0000  0000 0000 0000 0001 */

			0xffffffff, /* 1111 1111 1111 1111  1111 1111 1111 1111 */
			0xffffffff, /* 1111 1111 1111 1111  1111 1111 1111 1111 */
			0xffffffff, /* 1111 1111 1111 1111  1111 1111 1111 1111 */
			0xffffffff  /* 1111 1111 1111 1111  1111 1111 1111 1111 */
	};

	/* " ", """, "'", not allowed */

	static uint32_t   refresh[] = {
			0xffffffff, /* 1111 1111 1111 1111  1111 1111 1111 1111 */

			/* ?>=< ;:98 7654 3210  /.-, +*)( '&%$ #"!  */
			0x50000085, /* 0101 0000 0000 0000  0000 0000 1000 0101 */

			/* _^]\ [ZYX WVUT SRQP  ONML KJIH GFED CBA@ */
			0x50000000, /* 0101 0000 0000 0000  0000 0000 0000 0000 */

			/*  ~}| {zyx wvut srqp  onml kjih gfed cba` */
			0xd8000001, /* 1011 1000 0000 0000  0000 0000 0000 0001 */

			0xffffffff, /* 1111 1111 1111 1111  1111 1111 1111 1111 */
			0xffffffff, /* 1111 1111 1111 1111  1111 1111 1111 1111 */
			0xffffffff, /* 1111 1111 1111 1111  1111 1111 1111 1111 */
			0xffffffff  /* 1111 1111 1111 1111  1111 1111 1111 1111 */
	};

	/* " ", "%", %00-%1F */

	static uint32_t   memcached[] = {
			0xffffffff, /* 1111 1111 1111 1111  1111 1111 1111 1111 */

			/* ?>=< ;:98 7654 3210  /.-, +*)( '&%$ #"!  */
			0x00000021, /* 0000 0000 0000 0000  0000 0000 0010 0001 */

			/* _^]\ [ZYX WVUT SRQP  ONML KJIH GFED CBA@ */
			0x00000000, /* 0000 0000 0000 0000  0000 0000 0000 0000 */

			/*  ~}| {zyx wvut srqp  onml kjih gfed cba` */
			0x00000000, /* 0000 0000 0000 0000  0000 0000 0000 0000 */

			0x00000000, /* 0000 0000 0000 0000  0000 0000 0000 0000 */
			0x00000000, /* 0000 0000 0000 0000  0000 0000 0000 0000 */
			0x00000000, /* 0000 0000 0000 0000  0000 0000 0000 0000 */
			0x00000000, /* 0000 0000 0000 0000  0000 0000 0000 0000 */
	};

	/* mail_auth is the same as memcached */

	static uint32_t  *map[] =
			{ uri, args, uri_component, html, refresh, memcached, memcached };


	escape = map[type];

	if (dst == NULL) {

		/* find the number of the characters to be escaped */

		n = 0;

		while (size) {
			if (escape[*src >> 5] & (1U << (*src & 0x1f))) {
				n++;
			}
			src++;
			size--;
		}

		return (uintptr_t) n;
	}

	while (size) {
		if (escape[*src >> 5] & (1U << (*src & 0x1f))) {
			*dst++ = '%';
			*dst++ = hex[*src >> 4];
			*dst++ = hex[*src & 0xf];
			src++;

		} else {
			*dst++ = *src++;
		}
		size--;
	}

	return (uintptr_t) dst;
}


void ngx_unescape_uri(u_char **dst, u_char **src, size_t size, unsigned int type)
{
	u_char  *d, *s, ch, c, decoded;
	enum {
		sw_usual = 0,
		sw_quoted,
		sw_quoted_second
	} state;

	d = *dst;
	s = *src;

	state = 0;
	decoded = 0;

	while (size--) {

		ch = *s++;

		switch (state) {
			case sw_usual:
				if (ch == '?'
					&& (type & (NGX_UNESCAPE_URI | NGX_UNESCAPE_REDIRECT))) {
					*d++ = ch;
					goto done;
				}

				if (ch == '%') {
					state = sw_quoted;
					break;
				}

				*d++ = ch;
				break;

			case sw_quoted:

				if (ch >= '0' && ch <= '9') {
					decoded = (u_char) (ch - '0');
					state = sw_quoted_second;
					break;
				}

				c = (u_char) (ch | 0x20);
				if (c >= 'a' && c <= 'f') {
					decoded = (u_char) (c - 'a' + 10);
					state = sw_quoted_second;
					break;
				}

				/* the invalid quoted character */

				state = sw_usual;

				*d++ = ch;

				break;

			case sw_quoted_second:

				state = sw_usual;

				if (ch >= '0' && ch <= '9') {
					ch = (u_char) ((decoded << 4) + (ch - '0'));

					if (type & NGX_UNESCAPE_REDIRECT) {
						if (ch > '%' && ch < 0x7f) {
							*d++ = ch;
							break;
						}

						*d++ = '%'; *d++ = *(s - 2); *d++ = *(s - 1);

						break;
					}

					*d++ = ch;

					break;
				}

				c = (u_char) (ch | 0x20);
				if (c >= 'a' && c <= 'f') {
					ch = (u_char) ((decoded << 4) + (c - 'a') + 10);

					if (type & NGX_UNESCAPE_URI) {
						if (ch == '?') {
							*d++ = ch;
							goto done;
						}

						*d++ = ch;
						break;
					}

					if (type & NGX_UNESCAPE_REDIRECT) {
						if (ch == '?') {
							*d++ = ch;
							goto done;
						}

						if (ch > '%' && ch < 0x7f) {
							*d++ = ch;
							break;
						}

						*d++ = '%'; *d++ = *(s - 2); *d++ = *(s - 1);
						break;
					}

					*d++ = ch;

					break;
				}

				/* the invalid quoted character */

				break;
		}
	}

	done:

	*dst = d;
	*src = s;
}


uint32_t example_uri_encode(char *dest, const char *src, size_t len)
{
	if (!src || !dest) {
		return 0;
	}

	uintptr_t ret = ngx_escape_uri((unsigned char *)dest, (unsigned char *)src, len, NGX_ESCAPE_URI_COMPONENT);
	return (uint32_t)(ret - (uintptr_t)dest);
}


void example_uri_decode(char *dest, const char *src, size_t len)
{
	if (!src || !dest) {
		return;
	}

	unsigned char *src_ptr = (unsigned char *)src;
	unsigned char *dst_ptr = (unsigned char *)dest;
	ngx_unescape_uri(&dst_ptr, &src_ptr, len, NGX_UNESCAPE_URI);
}