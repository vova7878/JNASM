package com.v7878.jnasm.riscv64;

// Vector mask
public enum RV64VM {
    kV0_t(0),
    kUnmasked(1);

    private final int value;

    RV64VM(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
