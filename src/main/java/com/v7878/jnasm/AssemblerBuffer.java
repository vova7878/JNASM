package com.v7878.jnasm;

import java.util.Objects;

final class AssemblerBuffer implements CodeBuffer {
    private record AssemblerFixupContainer(
            AssemblerFixupContainer previous,
            AssemblerFixup impl, int position
    ) {
    }

    private AssemblerFixupContainer fixup_;

    public AssemblerBuffer() {
        this.fixup_ = null;
    }

    public void processFixups() {
        var fixup = fixup_;
        fixup_ = null;
        while (fixup != null) {
            fixup.impl().process(this, fixup.position());
            fixup = fixup.previous();
        }
    }

    public byte[] finalizeInstructions() {
        processFixups();
        // TODO
    }

    public int size() {
        // TODO
    }

    public void emitFixup(AssemblerFixup fixup) {
        fixup_ = new AssemblerFixupContainer(fixup_, fixup, size());
    }

    public void move(int new_position, int old_position, int size) {
        // TODO
    }

    public void emit8(int value) {
        // TODO
    }

    public void emit16(int value) {
        // TODO
    }

    public void emit32(int value) {
        // TODO
    }

    public void emit64(long value) {
        // TODO
    }

    public void store8(int position, int value) {
        Objects.checkFromIndexSize(position, 1, size());
        // TODO
    }

    public void store16(int position, int value) {
        Objects.checkFromIndexSize(position, 2, size());
        // TODO
    }

    public void store32(int position, int value) {
        Objects.checkFromIndexSize(position, 4, size());
        // TODO
    }

    public void store64(int position, long value) {
        Objects.checkFromIndexSize(position, 8, size());
        // TODO
    }

    public byte load8(int position) {
        Objects.checkFromIndexSize(position, 1, size());
        // TODO
    }

    public short load16(int position) {
        Objects.checkFromIndexSize(position, 2, size());
        // TODO
    }

    public int load32(int position) {
        Objects.checkFromIndexSize(position, 4, size());
        // TODO
    }

    public long load64(int position) {
        Objects.checkFromIndexSize(position, 8, size());
        // TODO
    }
}
