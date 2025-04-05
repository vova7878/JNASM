package com.v7878.jnasm.x86;

import static com.v7878.jnasm.x86.CpuRegister.EBP;
import static com.v7878.jnasm.x86.CpuRegister.ESP;

import com.v7878.jnasm.AssemblerFixup;
import com.v7878.jnasm.ScaleFactor;

import java.util.Objects;

public class Operand {
    protected int length;
    protected AssemblerFixup fixup;
    protected final byte[] encoding;

    protected Operand() {
        this.length = 0;
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
        return CpuRegister.of(encodingAt(0) & 7);
    }

    ScaleFactor scale() {
        return ScaleFactor.of((encodingAt(1) >> 6) & 3);
    }

    CpuRegister index() {
        return CpuRegister.of((encodingAt(1) >> 3) & 7);
    }

    CpuRegister base() {
        return CpuRegister.of(encodingAt(1) & 7);
    }

    int disp() {
        return switch (mod()) {
            // With mod 00b RBP is special and means disp32 (either in r/m or in SIB base).
            case 0 -> (rm() == EBP || (rm() == ESP && base() == EBP)) ? disp32() : 0;
            case 1 -> disp8();
            case 2 -> disp32();
            // Mod 11b means reg/reg, so there is no address and consequently no displacement.
            default -> throw new IllegalStateException(
                    "There is no displacement in x86_64 reg/reg operand");
        };
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
                (encodingAt(0) & 0x07) == reg.index(); // CpuRegister codes match.
    }

    protected byte encodingAt(int index) {
        Objects.checkIndex(index, length);
        return encoding[index];
    }

    protected void setModRM(int mod_in, CpuRegister rm) {
        assert (mod_in & ~3) == 0;
        encoding[0] = (byte) ((mod_in << 6) | rm.index());
        length = 1;
    }

    protected void setSIB(ScaleFactor scale, CpuRegister index, CpuRegister base) {
        assert length == 1;
        encoding[1] = (byte) ((scale.index() << 6) |
                (index.index() << 3) | base.index());
        length = 2;
    }

    protected void setDisp8(byte disp) {
        assert length == 1 || length == 2;
        encoding[length++] = disp;
    }

    protected void setDisp32(int disp) {
        assert length == 1 || length == 2;
        // little-endian
        encoding[length++] = (byte) disp;
        encoding[length++] = (byte) (disp >> 8);
        encoding[length++] = (byte) (disp >> 16);
        encoding[length++] = (byte) (disp >> 24);
    }
}