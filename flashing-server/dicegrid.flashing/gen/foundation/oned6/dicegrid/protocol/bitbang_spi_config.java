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
 * struct bitbang_spi_config {
 *     uint8_t rst;
 *     uint8_t clk;
 *     uint8_t mosi;
 *     uint8_t miso;
 *     uint32_t clock_rate;
 * }
 * }
 */
class bitbang_spi_config {

    bitbang_spi_config() {
        // Should not be called directly
    }

    private static final GroupLayout $LAYOUT = MemoryLayout.structLayout(
        protocol_h.C_CHAR.withName("rst"),
        protocol_h.C_CHAR.withName("clk"),
        protocol_h.C_CHAR.withName("mosi"),
        protocol_h.C_CHAR.withName("miso"),
        protocol_h.C_INT.withName("clock_rate")
    ).withName("bitbang_spi_config");

    /**
     * The layout of this struct
     */
    public static final GroupLayout layout() {
        return $LAYOUT;
    }

    private static final OfByte rst$LAYOUT = (OfByte)$LAYOUT.select(groupElement("rst"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * uint8_t rst
     * }
     */
    public static final OfByte rst$layout() {
        return rst$LAYOUT;
    }

    private static final long rst$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * uint8_t rst
     * }
     */
    public static final long rst$offset() {
        return rst$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * uint8_t rst
     * }
     */
    public static byte rst(MemorySegment struct) {
        return struct.get(rst$LAYOUT, rst$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * uint8_t rst
     * }
     */
    public static void rst(MemorySegment struct, byte fieldValue) {
        struct.set(rst$LAYOUT, rst$OFFSET, fieldValue);
    }

    private static final OfByte clk$LAYOUT = (OfByte)$LAYOUT.select(groupElement("clk"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * uint8_t clk
     * }
     */
    public static final OfByte clk$layout() {
        return clk$LAYOUT;
    }

    private static final long clk$OFFSET = 1;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * uint8_t clk
     * }
     */
    public static final long clk$offset() {
        return clk$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * uint8_t clk
     * }
     */
    public static byte clk(MemorySegment struct) {
        return struct.get(clk$LAYOUT, clk$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * uint8_t clk
     * }
     */
    public static void clk(MemorySegment struct, byte fieldValue) {
        struct.set(clk$LAYOUT, clk$OFFSET, fieldValue);
    }

    private static final OfByte mosi$LAYOUT = (OfByte)$LAYOUT.select(groupElement("mosi"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * uint8_t mosi
     * }
     */
    public static final OfByte mosi$layout() {
        return mosi$LAYOUT;
    }

    private static final long mosi$OFFSET = 2;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * uint8_t mosi
     * }
     */
    public static final long mosi$offset() {
        return mosi$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * uint8_t mosi
     * }
     */
    public static byte mosi(MemorySegment struct) {
        return struct.get(mosi$LAYOUT, mosi$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * uint8_t mosi
     * }
     */
    public static void mosi(MemorySegment struct, byte fieldValue) {
        struct.set(mosi$LAYOUT, mosi$OFFSET, fieldValue);
    }

    private static final OfByte miso$LAYOUT = (OfByte)$LAYOUT.select(groupElement("miso"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * uint8_t miso
     * }
     */
    public static final OfByte miso$layout() {
        return miso$LAYOUT;
    }

    private static final long miso$OFFSET = 3;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * uint8_t miso
     * }
     */
    public static final long miso$offset() {
        return miso$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * uint8_t miso
     * }
     */
    public static byte miso(MemorySegment struct) {
        return struct.get(miso$LAYOUT, miso$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * uint8_t miso
     * }
     */
    public static void miso(MemorySegment struct, byte fieldValue) {
        struct.set(miso$LAYOUT, miso$OFFSET, fieldValue);
    }

    private static final OfInt clock_rate$LAYOUT = (OfInt)$LAYOUT.select(groupElement("clock_rate"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * uint32_t clock_rate
     * }
     */
    public static final OfInt clock_rate$layout() {
        return clock_rate$LAYOUT;
    }

    private static final long clock_rate$OFFSET = 4;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * uint32_t clock_rate
     * }
     */
    public static final long clock_rate$offset() {
        return clock_rate$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * uint32_t clock_rate
     * }
     */
    public static int clock_rate(MemorySegment struct) {
        return struct.get(clock_rate$LAYOUT, clock_rate$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * uint32_t clock_rate
     * }
     */
    public static void clock_rate(MemorySegment struct, int fieldValue) {
        struct.set(clock_rate$LAYOUT, clock_rate$OFFSET, fieldValue);
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

