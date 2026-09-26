package com.swingme.util;

import com.swingme.config.ItemOverride;
import com.swingme.gui.Row;
import com.swingme.gui.Rows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A compact, shareable text code for the mod's settings.
 * <p>
 * Only fields that differ from their default are encoded, so a code for "I nudged two sliders"
 * stays a handful of characters. The text form is Crockford base32, which survives being typed
 * back out of a screenshot: it has no {@code I}, {@code L}, {@code O} or {@code U}, and decoding
 * maps the look-alikes home ({@code I}/{@code L} to {@code 1}, {@code O} to {@code 0}).
 *
 * <h2>Wire format (MSB-first bit stream)</h2>
 * <pre>
 *   4 bits   version (currently 1)
 *   1 bit    scope: 0 = global / all items, 1 = single item
 *   repeat, in ascending registry index, for every non-default field:
 *     6 bits   registry index
 *     12 bits  quantized value   (float rows)
 *     1 bit    value             (toggle rows)
 *   6 bits   63, the end marker
 *   8 bits   FNV-1a checksum over everything above, zero-padded to a byte boundary
 *   zero padding to a byte boundary
 * </pre>
 *
 * <h2>Global-only rows</h2>
 * The rows contributed by {@code Rows.SCALE} and {@code Rows.VIEW} read and write
 * {@code SwingMeConfig} statics directly and ignore their {@link ItemOverride} argument
 * entirely, so any instance may be passed for those. They are never written into an
 * item-scope code, and a code that claims item scope while carrying one is rejected.
 */
public final class ShareCode {

    private ShareCode() {}

    /**
     * Bumped only on an incompatible format change; older codes are then rejected by name.
     * <p>
     * v2 dropped the villager scale row, repointing registry indices 45-55. Item-scope codes were
     * unaffected — the cutoff at {@link #ITEM_ROW_COUNT} did not move — but a v1 global code would
     * decode a slider where a toggle now sits, so v1 is refused rather than reinterpreted.
     */
    private static final int VERSION = 2;

    private static final int VERSION_BITS = 4;
    private static final int INDEX_BITS = 6;
    private static final int VALUE_BITS = 12;
    private static final int CHECKSUM_BITS = 8;

    /** Terminates the field list. Also the reason the registry can never reach 63 entries. */
    private static final int END_MARKER = 63;

    /** Largest value a 12-bit quantized float can take. */
    private static final int QUANT_MAX = 4095;

    /**
     * Every shareable row, flattened.
     * <p>
     * <b>The index into this list is the wire identifier, so this order is append-only forever.</b>
     * New rows go on the end of their own table and new tables go on the end of this list;
     * reordering or removing an entry silently repoints every code already in circulation.
     */
    public static final List<Row> REGISTRY = flatten();

    /**
     * Rows at or past this index are global-only (player scale, camera) and must never
     * appear in an item-scope code.
     */
    private static final int ITEM_ROW_COUNT =
            Rows.POSITION.size() + Rows.ITEM.size() + Rows.SWING.size();

    static {
        if (REGISTRY.size() >= END_MARKER) {
            throw new IllegalStateException(
                    "ShareCode registry holds " + REGISTRY.size() + " rows, but the 6-bit wire index "
                            + "reserves " + END_MARKER + " as its end marker. Widen the format and bump VERSION.");
        }
    }

    private static List<Row> flatten() {
        List<Row> all = new ArrayList<>();
        all.addAll(Rows.POSITION);
        all.addAll(Rows.ITEM);
        all.addAll(Rows.SWING);
        all.addAll(Rows.SCALE);
        all.addAll(Rows.VIEW);
        return List.copyOf(all);
    }

    // -- Encoding -----------------------------------------------------------

    /**
     * Writes every non-default field of {@code source} into a share code.
     * <p>
     * Global-only rows ignore {@code source} and read {@code SwingMeConfig} instead, so when
     * {@code itemScope} is false the instance passed in only supplies the per-item half.
     *
     * @param itemScope true to encode only the per-item settings.
     * @return the code in uppercase Crockford base32, never null.
     */
    public static String encode(ItemOverride source, boolean itemScope) {
        BitWriter w = new BitWriter();
        w.write(VERSION, VERSION_BITS);
        w.write(itemScope ? 1 : 0, 1);

        for (int i = 0; i < REGISTRY.size(); i++) {
            if (itemScope && i >= ITEM_ROW_COUNT) continue;

            Row row = REGISTRY.get(i);
            Object value = row.get(source);
            if (value.equals(row.defaultValue())) continue;

            w.write(i, INDEX_BITS);
            if (row.isFloat()) {
                w.write(quantize((Float) value, row.min(), row.max()), VALUE_BITS);
            } else {
                w.write(((Boolean) value) ? 1 : 0, 1);
            }
        }

        w.write(END_MARKER, INDEX_BITS);
        w.write(fnv1a8(w.prefix(w.bitLength())), CHECKSUM_BITS);
        w.padToByte();
        return base32Encode(w.toBytes());
    }

