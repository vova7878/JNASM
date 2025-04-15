package com.v7878.jnasm.riscv64;

public class RV64Literal {
    private final RV64Label label;
    private final long value;
    private final boolean is_32_bit;

    RV64Literal(long value, boolean is_32_bit) {
        this.value = value;
        this.is_32_bit = is_32_bit;
        this.label = new RV64Label();
    }

    public int getSize() {
        return is_32_bit ? 4 : 8;
    }

    public long getValue() {
        return value;
    }

    public RV64Label getLabel() {
        return label;
    }
}
