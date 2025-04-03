package com.v7878.jnasm;

@FunctionalInterface
public interface AssemblerFixup {
    void process(CodeBuffer buffer, int position);
}
