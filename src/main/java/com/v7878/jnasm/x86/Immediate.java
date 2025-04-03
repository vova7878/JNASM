package com.v7878.jnasm.x86;

import com.v7878.jnasm.Utils;

public class Immediate {
    private final int value_;

    public Immediate(int value) {
        this.value_ = value;
    }

    public int value() {
        return value_;
    }

    public boolean isInt8() {
        return Utils.isInt8(value_);
    }

    public boolean isUInt8() {
        return Utils.isUInt8(value_);
    }

    public boolean isInt16() {
        return Utils.isInt16(value_);
    }

    public boolean isUInt16() {
        return Utils.isUInt16(value_);
    }

    public boolean isInt32() {
        return true; // Immediate is always 32-bit
    }
}
