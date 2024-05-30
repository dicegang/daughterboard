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

/**
 * {@snippet lang=c :
 * struct node_info {
 *     enum {
 *         NODE_TYPE_SOURCE,
 *         NODE_TYPE_LOAD
 *     } type;
 *     uint8_t node_id;
 *     uint8_t owner_id;
 * }
 * }
 */
class node_info {

    node_info() {
        // Should not be called directly
    }

    private static final GroupLayout $LAYOUT = MemoryLayout.structLayout(
        protocol_h.C_INT.withName("type"),
        protocol_h.C_CHAR.withName("node_id"),
        protocol_h.C_CHAR.withName("owner_id"),
        MemoryLayout.paddingLayout(2)
    ).withName("node_info");

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
     *     NODE_TYPE_SOURCE,
     *     NODE_TYPE_LOAD
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
     *     NODE_TYPE_SOURCE,
     *     NODE_TYPE_LOAD
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
     *     NODE_TYPE_SOURCE,
     *     NODE_TYPE_LOAD
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
     *     NODE_TYPE_SOURCE,
     *     NODE_TYPE_LOAD
     * } type
     * }
     */
    public static void type(MemorySegment struct, int fieldValue) {
        struct.set(type$LAYOUT, type$OFFSET, fieldValue);
    }

    private static final OfByte node_id$LAYOUT = (OfByte)$LAYOUT.select(groupElement("node_id"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * uint8_t node_id
     * }
     */
    public static final OfByte node_id$layout() {
        return node_id$LAYOUT;
    }

    private static final long node_id$OFFSET = 4;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * uint8_t node_id
     * }
     */
    public static final long node_id$offset() {
        return node_id$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * uint8_t node_id
     * }
     */
    public static byte node_id(MemorySegment struct) {
        return struct.get(node_id$LAYOUT, node_id$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * uint8_t node_id
     * }
     */
    public static void node_id(MemorySegment struct, byte fieldValue) {
        struct.set(node_id$LAYOUT, node_id$OFFSET, fieldValue);
    }

    private static final OfByte owner_id$LAYOUT = (OfByte)$LAYOUT.select(groupElement("owner_id"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * uint8_t owner_id
     * }
     */
    public static final OfByte owner_id$layout() {
        return owner_id$LAYOUT;
    }

    private static final long owner_id$OFFSET = 5;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * uint8_t owner_id
     * }
     */
    public static final long owner_id$offset() {
        return owner_id$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * uint8_t owner_id
     * }
     */
    public static byte owner_id(MemorySegment struct) {
        return struct.get(owner_id$LAYOUT, owner_id$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * uint8_t owner_id
     * }
     */
    public static void owner_id(MemorySegment struct, byte fieldValue) {
        struct.set(owner_id$LAYOUT, owner_id$OFFSET, fieldValue);
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

