FROM alpine:3.20

RUN apk add ca-certificates wget make cmake avrdude avr-libc gcc-avr binutils-avr

RUN echo "avr-gcc -Wall -Os -DF_CPU=8000000 -mmcu=attiny85 -c /input.c -o /output.o" >> /compile.sh
RUN echo "avr-gcc -Wall -Os -DF_CPU=8000000 -mmcu=attiny85 -o /output.elf /output.o" >> /compile.sh
RUN echo "avr-objcopy -j .text -j .data -O ihex /output.elf /output.hex" >> /compile.sh
RUN echo "avr-size --format=avr --mcu=attiny85 /output.elf" >> /compile.sh
RUN chmod +x /compile.sh

ENTRYPOINT ["/compile.sh"]