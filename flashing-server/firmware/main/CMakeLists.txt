idf_component_register(SRCS "test_avr_isp.c" "network.c" "avrisp.c" "node_config.c" "device.c" "MCP3461.c"
                    INCLUDE_DIRS "." ".." )
component_compile_options("-Werror")