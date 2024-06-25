set(GENERATED_HEX_FILE ${CMAKE_CURRENT_SOURCE_DIR}/../../attiny84-spi-test/attiny_firmware.hex)
string(REGEX REPLACE "-fno-builtin-[a-zA-Z0-9-]+" "" CMAKE_C_FLAGS "${CMAKE_C_FLAGS}")
string(REGEX REPLACE "-fno-builtin-[a-zA-Z0-9-]+" "" CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}")


idf_component_register(SRCS "../../common/network-common.c" "avrisp.c" "node_config.c" "management.c" "MCP3461.c"
                            "crc8.c" "network.c" "app_wifi.c" "wifi.c" "watchdog.c" "measurements.c"
                    INCLUDE_DIRS "../../common" "../../attiny84-spi-test"
                    EMBED_FILES ${GENERATED_HEX_FILE}
        EMBED_TXTFILES "certs/operator.crt" "certs/tower.pem"
        "certs/tower.crt")
component_compile_options("-Werror" "$<$<COMPILE_LANGUAGE:C>:-Wmissing-prototypes>")
