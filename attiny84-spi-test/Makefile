CC=avr-gcc
OBJCOPY=avr-objcopy
CFLAGS=-Wall -O0 -DF_CPU=8000000UL -mmcu=attiny84

all: attiny_firmware.hex
attiny_firmware.hex: main.elf
	$(OBJCOPY) -j .text -j .data -O ihex $< $@


%.elf: %.o
	$(CC) $(CFLAGS) -o $@ $<

%.o: %.c
	$(CC) $(CFLAGS) -c -o $@ $<


clean:
	rm -f *.o *.elf *.hex attiny_firmware.c