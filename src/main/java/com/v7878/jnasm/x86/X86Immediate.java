package com.v7878.jnasm.x86;

import com.v7878.jnasm.Utils;

public record X86Immediate(int value) {
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
}
