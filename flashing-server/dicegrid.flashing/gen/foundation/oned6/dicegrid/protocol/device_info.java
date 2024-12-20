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
 * struct device_info {
 *     uint8_t node_count;
 *     struct node_info nodes[];
 * }
 * }
 */
class device_info {

    device_info() {
        // Should not be called directly
    }

    private static final GroupLayout $LAYOUT = MemoryLayout.structLayout(
        protocol_h.C_CHAR.withName("node_count"),
        MemoryLayout.paddingLayout(3),
        MemoryLayout.sequenceLayout(0, node_info.layout()).withName("nodes")
    ).withName("device_info");

    /**
     * The layout of this struct
     */
    public static final GroupLayout layout() {
        return $LAYOUT;
    }

    private static final OfByte node_count$LAYOUT = (OfByte)$LAYOUT.select(groupElement("node_count"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * uint8_t node_count
     * }
     */
    public static final OfByte node_count$layout() {
        return node_count$LAYOUT;
    }

    private static final long node_count$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * uint8_t node_count
     * }
     */
    public static final long node_count$offset() {
        return node_count$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * uint8_t node_count
     * }
     */
    public static byte node_count(MemorySegment struct) {
        return struct.get(node_count$LAYOUT, node_count$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * uint8_t node_count
     * }
     */
    public static void node_count(MemorySegment struct, byte fieldValue) {
        struct.set(node_count$LAYOUT, node_count$OFFSET, fieldValue);
    }

    private static final SequenceLayout nodes$LAYOUT = (SequenceLayout)$LAYOUT.select(groupElement("nodes"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * struct node_info nodes[]
     * }
     */
    public static final SequenceLayout nodes$layout() {
        return nodes$LAYOUT;
    }

    private static final long nodes$OFFSET = 4;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * struct node_info nodes[]
     * }
     */
    public static final long nodes$offset() {
        return nodes$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * struct node_info nodes[]
     * }
     */
    public static MemorySegment nodes(MemorySegment struct) {
        return struct.asSlice(nodes$OFFSET, nodes$LAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * struct node_info nodes[]
     * }
     */
    public static void nodes(MemorySegment struct, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, nodes$OFFSET, nodes$LAYOUT.byteSize());
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

