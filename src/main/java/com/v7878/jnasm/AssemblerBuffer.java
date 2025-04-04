package com.v7878.jnasm;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

final class AssemblerBuffer implements CodeBuffer {
    private record AssemblerFixupContainer(
            AssemblerFixupContainer previous,
            AssemblerFixup impl, int position
    ) {
    }

    private static final int DEFAULT_CODE_SIZE = 128;

    private AssemblerFixupContainer fixup_;
    private ByteBuffer data;
    // TODO: private boolean finalized;

    public AssemblerBuffer() {
        this.fixup_ = null;
        this.data = newBuffer(DEFAULT_CODE_SIZE);
    }

    private ByteBuffer newBuffer(int size) {
        return ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
    }

    private void ensureSpace() {
        if (data.remaining() < 16) grow();
    }

    private void grow() {
        data = newBuffer(data.capacity() * 2).put(data.flip());
    }

    public void processFixups() {
        var fixup = fixup_;
        fixup_ = null;
        while (fixup != null) {
            fixup.impl().process(this, fixup.position());
            fixup = fixup.previous();
        }
    }

    public void finalizeCode() {
        processFixups();
    }

    public byte[] getCode() {
        byte[] out = new byte[size()];
        int old_pos = data.position();
        data.position(0);
        data.get(out);
        data.position(old_pos);
        return out;
    }

    public int size() {
        return data.position();
    }

    public void emitFixup(AssemblerFixup fixup) {
        fixup_ = new AssemblerFixupContainer(fixup_, fixup, size());
    }

    public void move(int new_position, int old_position, int size) {
        // TODO
        throw new UnsupportedOperationException();
    }

    public void emit8(int value) {
        ensureSpace();
        data.put((byte) value);
    }

    public void emit16(int value) {
        ensureSpace();
        data.putShort((short) value);
    }

    public void emit32(int value) {
        ensureSpace();
        data.putInt(value);
    }

    public void emit64(long value) {
        ensureSpace();
        data.putLong(value);
    }

    public void store8(int position, int value) {
        Objects.checkFromIndexSize(position, 1, size());
        data.put(position, (byte) value);
    }

    public void store16(int position, int value) {
        Objects.checkFromIndexSize(position, 2, size());
        data.putShort(position, (short) value);
    }

    public void store32(int position, int value) {
        Objects.checkFromIndexSize(position, 4, size());
        data.putInt(position, value);
    }

    public void store64(int position, long value) {
        Objects.checkFromIndexSize(position, 8, size());
        data.putLong(position, value);
    }

    public byte load8(int position) {
        Objects.checkFromIndexSize(position, 1, size());
        return data.get(position);
    }

    public short load16(int position) {
        Objects.checkFromIndexSize(position, 2, size());
        return data.getShort(position);
    }

    public int load32(int position) {
        Objects.checkFromIndexSize(position, 4, size());
        return data.getInt(position);
    }

    public long load64(int position) {
        Objects.checkFromIndexSize(position, 8, size());
        return data.getLong(position);
    }
}
