package com.v7878.jnasm.riscv64;

public enum FPRoundingMode {
    kRNE(0x0),  // Round to Nearest, ties to Even
    kRTZ(0x1),  // Round towards Zero
    kRDN(0x2),  // Round Down (towards −Infinity)
    kRUP(0x3),  // Round Up (towards +Infinity)
    kRMM(0x4),  // Round to Nearest, ties to Max Magnitude
    kDYN(0x7);  // Dynamic rounding mode

    public static final FPRoundingMode kDefault = kDYN;
    // Some instructions never need to round even though the spec includes the RM field.
    // To simplify testing, emit the RM as 0 by default for these instructions because that's what
    // `clang` does and because the `llvm-objdump` fails to disassemble the other rounding modes.
    public static final FPRoundingMode kIgnored = kRNE;

    private final int value;

    FPRoundingMode(int value) {
        this.value = value;
    }

    public int index() {
        return value;
    }
}
