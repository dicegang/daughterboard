idf_component_register(SRCS "test_avr_isp.c" "../../common/network-common.c" "avrisp.c" "node_config.c" "device.c" "MCP3461.c"
                            "crc8.c" "network.c"
                    INCLUDE_DIRS "../../common" )
component_compile_options("-Werror" "$<$<COMPILE_LANGUAGE:C>:-Wmissing-prototypes>")
