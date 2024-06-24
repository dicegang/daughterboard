#include <avr/io.h>
#include <avr/interrupt.h>
#include <avr/sleep.h>
#include <util/delay.h>
#include <stdbool.h>

#define BIT(n) (1 << n)

#define MISO PA5
#define CS_PORT PINA
#define CS_PIN PA7

static void await_transfer() {
	while (!(USISR & BIT(USIOIF)));
}

static void await_cs() {
	while (!(CS_PORT & BIT(CS_PIN)));
}

static void init_spi(void) {
	USICR |= BIT(USIWM0) | BIT(USICS1);
	DDRA = BIT(MISO);
	USIDR = 0;
}

static uint8_t transfer(uint8_t out, uint8_t count) {
	USISR = BIT(USIOIF) | ((16 - count) & 0x0F);
	USIDR = out;

	await_transfer();
	return USIDR;
}


int main (void)
{
	init_spi();

	for (;;) {
		await_cs();

		uint8_t bytes_in = transfer(0xcc, 8);
		transfer(bytes_in, 8);
	}
}