    // -- Decoding -----------------------------------------------------------

    /** Outcome of decoding, so callers can report a reason without exceptions. */
    public static final class Result {
        public final boolean ok;
        /** Short, chat-safe reason for the rejection; empty when {@link #ok}. */
        public final String error;
        public final boolean itemScope;
        /** Fields written. Always 0 when the code was rejected. */
        public final int applied;

        private Result(boolean ok, String error, boolean itemScope, int applied) {
            this.ok = ok;
            this.error = error;
            this.itemScope = itemScope;
            this.applied = applied;
        }

        private static Result failed(String error) {
            return new Result(false, error, false, 0);
        }

        private static Result succeeded(boolean itemScope, int applied) {
            return new Result(true, "", itemScope, applied);
        }
    }

    /**
     * Validates {@code code} and writes its values into {@code target}.
     * <p>
     * All or nothing: the whole code is parsed and checked before a single field is written,
     * so a rejected code leaves {@code target} exactly as it was. Global-only rows write
     * {@code SwingMeConfig} statics and ignore {@code target}.
     * <p>
     * Applying is a replacement, not a merge. Every field the code covers is first put back to
     * its default, so a field the sender left at default arrives at default rather than keeping
     * whatever the receiver happened to have. Without that, a code would not reproduce the
     * settings it was made from.
     *
     * @param expectItemScope the scope the caller is editing; a code made for the other scope is
     *                        rejected rather than half-applied.
     * @return the outcome; on failure {@link Result#error} says why, in one short line.
     */
    public static Result apply(String code, ItemOverride target, boolean expectItemScope) {
        if (code == null) return Result.failed("empty code");

        String cleaned = strip(code);
        if (cleaned.isEmpty()) return Result.failed("empty code");

        byte[] data = base32Decode(cleaned);
        if (data == null) {
            for (int i = 0; i < cleaned.length(); i++) {
                if (base32Value(cleaned.charAt(i)) < 0) {
                    return Result.failed("bad character " + cleaned.charAt(i) + " in code");
                }
            }
            return Result.failed("bad character in code");
        }

        BitReader r = new BitReader(data);
        if (!r.has(VERSION_BITS + 1)) return Result.failed("code is too short");

        int version = r.read(VERSION_BITS);
        if (version != VERSION) return Result.failed("code version " + version + " is not supported");

        boolean itemScope = r.read(1) == 1;
        if (itemScope != expectItemScope) {
            return Result.failed(itemScope
                    ? "that code is for a single item"
                    : "that code is for All Items");
        }

        List<Pending> pending = new ArrayList<>();
        while (true) {
            if (!r.has(INDEX_BITS)) return Result.failed("code is truncated");
            int index = r.read(INDEX_BITS);
            if (index == END_MARKER) break;

            if (index >= REGISTRY.size()) return Result.failed("unknown setting " + index + " in code");
            if (itemScope && index >= ITEM_ROW_COUNT) {
                return Result.failed("item code carries a global setting");
            }

            Row row = REGISTRY.get(index);
            if (row.isFloat()) {
                if (!r.has(VALUE_BITS)) return Result.failed("code is truncated");
                pending.add(new Pending(row, dequantize(r.read(VALUE_BITS), row.min(), row.max())));
            } else {
                if (!r.has(1)) return Result.failed("code is truncated");
                pending.add(new Pending(row, r.read(1) == 1));
            }
        }

        int payloadBits = r.position();
        if (!r.has(CHECKSUM_BITS)) return Result.failed("code is truncated");
        if (r.read(CHECKSUM_BITS) != fnv1a8(prefixBytes(data, payloadBits))) {
            return Result.failed("code is damaged (checksum failed)");
        }

        int covered = itemScope ? ITEM_ROW_COUNT : REGISTRY.size();
        for (int i = 0; i < covered; i++) {
            REGISTRY.get(i).reset(target);
        }
        for (Pending p : pending) {
            p.row().set(target, p.value());
        }
        return Result.succeeded(itemScope, pending.size());
    }

    /** One validated field, held back until the whole code has parsed. */
    private record Pending(Row row, Object value) {}

    // ==  Self-contained bit and text plumbing  =============================
    // Nothing below this line touches Row, ItemOverride or any Minecraft class, so it can be
    // lifted out verbatim and exercised on a bare JVM.

    private static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";

