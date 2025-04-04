package com.v7878.jnasm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConstantArea {
    // TODO: something better to store ints
    private final List<Integer> buffer;
    private static final int ELEM_SIZE = Integer.BYTES;

    public ConstantArea() {
        this.buffer = new ArrayList<>();
    }

    // Add a double to the constant area, returning the offset into
    // the constant area where the literal resides.
    public int addDouble(double v) {
        return addInt64(Double.doubleToRawLongBits(v));
    }

    // Add a float to the constant area, returning the offset into
    // the constant area where the literal resides.
    public int addFloat(float v) {
        return addInt32(Float.floatToRawIntBits(v));
    }

    // Add an int32_t to the end of the constant area, returning the offset into
    // the constant area where the literal resides.
    public int addInt32(int v) {
        for (int i = 0; i < buffer.size(); i++) {
            if (v == buffer.get(i)) {
                return i * ELEM_SIZE;
            }
        }

        // Didn't match anything.
        int result = buffer.size() * ELEM_SIZE;
        buffer.add(v);
        return result;
    }

    // Add an int64_t to the constant area, returning the offset into
    // the constant area where the literal resides.
    public int addInt64(long v) {
        int low = (int) v;
        int high = (int) (v >> 32);
        if (buffer.size() > 1) {
            for (int i = 0; i < buffer.size() - 1; i++) {
                if (low == buffer.get(i) && high == buffer.get(i + 1)) {
                    return i * ELEM_SIZE;
                }
            }
        }

        // Didn't match anything.
        int result = buffer.size() * ELEM_SIZE;
        buffer.addAll(Arrays.asList(low, high));
        return result;
    }

    public boolean isEmpty() {
        return buffer.isEmpty();
    }

    public int getSize() {
        return buffer.size() * ELEM_SIZE;
    }

    public List<Integer> getBuffer() {
        return buffer;
    }
}
