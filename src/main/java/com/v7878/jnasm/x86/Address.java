package com.v7878.jnasm.x86;

import static com.v7878.jnasm.ScaleFactor.TIMES_1;
import static com.v7878.jnasm.x86.CpuRegister.EBP;
import static com.v7878.jnasm.x86.CpuRegister.ESP;

import com.v7878.jnasm.AssemblerFixup;
import com.v7878.jnasm.ScaleFactor;

public class Address extends Operand {
    public Address() {
    }

    public Address(CpuRegister base, int disp) {
        init(base, disp);
    }

    public Address(CpuRegister base, int disp, AssemblerFixup fixup) {
        init(base, disp);
        setFixup(fixup);
    }

    public Address(CpuRegister index, ScaleFactor scale, int disp) {
        setModRM(0, ESP);
        setSIB(scale, index, EBP);
        setDisp32(disp);
    }

    public Address(CpuRegister base, CpuRegister index, ScaleFactor scale, int disp) {
        init(base, index, scale, disp);
    }

    public Address(CpuRegister base, CpuRegister index, ScaleFactor scale, int disp, AssemblerFixup fixup) {
        init(base, index, scale, disp);
        setFixup(fixup);
    }

    private void init(CpuRegister base_in, int disp) {
        if (disp == 0 && base_in != EBP) {
            setModRM(0, base_in);
            if (base_in == ESP) setSIB(TIMES_1, ESP, base_in);
        } else if (disp >= -128 && disp <= 127) {
            setModRM(1, base_in);
            if (base_in == ESP) setSIB(TIMES_1, ESP, base_in);
            setDisp8((byte) disp);
        } else {
            setModRM(2, base_in);
            if (base_in == ESP) setSIB(TIMES_1, ESP, base_in);
            setDisp32(disp);
        }
    }

    private void init(CpuRegister base_in, CpuRegister index_in, ScaleFactor scale_in, int disp) {
        // TODO
        assert index_in != ESP;  // Illegal addressing mode.
        if (disp == 0 && base_in != EBP) {
            setModRM(0, ESP);
            setSIB(scale_in, index_in, base_in);
        } else if (disp >= -128 && disp <= 127) {
            setModRM(1, ESP);
            setSIB(scale_in, index_in, base_in);
            setDisp8((byte) disp);
        } else {
            setModRM(2, ESP);
            setSIB(scale_in, index_in, base_in);
            setDisp32(disp);
        }
    }

    public static Address displace(Address addr, int disp) {
        int newDisp = addr.disp() + disp;
        boolean sib = addr.rm() == ESP;
        boolean ebp = EBP == (sib ? addr.base() : addr.rm());

        Address newAddr = new Address();
        if (addr.mod() == 0 && ebp) {
            newAddr.setModRM(0, addr.rm());
            if (sib) {
                newAddr.setSIB(addr.scale(), addr.index(), addr.base());
            }
            newAddr.setDisp32(newDisp);
        } else if (newDisp == 0 && !ebp) {
            newAddr.setModRM(0, addr.rm());
            if (sib) {
                newAddr.setSIB(addr.scale(), addr.index(), addr.base());
            }
        } else if (-128 <= newDisp && newDisp <= 127) {
            newAddr.setModRM(1, addr.rm());
            if (sib) {
                newAddr.setSIB(addr.scale(), addr.index(), addr.base());
            }
            newAddr.setDisp8((byte) newDisp);
        } else {
            newAddr.setModRM(2, addr.rm());
            if (sib) {
                newAddr.setSIB(addr.scale(), addr.index(), addr.base());
            }
            newAddr.setDisp32(newDisp);
        }
        newAddr.setFixup(addr.getFixup());
        return newAddr;
    }

    public CpuRegister getBaseRegister() {
        return rm() == ESP ? base() : rm();
    }

    public static Address absolute(int addr) {
        Address result = new Address();
        result.setModRM(0, EBP);
        result.setDisp32(addr);
        return result;
    }

    @Override
    public String toString() {
        return switch (mod()) {
            case 0 -> {
                if (rm() != ESP || index() == ESP) {
                    yield "(%%%s)".formatted(rm());
                } else if (base() == EBP) {
                    yield "%d(,%%%s,%d)".formatted(disp32(), index(), 1 << scale().getValue());
                }
                yield "(%%%s,%%%s,%d)".formatted(base(), index(), 1 << scale().getValue());
            }
            case 1 -> {
                if (rm() != ESP || index() == ESP) {
                    yield "%s(%%%s)".formatted(disp8(), rm());
                }
                yield "%s(%%%s,%%%s,%d)".formatted(disp8(), base(), index(), 1 << scale().getValue());
            }
            case 2 -> {
                if (rm() != ESP || index() == ESP) {
                    yield "%d(%%%s)".formatted(disp32(), rm());
                }
                yield "%d(%%%s,%%%s,%d)".formatted(disp32(), base(), index(), 1 << scale().getValue());
            }
            default -> "<address?>";
        };
    }
}
