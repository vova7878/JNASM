package com.v7878.jnasm.x86;

import java.util.Objects;

public record X86ExternalLabel(String name, int address) {
    public X86ExternalLabel(String name, int address) {
        this.name = Objects.requireNonNull(name);
        this.address = address;
    }
}
