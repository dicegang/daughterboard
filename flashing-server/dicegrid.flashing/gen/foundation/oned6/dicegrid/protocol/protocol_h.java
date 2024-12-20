// Generated by jextract

package foundation.oned6.dicegrid.protocol;

import java.lang.invoke.*;
import java.lang.foreign.*;
import java.nio.ByteOrder;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static java.lang.foreign.ValueLayout.*;
import static java.lang.foreign.MemoryLayout.PathElement.*;

class protocol_h {

    protocol_h() {
        // Should not be called directly
    }

    static final Arena LIBRARY_ARENA = Arena.ofAuto();
    static final boolean TRACE_DOWNCALLS = Boolean.getBoolean("jextract.trace.downcalls");

    static void traceDowncall(String name, Object... args) {
         String traceArgs = Arrays.stream(args)
                       .map(Object::toString)
                       .collect(Collectors.joining(", "));
         System.out.printf("%s(%s)\n", name, traceArgs);
    }

    static MemorySegment findOrThrow(String symbol) {
        return SYMBOL_LOOKUP.find(symbol)
            .orElseThrow(() -> new UnsatisfiedLinkError("unresolved symbol: " + symbol));
    }

    static MethodHandle upcallHandle(Class<?> fi, String name, FunctionDescriptor fdesc) {
        try {
            return MethodHandles.lookup().findVirtual(fi, name, fdesc.toMethodType());
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

    static final SymbolLookup SYMBOL_LOOKUP = SymbolLookup.loaderLookup()
            .or(Linker.nativeLinker().defaultLookup());

    public static final ValueLayout.OfBoolean C_BOOL = ValueLayout.JAVA_BOOLEAN;
    public static final ValueLayout.OfByte C_CHAR = ValueLayout.JAVA_BYTE;
    public static final ValueLayout.OfShort C_SHORT = ValueLayout.JAVA_SHORT;
    public static final ValueLayout.OfInt C_INT = ValueLayout.JAVA_INT;
    public static final ValueLayout.OfLong C_LONG_LONG = ValueLayout.JAVA_LONG;
    public static final ValueLayout.OfFloat C_FLOAT = ValueLayout.JAVA_FLOAT;
    public static final ValueLayout.OfDouble C_DOUBLE = ValueLayout.JAVA_DOUBLE;
    public static final AddressLayout C_POINTER = ValueLayout.ADDRESS
            .withTargetLayout(MemoryLayout.sequenceLayout(java.lang.Long.MAX_VALUE, JAVA_BYTE));
    public static final ValueLayout.OfLong C_LONG = ValueLayout.JAVA_LONG;
    static final int MAX_CHUNK_SIZE = (int)16L;
    /**
     * {@snippet lang=c :
     * #define MAX_CHUNK_SIZE 16
     * }
     */
    public static int MAX_CHUNK_SIZE() {
        return MAX_CHUNK_SIZE;
    }
    static final int TRIP_REASON_NONE = (int)0L;
    /**
     * {@snippet lang=c :
     * enum <anonymous>.TRIP_REASON_NONE = 0
     * }
     */
    public static int TRIP_REASON_NONE() {
        return TRIP_REASON_NONE;
    }
    static final int TRIP_REASON_OVERCURRENT = (int)1L;
    /**
     * {@snippet lang=c :
     * enum <anonymous>.TRIP_REASON_OVERCURRENT = 1
     * }
     */
    public static int TRIP_REASON_OVERCURRENT() {
        return TRIP_REASON_OVERCURRENT;
    }
    static final int TRIP_REASON_OVERVOLTAGE = (int)2L;
    /**
     * {@snippet lang=c :
     * enum <anonymous>.TRIP_REASON_OVERVOLTAGE = 2
     * }
     */
    public static int TRIP_REASON_OVERVOLTAGE() {
        return TRIP_REASON_OVERVOLTAGE;
    }
    static final int TRIP_REASON_ANGLE = (int)3L;
    /**
     * {@snippet lang=c :
     * enum <anonymous>.TRIP_REASON_ANGLE = 3
     * }
     */
    public static int TRIP_REASON_ANGLE() {
        return TRIP_REASON_ANGLE;
    }
    static final int TRIP_REASON_THD = (int)4L;
    /**
     * {@snippet lang=c :
     * enum <anonymous>.TRIP_REASON_THD = 4
     * }
     */
    public static int TRIP_REASON_THD() {
        return TRIP_REASON_THD;
    }
    static final int TRIP_REASON_MANUAL = (int)5L;
    /**
     * {@snippet lang=c :
     * enum <anonymous>.TRIP_REASON_MANUAL = 5
     * }
     */
    public static int TRIP_REASON_MANUAL() {
        return TRIP_REASON_MANUAL;
    }
    static final int NODE_TYPE_SOURCE = (int)0L;
    /**
     * {@snippet lang=c :
     * enum <anonymous>.NODE_TYPE_SOURCE = 0
     * }
     */
    public static int NODE_TYPE_SOURCE() {
        return NODE_TYPE_SOURCE;
    }
    static final int NODE_TYPE_LOAD = (int)1L;
    /**
     * {@snippet lang=c :
     * enum <anonymous>.NODE_TYPE_LOAD = 1
     * }
     */
    public static int NODE_TYPE_LOAD() {
        return NODE_TYPE_LOAD;
    }
    static final int FLASH_BEGIN = (int)0L;
    /**
     * {@snippet lang=c :
     * enum message_type.FLASH_BEGIN = 0
     * }
     */
    public static int FLASH_BEGIN() {
        return FLASH_BEGIN;
    }
    static final int FLASH_DATA = (int)1L;
    /**
     * {@snippet lang=c :
     * enum message_type.FLASH_DATA = 1
     * }
     */
    public static int FLASH_DATA() {
        return FLASH_DATA;
    }
    static final int FLASH_DATA_END = (int)2L;
    /**
     * {@snippet lang=c :
     * enum message_type.FLASH_DATA_END = 2
     * }
     */
    public static int FLASH_DATA_END() {
        return FLASH_DATA_END;
    }
    static final int CONFIGURE_SHUTDOWN = (int)3L;
    /**
     * {@snippet lang=c :
     * enum message_type.CONFIGURE_SHUTDOWN = 3
     * }
     */
    public static int CONFIGURE_SHUTDOWN() {
        return CONFIGURE_SHUTDOWN;
    }
    static final int CONFIGURE_ENGAGEMENT = (int)4L;
    /**
     * {@snippet lang=c :
     * enum message_type.CONFIGURE_ENGAGEMENT = 4
     * }
     */
    public static int CONFIGURE_ENGAGEMENT() {
        return CONFIGURE_ENGAGEMENT;
    }
    static final int NODE_STATE = (int)5L;
    /**
     * {@snippet lang=c :
     * enum message_type.NODE_STATE = 5
     * }
     */
    public static int NODE_STATE() {
        return NODE_STATE;
    }
    static final int SCAN = (int)6L;
    /**
     * {@snippet lang=c :
     * enum message_type.SCAN = 6
     * }
     */
    public static int SCAN() {
        return SCAN;
    }
    static final int SET_NODE_INFO = (int)7L;
    /**
     * {@snippet lang=c :
     * enum message_type.SET_NODE_INFO = 7
     * }
     */
    public static int SET_NODE_INFO() {
        return SET_NODE_INFO;
    }
}

