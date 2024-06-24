#ifndef ATTINY_FW_H_ 
#define ATTINY_FW_H_

#include <stdint.h>
#include <stddef.h>

extern uint8_t const attiny_firmware[] asm("attiny_firmware_hex");
extern size_t const attiny_firmware_len asm("attiny_firmware_hex_length");

#endif 				 		 
