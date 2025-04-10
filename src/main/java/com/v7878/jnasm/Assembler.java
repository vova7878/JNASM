package com.v7878.jnasm;

public abstract class Assembler {
    private final AssemblerBuffer buffer;

    public Assembler() {
        this.buffer = new AssemblerBuffer();
    }

    public void finalizeCode() {
        buffer.finalizeCode();
    }

    public byte[] getCode() {
        return buffer.getCode();
    }

    public abstract void bind(Label label);

    public abstract void jump(Label label);

    public CodeBuffer getBuffer() {
        return buffer;
    }

    // Methods to make implementation easier

    protected void emitFixup(AssemblerFixup fixup) {
        buffer.emitFixup(fixup);
    }

    protected int size() {
        return buffer.size();
    }

    protected void emit8(int value) {
        buffer.emit8(value);
    }

    protected void emit16(int value) {
        buffer.emit16(value);
    }

    protected void emit32(int value) {
        buffer.emit32(value);
    }

    protected void emit64(long value) {
        buffer.emit64(value);
    }

    protected void store8(int position, int value) {
        buffer.store8(position, value);
    }

    protected void store16(int position, int value) {
        buffer.store16(position, value);
    }

    protected void store32(int position, int value) {
        buffer.store32(position, value);
    }

    protected void store64(int position, long value) {
        buffer.store64(position, value);
    }

    protected byte load8(int position) {
        return buffer.load8(position);
    }

    protected int loadU8(int position) {
        return buffer.loadU8(position);
    }

    protected short load16(int position) {
        return buffer.load16(position);
    }

    protected int loadU16(int position) {
        return buffer.loadU16(position);
    }

    protected int load32(int position) {
        return buffer.load32(position);
    }

    protected long load64(int position) {
        return buffer.load64(position);
    }
}