package io.github.s7i.doer.shade.flink;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 *  Apache Flink source code part.
 *  Shaded : https://github.com/apache/flink/blob/master/flink-core/src/main/java/org/apache/flink/types/StringValue.java
 */
public class StringValue {

    private static final int HIGH_BIT = 0x1 << 7;

    private static final int HIGH_BIT14 = 0x1 << 14;

    private static final int HIGH_BIT21 = 0x1 << 21;

    private static final int HIGH_BIT28 = 0x1 << 28;

    private static final int HIGH_BIT2 = 0x1 << 13;

    private static final int HIGH_BIT2_MASK = 0x3 << 6;

    private static final int SHORT_STRING_MAX_LENGTH = 2048;

    private static final ThreadLocal<char[]> charBuffer =
            ThreadLocal.withInitial(() -> new char[SHORT_STRING_MAX_LENGTH]);

    public static String readString(DataInput in) throws IOException {
        // the length we read is offset by one, because a length of zero indicates a null value
        int len = in.readUnsignedByte();

        if (len == 0) {
            return null;
        }

        if (len >= HIGH_BIT) {
            int shift = 7;
            int curr;
            len = len & 0x7f;
            while ((curr = in.readUnsignedByte()) >= HIGH_BIT) {
                len |= (curr & 0x7f) << shift;
                shift += 7;
            }
            len |= curr << shift;
        }

        // subtract one for the null length
        len -= 1;

        final char[] data;
        if (len > SHORT_STRING_MAX_LENGTH) {
            data = new char[len];
        } else {
            data = charBuffer.get();
        }

        for (int i = 0; i < len; i++) {
            int c = in.readUnsignedByte();
            if (c >= HIGH_BIT) {
                int shift = 7;
                int curr;
                c = c & 0x7f;
                while ((curr = in.readUnsignedByte()) >= HIGH_BIT) {
                    c |= (curr & 0x7f) << shift;
                    shift += 7;
                }
                c |= curr << shift;
            }
            data[i] = (char) c;
        }

        return new String(data, 0, len);
    }

    public static final void writeString(CharSequence cs, DataOutput out) throws IOException {
        if (cs != null) {
            int strlen = cs.length();

            // the length we write is offset by one, because a length of zero indicates a null value
            int lenToWrite = strlen + 1;
            if (lenToWrite < 0) {
                throw new IllegalArgumentException("CharSequence is too long.");
            }

            // string is prefixed by it's variable length encoded size, which can take 1-5 bytes.
            if (lenToWrite < HIGH_BIT) {
                out.write((byte) lenToWrite);
            } else if (lenToWrite < HIGH_BIT14) {
                out.write((lenToWrite | HIGH_BIT));
                out.write((lenToWrite >>> 7));
            } else if (lenToWrite < HIGH_BIT21) {
                out.write(lenToWrite | HIGH_BIT);
                out.write((lenToWrite >>> 7) | HIGH_BIT);
                out.write((lenToWrite >>> 14));
            } else if (lenToWrite < HIGH_BIT28) {
                out.write(lenToWrite | HIGH_BIT);
                out.write((lenToWrite >>> 7) | HIGH_BIT);
                out.write((lenToWrite >>> 14) | HIGH_BIT);
                out.write((lenToWrite >>> 21));
            } else {
                out.write(lenToWrite | HIGH_BIT);
                out.write((lenToWrite >>> 7) | HIGH_BIT);
                out.write((lenToWrite >>> 14) | HIGH_BIT);
                out.write((lenToWrite >>> 21) | HIGH_BIT);
                out.write((lenToWrite >>> 28));
            }

            // write the char data, variable length encoded
            for (int i = 0; i < strlen; i++) {
                int c = cs.charAt(i);

                // manual loop unroll, as it performs much better on jdk8
                if (c < HIGH_BIT) {
                    out.write(c);
                } else if (c < HIGH_BIT14) {
                    out.write(c | HIGH_BIT);
                    out.write((c >>> 7));
                } else {
                    out.write(c | HIGH_BIT);
                    out.write((c >>> 7) | HIGH_BIT);
                    out.write((c >>> 14));
                }
            }
        } else {
            out.write(0);
        }
    }
}
