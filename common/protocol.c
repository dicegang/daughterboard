#include <stddef.h>
#include <stdio.h>
#include "protocol.h"

char const* message_type_to_string(enum message_type type) {
#define CASE(x) case x: return #x
	switch (type) {
		CASE(FLASH_BEGIN);
		CASE(FLASH_DATA);
		CASE(FLASH_DATA_END);
		CASE(CONFIGURE_SHUTDOWN);
		CASE(CONFIGURE_ENGAGEMENT);
		CASE(SCAN);
		CASE(NODE_STATE);
		CASE(SET_NODE_INFO);
		default:
			return "UNKNOWN";
	}
#undef CASE
}

char* response_msg_to_string(struct response_msg *msg) {
	// use snprintf
	int size = snprintf(NULL, 0, "type: %s, ok: %hhx", message_type_to_string(msg->type), msg->ok);
	char *str = malloc(size + 1);
	snprintf(str, size + 1, "type: %s, ok: %hhx", message_type_to_string(msg->type), msg->ok);
	return str;
}