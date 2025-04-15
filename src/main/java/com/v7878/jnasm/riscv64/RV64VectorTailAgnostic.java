package com.v7878.jnasm.riscv64;

public enum RV64VectorTailAgnostic {
    kUndisturbed(0),
    kAgnostic(1);

    private final int value;

    RV64VectorTailAgnostic(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
