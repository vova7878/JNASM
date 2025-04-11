package com.v7878.jnasm.riscv64;

public class Literal {
    private final Riscv64Label label;
    private final long value;
    private final boolean is_32_bit;

    Literal(long value, boolean is_32_bit) {
        this.value = value;
        this.is_32_bit = is_32_bit;
        this.label = new Riscv64Label();
    }

    public int getSize() {
        return is_32_bit ? 4 : 8;
    }

    public long getValue() {
        return value;
    }

    public Riscv64Label getLabel() {
        return label;
    }
}
