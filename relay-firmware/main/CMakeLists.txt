idf_component_register(SRCS "app.c" "network.c" "../../common/network-common.c" "../../common/protocol.c"
                    INCLUDE_DIRS "../../common" )
component_compile_options("-Werror")