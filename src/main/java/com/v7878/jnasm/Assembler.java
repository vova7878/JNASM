package com.v7878.jnasm;

public abstract class Assembler {
    private final AssemblerBuffer buffer_;

    public Assembler() {
        this.buffer_ = new AssemblerBuffer();
    }

    public byte[] finalizeCode() {
        return buffer_.finalizeInstructions();
    }

    public abstract void bind(Label label);

    public abstract void jump(Label label);

    protected void emitFixup(AssemblerFixup fixup) {
        buffer_.emitFixup(fixup);
    }

    protected int size() {
        return buffer_.size();
    }

    protected void emit8(int value) {
        buffer_.emit8((byte) value);
    }

    protected void emit16(int value) {
        buffer_.emit16((short) value);
    }

    protected void emit32(int value) {
        buffer_.emit32(value);
    }

    protected void emit64(long value) {
        buffer_.emit64(value);
    }

    protected void store8(int position, int value) {
        buffer_.store8(position, value);
    }

    protected void store16(int position, int value) {
        buffer_.store16(position, value);
    }

    protected void store32(int position, int value) {
        buffer_.store32(position, value);
    }

    protected void store64(int position, long value) {
        buffer_.store64(position, value);
    }

    protected byte load8(int position) {
        return buffer_.load8(position);
    }

    protected int loadU8(int position) {
        return buffer_.loadU8(position);
    }

    protected short load16(int position) {
        return buffer_.load16(position);
    }

    protected int loadU16(int position) {
        return buffer_.loadU16(position);
    }

    protected int load32(int position) {
        return buffer_.load32(position);
    }

    protected long load64(int position) {
        return buffer_.load64(position);
    }
}