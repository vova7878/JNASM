package com.v7878.jnasm.riscv64;

public class Literal {
    private static final int kMaxSize = 8;

    private final Riscv64Label label;
    private final byte[] data;

    public Literal(byte[] data) {
        if (data.length > kMaxSize) {
            throw new IllegalArgumentException("data exceeds maximum size");
        }
        this.data = data;
        this.label = new Riscv64Label();
    }

    // TODO: of(int), of(long), of(float), of(double)

    public int getSize() {
        return data.length;
    }

    public byte[] getData() {
        return data;
    }

    public Riscv64Label getLabel() {
        return label;
    }
}
