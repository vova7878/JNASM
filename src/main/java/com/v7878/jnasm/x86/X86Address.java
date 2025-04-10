package com.v7878.jnasm.x86;

import static com.v7878.jnasm.ScaleFactor.TIMES_1;
import static com.v7878.jnasm.x86.X86CpuRegister.EBP;
import static com.v7878.jnasm.x86.X86CpuRegister.ESP;

import com.v7878.jnasm.ScaleFactor;
import com.v7878.jnasm.Utils;

public class X86Address extends X86Operand {
    private X86Address() {
    }

    public X86Address(X86CpuRegister base, int disp) {
        if (disp == 0 && base != EBP) {
            setModRM(0, base);
            if (base == ESP) setSIB(TIMES_1, ESP, base);
        } else if (Utils.isInt(8, disp)) {
            setModRM(1, base);
            if (base == ESP) setSIB(TIMES_1, ESP, base);
            setDisp8((byte) disp);
        } else {
            setModRM(2, base);
            if (base == ESP) setSIB(TIMES_1, ESP, base);
            setDisp32(disp);
        }
    }

    public X86Address(X86CpuRegister index, ScaleFactor scale, int disp) {
        if (index == ESP) {
            throw new IllegalArgumentException("%s in not allowed as index".formatted(index));
        }
        setModRM(0, ESP);
        setSIB(scale, index, EBP);
        setDisp32(disp);
    }

    public X86Address(X86CpuRegister base, X86CpuRegister index, ScaleFactor scale, int disp) {
        if (index == ESP) {
            throw new IllegalArgumentException("%s in not allowed as index".formatted(index));
        }
        if (disp == 0 && base != EBP) {
            setModRM(0, ESP);
            setSIB(scale, index, base);
        } else if (Utils.isInt(8, disp)) {
            setModRM(1, ESP);
            setSIB(scale, index, base);
            setDisp8((byte) disp);
        } else {
            setModRM(2, ESP);
            setSIB(scale, index, base);
            setDisp32(disp);
        }
    }

    // Break the address into pieces and reassemble it again with a new displacement.
    // Note that it may require a new addressing mode if displacement size is changed.
    public static X86Address displace(X86Address addr, int disp) {
        int newDisp = addr.disp() + disp;
        boolean sib = addr.rm() == ESP;
        boolean ebp = EBP == (sib ? addr.base() : addr.rm());

        X86Address newAddr = new X86Address();
        if (addr.mod() == 0 && ebp) {
            // Special case: mod 00b and EBP in r/m or SIB base => 32-bit displacement.
            newAddr.setModRM(0, addr.rm());
            if (sib) {
                newAddr.setSIB(addr.scale(), addr.index(), addr.base());
            }
            newAddr.setDisp32(newDisp);
        } else if (newDisp == 0 && !ebp) {
            // Mod 00b (excluding a special case for EBP) => no displacement.
            newAddr.setModRM(0, addr.rm());
            if (sib) {
                newAddr.setSIB(addr.scale(), addr.index(), addr.base());
            }
        } else if (Utils.isInt(8, newDisp)) {
            // Mod 01b => 8-bit displacement.
            newAddr.setModRM(1, addr.rm());
            if (sib) {
                newAddr.setSIB(addr.scale(), addr.index(), addr.base());
            }
            newAddr.setDisp8((byte) newDisp);
        } else {
            // Mod 10b => 32-bit displacement.
            newAddr.setModRM(2, addr.rm());
            if (sib) {
                newAddr.setSIB(addr.scale(), addr.index(), addr.base());
            }
            newAddr.setDisp32(newDisp);
        }
        newAddr.setFixup(addr.getFixup());
        return newAddr;
    }

    public static X86Address absolute(int addr) {
        X86Address result = new X86Address();
        result.setModRM(0, EBP);
        result.setDisp32(addr);
        return result;
    }

    @Override
    public String toString() {
        return switch (mod()) {
            case 0 -> {
                var rm = rm();
                if ((rm == ESP ? base() : rm) == EBP) {
                    if (rm == ESP) {
                        yield "%d(,%%%s,%d)".formatted(disp32(), index(), scale().value());
                    }
                    yield "%d".formatted(disp32());
                }
                if (rm != ESP || index() == ESP) {
                    yield "(%%%s)".formatted(rm);
                }
                yield "(%%%s,%%%s,%d)".formatted(base(), index(), scale().value());
            }
            case 1 -> {
                if (rm() != ESP || index() == ESP) {
                    yield "%s(%%%s)".formatted(disp8(), rm());
                }
                yield "%s(%%%s,%%%s,%d)".formatted(disp8(), base(), index(), scale().value());
            }
            case 2 -> {
                if (rm() != ESP || index() == ESP) {
                    yield "%d(%%%s)".formatted(disp32(), rm());
                }
                yield "%d(%%%s,%%%s,%d)".formatted(disp32(), base(), index(), scale().value());
            }
            // TODO?
            default -> "<address?>";
        };
    }
}
