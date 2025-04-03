package com.v7878.jnasm;

import java.util.Objects;

public record ExternalLabel(String name, long address) {
    public ExternalLabel(String name, long address) {
        this.name = Objects.requireNonNull(name);
        this.address = address;
    }
}