    /**
     * Maps a slider value onto 12 bits.
     * <p>
     * 12 bits (4096 steps) matches or beats the precision the config itself stores:
     * {@code precision = 1000} over a range of 4 is 4000 steps and {@code precision = 10}
     * over 360 is 3600, so a round trip through a share code is lossless in practice.
     */
    static int quantize(float value, float min, float max) {
        if (!(max > min)) return 0;
        int q = Math.round((value - min) / (max - min) * QUANT_MAX);
        return Math.max(0, Math.min(QUANT_MAX, q));
    }

    static float dequantize(int q, float min, float max) {
        return min + q / (float) QUANT_MAX * (max - min);
    }

    /** FNV-1a, folded down to its low 8 bits. Catches typos, not tampering. */
    static int fnv1a8(byte[] data) {
        int hash = 0x811C9DC5;
        for (byte b : data) {
            hash ^= (b & 0xFF);
            hash *= 0x01000193;
        }
        return hash & 0xFF;
    }

    /** The first {@code bitLength} bits of {@code data}, zero-padded out to a whole byte. */
    static byte[] prefixBytes(byte[] data, int bitLength) {
        int length = (bitLength + 7) / 8;
        byte[] out = Arrays.copyOf(data, length);
        int used = bitLength & 7;
        if (used != 0) out[length - 1] &= (byte) (0xFF << (8 - used));
        return out;
    }

    /** Appends bits MSB-first, growing as it goes. */
    static final class BitWriter {
        private byte[] buf = new byte[16];
        private int bits;

        void write(int value, int count) {
            for (int i = count - 1; i >= 0; i--) writeBit((value >>> i) & 1);
        }

        void writeBit(int bit) {
            int index = bits >>> 3;
            if (index >= buf.length) buf = Arrays.copyOf(buf, buf.length * 2);
            if (bit != 0) buf[index] |= (byte) (1 << (7 - (bits & 7)));
            bits++;
        }

        int bitLength() {
            return bits;
        }

        void padToByte() {
            while ((bits & 7) != 0) writeBit(0);
        }

        /** What has been written so far, zero-padded to a byte boundary. */
        byte[] prefix(int count) {
            return prefixBytes(buf, count);
        }

        byte[] toBytes() {
            return prefixBytes(buf, bits);
        }
    }

    /** Reads bits MSB-first. Every {@link #read} must be guarded by {@link #has}. */
    static final class BitReader {
        private final byte[] data;
        private final int bits;
        private int pos;

        BitReader(byte[] data) {
            this.data = data;
            this.bits = data.length * 8;
        }

        boolean has(int count) {
            return pos + count <= bits;
        }

        int position() {
            return pos;
        }

        int read(int count) {
            int value = 0;
            for (int i = 0; i < count; i++) {
                value = (value << 1) | ((data[pos >>> 3] >>> (7 - (pos & 7))) & 1);
                pos++;
            }
            return value;
        }
    }

    /** Drops whitespace and the grouping dashes people add by hand. */
    static String strip(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!Character.isWhitespace(c) && c != '-') out.append(c);
        }
        return out.toString();
    }

    /** @return the Crockford digit value, or -1 when the character is not one. */
    static int base32Value(char c) {
        char upper = Character.toUpperCase(c);
        if (upper == 'I' || upper == 'L') upper = '1';
        else if (upper == 'O') upper = '0';
        return ALPHABET.indexOf(upper);
    }

    static String base32Encode(byte[] data) {
        int total = data.length * 8;
        StringBuilder out = new StringBuilder((total + 4) / 5);
        for (int start = 0; start < total; start += 5) {
            int value = 0;
            for (int i = 0; i < 5; i++) {
                int bit = start + i;
                int b = bit < total ? (data[bit >>> 3] >>> (7 - (bit & 7))) & 1 : 0;
                value = (value << 1) | b;
            }
            out.append(ALPHABET.charAt(value));
        }
        return out.toString();
    }

    /**
     * Decodes Crockford base32 back into bytes, dropping the trailing partial byte that the
     * 5-bit grouping leaves behind.
     *
     * @return the bytes, or null when {@code text} holds a character outside the alphabet.
     */
    static byte[] base32Decode(String text) {
        byte[] out = new byte[text.length() * 5 / 8];
        int pos = 0;
        int total = out.length * 8;
        for (int i = 0; i < text.length(); i++) {
            int value = base32Value(text.charAt(i));
            if (value < 0) return null;
            for (int j = 4; j >= 0; j--) {
                if (pos < total && ((value >>> j) & 1) != 0) {
                    out[pos >>> 3] |= (byte) (1 << (7 - (pos & 7)));
                }
                pos++;
            }
        }
        return out;
    }
}
