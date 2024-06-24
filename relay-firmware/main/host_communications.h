#ifndef HOST_COMMUNICATIONS_H
#define HOST_COMMUNICATIONS_H

#include <esp_err.h>
#include "comms.h"

void host_communications_init();
esp_err_t host_receive_message(struct comms_request *request);
esp_err_t host_send_message(struct comms_message const *msg);
#endif