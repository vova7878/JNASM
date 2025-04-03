package com.v7878.jnasm.x86;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConstantArea {
    private final List<Integer> buffer_;
    private static final int ELEM_SIZE = Integer.BYTES;

    public ConstantArea() {
        this.buffer_ = new ArrayList<>();
    }

    public int addDouble(double v) {
        return addInt64(Double.doubleToRawLongBits(v));
    }

    public int addFloat(float v) {
        return addInt32(Float.floatToRawIntBits(v));
    }

    public int addInt32(int v) {
        for (int i = 0; i < buffer_.size(); i++) {
            if (v == buffer_.get(i)) {
                return i * ELEM_SIZE;
            }
        }

        // Didn't match anything.
        int result = buffer_.size() * ELEM_SIZE;
        buffer_.add(v);
        return result;
    }

    public int addInt64(long v) {
        int low = (int) v;
        int high = (int) (v >> 32);
        if (buffer_.size() > 1) {
            for (int i = 0; i < buffer_.size() - 1; i++) {
                if (low == buffer_.get(i) && high == buffer_.get(i + 1)) {
                    return i * ELEM_SIZE;
                }
            }
        }

        // Didn't match anything.
        int result = buffer_.size() * ELEM_SIZE;
        buffer_.addAll(Arrays.asList(low, high));
        return result;
    }

    public boolean isEmpty() {
        return buffer_.isEmpty();
    }

    public int getSize() {
        return buffer_.size() * ELEM_SIZE;
    }

    public List<Integer> getBuffer() {
        return buffer_;
    }
}
