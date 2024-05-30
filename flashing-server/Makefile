.PHONY: clean build_java

LIBS=$(subst $(subst ,, ),:,$(wildcard pi4j-2.6.0/lib/*.jar))

all: out/dist/dicegrid.flashing.jar

out/dist/dicegrid.flashing.jar: build_src
	$(JAVA_HOME)/bin/jar --create --file $@ -C out/classes/dicegrid.flashing .

build_src: bindings
	$(JAVA_HOME)/bin/javac --enable-preview --source 22 -d out/classes -m dicegrid.flashing --module-source-path "./*" \
		-p "${LIBS}" \
		--patch-module dicegrid.flashing=dicegrid.flashing/gen

bindings: comms_bindings protocol_bindings
	find dicegrid.flashing/gen -type f -name "*.java"  -exec sed -i "" 's/public class/class/g' {} +
	find dicegrid.flashing/gen -type f -name "*.java" -exec sed -i "" 's/private static final int/static final int/g' {} +

comms_bindings: dicegrid.flashing/gen/foundation/oned6/dicegrid/comms
protocol_bindings: dicegrid.flashing/gen/foundation/oned6/dicegrid/protocol

dicegrid.flashing/gen/foundation/oned6/dicegrid/comms: comms.h
	jextract $^  --output dicegrid.flashing/gen --target-package foundation.oned6.dicegrid.comms \
				--include-struct comms_request  --include-struct comms_response

dicegrid.flashing/gen/foundation/oned6/dicegrid/protocol: protocol.h
	jextract $^ --output dicegrid.flashing/gen --target-package foundation.oned6.dicegrid.protocol \
				--include-struct device_measurements --include-struct node_info --include-struct device_info \
				--include-struct node_state --include-struct request_msg --include-struct response_msg --include-constant CHUNK_SIZE

clean:
	rm -rf out/