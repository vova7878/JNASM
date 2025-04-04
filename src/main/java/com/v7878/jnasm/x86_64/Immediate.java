package com.v7878.jnasm.x86_64;

import com.v7878.jnasm.Utils;

public record Immediate(long value) {
    public boolean isInt8() {
        return Utils.isInt8(value);
    }

    public boolean isUInt8() {
        return Utils.isUInt8(value);
    }

    public boolean isInt16() {
        return Utils.isInt16(value);
    }

    public boolean isUInt16() {
        return Utils.isUInt16(value);
    }

    public boolean isInt32() {
        return Utils.isInt32(value);
    }

    public boolean isUInt32() {
        return Utils.isUInt32(value);
    }
}
