package com.v7878.jnasm.x86_64;

import static com.v7878.jnasm.x86_64.CpuRegister.RBP;
import static com.v7878.jnasm.x86_64.CpuRegister.RSP;

import com.v7878.jnasm.AssemblerFixup;
import com.v7878.jnasm.ScaleFactor;

import java.util.Objects;

public class Operand {
    protected int length;
    protected int rex;
    protected AssemblerFixup fixup;
    protected final byte[] encoding;

    protected Operand() {
        this.length = 0;
        this.rex = 0;
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

    int rex() {
        return rex;
    }

    int mod() {
        return (encodingAt(0) >> 6) & 3;
    }

    ScaleFactor scale() {
        return ScaleFactor.of((encodingAt(1) >> 6) & 3);
    }

    private int rawRM() {
        return encodingAt(0) & 7;
    }

    private int rawIndex() {
        return (encodingAt(1) >> 3) & 7;
    }

    private int rawBase() {
        return encodingAt(1) & 7;
    }

    CpuRegister lowRM() {
        return CpuRegister.of(rawRM());
    }

    CpuRegister lowBase() {
        return CpuRegister.of(rawBase());
    }

    CpuRegister rm() {
        int ext = (rex & 0x1) != 0 ? 8 : 0;
        return CpuRegister.of(rawRM() + ext);
    }

    CpuRegister index() {
        int ext = (rex & 0x2) != 0 ? 8 : 0;
        return CpuRegister.of(rawIndex() + ext);
    }

    CpuRegister base() {
        int ext = (rex & 0x1) != 0 ? 8 : 0;
        return CpuRegister.of(rawBase() + ext);
    }

    int disp() {
        return switch (mod()) {
            // With mod 00b RBP is special and means disp32 (either in r/m or in SIB base).
            case 0 -> (lowRM() == RBP || (lowRM() == RSP && lowBase() == RBP)) ? disp32() : 0;
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
        return ((encodingAt(0) & 0xF8) == 0xC0)  // Addressing mode is register only.
                && ((encodingAt(0) & 0x07) == reg.lowBits())  // Register codes match.
                && (reg.needsRex() == ((rex & 1) != 0));  // REX.000B bits match.
    }

    protected byte encodingAt(int index) {
        Objects.checkIndex(index, length);
        return encoding[index];
    }

    protected void setModRM(int mod_in, CpuRegister rm_in) {
        assert (mod_in & ~3) == 0;
        if (rm_in.needsRex()) {
            rex |= 0x41;  // REX.000B
        }
        encoding[0] = (byte) ((mod_in << 6) | rm_in.lowBits());
        length = 1;
    }

    protected void setSIB(ScaleFactor scale, CpuRegister index, CpuRegister base) {
        assert length == 1;
        if (base.needsRex()) {
            rex |= 0x41;  // REX.000B
        }
        if (index.needsRex()) {
            rex |= 0x42;  // REX.00X0
        }
        encoding[1] = (byte) ((scale.index() << 6) |
                (index.lowBits() << 3) | base.lowBits());
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
