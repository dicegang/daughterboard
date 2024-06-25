# re-enable builtins
string(REGEX REPLACE "-fno-builtin-[a-zA-Z0-9-]+" "" CMAKE_C_FLAGS "${CMAKE_C_FLAGS}")
string(REGEX REPLACE "-fno-builtin-[a-zA-Z0-9-]+" "" CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}")

idf_component_register(SRCS "test_adc.c" "../../common/network-common.c" "avrisp.c" "node_config.c" "device.c" "MCP3461.c"
                            "crc8.c" "network.c"
                    INCLUDE_DIRS "../../common" )
component_compile_options("-Werror" "$<$<COMPILE_LANGUAGE:C>:-Wmissing-prototypes>")
