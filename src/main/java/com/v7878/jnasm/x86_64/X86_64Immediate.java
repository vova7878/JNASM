package com.v7878.jnasm.x86_64;

import com.v7878.jnasm.Utils;

public record X86_64Immediate(long value) {
    public boolean isInt8() {
        return Utils.isLInt(8, value);
    }

    public boolean isUInt8() {
        return Utils.isLUInt(8, value);
    }

    public boolean isInt16() {
        return Utils.isLInt(16, value);
    }

    public boolean isUInt16() {
        return Utils.isLUInt(16, value);
    }

    public boolean isInt32() {
        return Utils.isLInt(32, value);
    }

    public boolean isUInt32() {
        return Utils.isLUInt(32, value);
    }
}
