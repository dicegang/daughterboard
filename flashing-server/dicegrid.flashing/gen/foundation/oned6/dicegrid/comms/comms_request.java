// Generated by jextract

package foundation.oned6.dicegrid.comms;

import java.lang.invoke.*;
import java.lang.foreign.*;
import java.nio.ByteOrder;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static java.lang.foreign.ValueLayout.*;
import static java.lang.foreign.MemoryLayout.PathElement.*;

/**
 * {@snippet lang=c :
 * struct comms_request {
 *     enum {
 *         COMMS_REQ_UNICAST,
 *         COMMS_REQ_BROADCAST,
 *         COMMS_REQ_HELLO
 *     } type;
 *     mac_address_t recipient;
 *     uint32_t message_size;
 *     uint8_t message[];
 * }
 * }
 */
class comms_request {

    comms_request() {
        // Should not be called directly
    }

    private static final GroupLayout $LAYOUT = MemoryLayout.structLayout(
        comms_h.C_INT.withName("type"),
        MemoryLayout.sequenceLayout(6, comms_h.C_CHAR).withName("recipient"),
        MemoryLayout.paddingLayout(2),
        comms_h.C_INT.withName("message_size"),
        MemoryLayout.sequenceLayout(0, comms_h.C_CHAR).withName("message")
    ).withName("comms_request");

    /**
     * The layout of this struct
     */
    public static final GroupLayout layout() {
        return $LAYOUT;
    }

    private static final OfInt type$LAYOUT = (OfInt)$LAYOUT.select(groupElement("type"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * enum {
     *     COMMS_REQ_UNICAST,
     *     COMMS_REQ_BROADCAST,
     *     COMMS_REQ_HELLO
     * } type
     * }
     */
    public static final OfInt type$layout() {
        return type$LAYOUT;
    }

    private static final long type$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * enum {
     *     COMMS_REQ_UNICAST,
     *     COMMS_REQ_BROADCAST,
     *     COMMS_REQ_HELLO
     * } type
     * }
     */
    public static final long type$offset() {
        return type$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * enum {
     *     COMMS_REQ_UNICAST,
     *     COMMS_REQ_BROADCAST,
     *     COMMS_REQ_HELLO
     * } type
     * }
     */
    public static int type(MemorySegment struct) {
        return struct.get(type$LAYOUT, type$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * enum {
     *     COMMS_REQ_UNICAST,
     *     COMMS_REQ_BROADCAST,
     *     COMMS_REQ_HELLO
     * } type
     * }
     */
    public static void type(MemorySegment struct, int fieldValue) {
        struct.set(type$LAYOUT, type$OFFSET, fieldValue);
    }

    private static final SequenceLayout recipient$LAYOUT = (SequenceLayout)$LAYOUT.select(groupElement("recipient"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * mac_address_t recipient
     * }
     */
    public static final SequenceLayout recipient$layout() {
        return recipient$LAYOUT;
    }

    private static final long recipient$OFFSET = 4;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * mac_address_t recipient
     * }
     */
    public static final long recipient$offset() {
        return recipient$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * mac_address_t recipient
     * }
     */
    public static MemorySegment recipient(MemorySegment struct) {
        return struct.asSlice(recipient$OFFSET, recipient$LAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * mac_address_t recipient
     * }
     */
    public static void recipient(MemorySegment struct, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, recipient$OFFSET, recipient$LAYOUT.byteSize());
    }

    private static final OfInt message_size$LAYOUT = (OfInt)$LAYOUT.select(groupElement("message_size"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * uint32_t message_size
     * }
     */
    public static final OfInt message_size$layout() {
        return message_size$LAYOUT;
    }

    private static final long message_size$OFFSET = 12;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * uint32_t message_size
     * }
     */
    public static final long message_size$offset() {
        return message_size$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * uint32_t message_size
     * }
     */
    public static int message_size(MemorySegment struct) {
        return struct.get(message_size$LAYOUT, message_size$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * uint32_t message_size
     * }
     */
    public static void message_size(MemorySegment struct, int fieldValue) {
        struct.set(message_size$LAYOUT, message_size$OFFSET, fieldValue);
    }

    private static final SequenceLayout message$LAYOUT = (SequenceLayout)$LAYOUT.select(groupElement("message"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * uint8_t message[]
     * }
     */
    public static final SequenceLayout message$layout() {
        return message$LAYOUT;
    }

    private static final long message$OFFSET = 16;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * uint8_t message[]
     * }
     */
    public static final long message$offset() {
        return message$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * uint8_t message[]
     * }
     */
    public static MemorySegment message(MemorySegment struct) {
        return struct.asSlice(message$OFFSET, message$LAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * uint8_t message[]
     * }
     */
    public static void message(MemorySegment struct, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, message$OFFSET, message$LAYOUT.byteSize());
    }

    /**
     * Obtains a slice of {@code arrayParam} which selects the array element at {@code index}.
     * The returned segment has address {@code arrayParam.address() + index * layout().byteSize()}
     */
    public static MemorySegment asSlice(MemorySegment array, long index) {
        return array.asSlice(layout().byteSize() * index);
    }

    /**
     * The size (in bytes) of this struct
     */
    public static long sizeof() { return layout().byteSize(); }

    /**
     * Allocate a segment of size {@code layout().byteSize()} using {@code allocator}
     */
    public static MemorySegment allocate(SegmentAllocator allocator) {
        return allocator.allocate(layout());
    }

    /**
     * Allocate an array of size {@code elementCount} using {@code allocator}.
     * The returned segment has size {@code elementCount * layout().byteSize()}.
     */
    public static MemorySegment allocateArray(long elementCount, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(elementCount, layout()));
    }

    /**
     * Reinterprets {@code addr} using target {@code arena} and {@code cleanupAction) (if any).
     * The returned segment has size {@code layout().byteSize()}
     */
    public static MemorySegment reinterpret(MemorySegment addr, Arena arena, Consumer<MemorySegment> cleanup) {
        return reinterpret(addr, 1, arena, cleanup);
    }

    /**
     * Reinterprets {@code addr} using target {@code arena} and {@code cleanupAction) (if any).
     * The returned segment has size {@code elementCount * layout().byteSize()}
     */
    public static MemorySegment reinterpret(MemorySegment addr, long elementCount, Arena arena, Consumer<MemorySegment> cleanup) {
        return addr.reinterpret(layout().byteSize() * elementCount, arena, cleanup);
    }
}
