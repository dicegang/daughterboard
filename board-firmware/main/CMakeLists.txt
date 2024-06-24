set(GENERATED_HEX_FILE ${CMAKE_CURRENT_SOURCE_DIR}/../../attiny84-spi-test/attiny_firmware.hex)


idf_component_register(SRCS "../../common/network-common.c" "avrisp.c" "node_config.c" "device.c" "MCP3461.c"
                            "crc8.c" "network.c" "app_wifi.c" "wifi.c"
                    INCLUDE_DIRS "../../common" "../../attiny84-spi-test"
                    EMBED_FILES ${GENERATED_HEX_FILE}
        EMBED_TXTFILES "certs/operator.crt" "certs/tower.pem"
        "certs/tower.crt")