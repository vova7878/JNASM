package com.v7878.jnasm.x86;

import com.v7878.jnasm.AssemblerFixup;
import com.v7878.jnasm.ScaleFactor;

import java.util.Objects;

public class Operand {
    protected int length;
    protected int disp;
    protected AssemblerFixup fixup;
    protected final byte[] encoding;

    protected Operand() {
        this.length = 0;
        this.disp = 0;
        this.fixup = null;
        this.encoding = new byte[6];
    }

    Operand(CpuRegister reg) {
        this();
        setModRM(3, reg);
    }

    public void setFixup(AssemblerFixup fixup) {
        this.fixup = fixup;
    }

    public AssemblerFixup getFixup() {
        return fixup;
    }

    int mod() {
        return (encodingAt(0) >> 6) & 3;
    }

    CpuRegister rm() {
        return CpuRegister.values()[encodingAt(0) & 7];
    }

    ScaleFactor scale() {
        return ScaleFactor.values()[(encodingAt(1) >> 6) & 3];
    }

    CpuRegister index() {
        return CpuRegister.values()[(encodingAt(1) >> 3) & 7];
    }

    CpuRegister base() {
        return CpuRegister.values()[encodingAt(1) & 7];
    }

    int disp() {
        return disp;
    }

    byte disp8() {
        assert length >= 2;
        return encodingAt(length - 1);
    }

    int disp32() {
        assert length >= 5;
        // little-endian
        return (encodingAt(length - 1) & 0xff) << 24 |
                (encodingAt(length - 2) & 0xff) << 16 |
                (encodingAt(length - 3) & 0xff) << 8 |
                (encodingAt(length - 4) & 0xff);
    }

    boolean isRegister(CpuRegister reg) {
        return (encodingAt(0) & 0xF8) == 0xC0 &&  // Addressing mode is register only.
                (encodingAt(0) & 0x07) == reg.ordinal(); // CpuRegister codes match.
    }

    protected byte encodingAt(int index) {
        Objects.checkIndex(index, length);
        return encoding[index];
    }

    protected void setModRM(int mod_in, CpuRegister rm_in) {
        assert (mod_in & ~3) == 0;
        encoding[0] = (byte) ((mod_in << 6) | rm_in.ordinal());
        length = 1;
    }

    protected void setSIB(ScaleFactor scale_in, CpuRegister index_in, CpuRegister base_in) {
        assert length == 1;
        encoding[1] = (byte) ((scale_in.getValue() << 6) |
                (index_in.getValue() << 3) | base_in.getValue());
        length = 2;
    }

    protected void setDisp8(byte disp) {
        assert length == 1 || length == 2;
        encoding[length++] = disp;
        this.disp = disp;
    }

    protected void setDisp32(int disp) {
        assert length == 1 || length == 2;
        // little-endian
        encoding[length++] = (byte) disp;
        encoding[length++] = (byte) (disp >> 8);
        encoding[length++] = (byte) (disp >> 16);
        encoding[length++] = (byte) (disp >> 24);
        this.disp = disp;
    }
}