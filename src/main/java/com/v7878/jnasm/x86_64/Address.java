package com.v7878.jnasm.x86_64;

import static com.v7878.jnasm.ScaleFactor.TIMES_1;
import static com.v7878.jnasm.x86_64.CpuRegister.RBP;
import static com.v7878.jnasm.x86_64.CpuRegister.RSP;

import com.v7878.jnasm.AssemblerFixup;
import com.v7878.jnasm.ScaleFactor;
import com.v7878.jnasm.Utils;

public class Address extends Operand {
    private Address() {
    }

    public Address(CpuRegister base, int disp) {
        if (disp == 0 && base.lowReg() != RBP) {
            setModRM(0, base);
            if (base.lowReg() == RSP) {
                setSIB(TIMES_1, RSP, base);
            }
        } else if (Utils.isInt8(disp)) {
            setModRM(1, base);
            if (base.lowReg() == RSP) {
                setSIB(TIMES_1, RSP, base);
            }
            setDisp8((byte) disp);
        } else {
            setModRM(2, base);
            if (base.lowReg() == RSP) {
                setSIB(TIMES_1, RSP, base);
            }
            setDisp32(disp);
        }
    }

    public Address(CpuRegister index, ScaleFactor scale, int disp) {
        if (index == RSP) {
            throw new IllegalArgumentException("%s in not allowed as index".formatted(index));
        }
        setModRM(0, RSP);
        setSIB(scale, index, RBP);
        setDisp32(disp);
    }

    public Address(CpuRegister base, CpuRegister index, ScaleFactor scale, int disp) {
        if (index == RSP) {
            throw new IllegalArgumentException("%s in not allowed as index".formatted(index));
        }
        if (disp == 0 && base.lowReg() != RBP) {
            setModRM(0, RSP);
            setSIB(scale, index, base);
        } else if (Utils.isInt8(disp)) {
            setModRM(1, RSP);
            setSIB(scale, index, base);
            setDisp8((byte) disp);
        } else {
            setModRM(2, RSP);
            setSIB(scale, index, base);
            setDisp32(disp);
        }
    }

    // If no_rip is true then the Absolute address isn't RIP relative.
    public static Address absolute(int addr, boolean no_rip) {
        Address result = new Address();
        if (no_rip) {
            result.setModRM(0, RSP);
            result.setSIB(TIMES_1, RSP, RBP);
            result.setDisp32(addr);
        } else {
            // RIP addressing is done using RBP as the base register.
            // The value in RBP isn't used. Instead the offset is added to RIP.
            result.setModRM(0, RBP);
            result.setDisp32(addr);
        }
        return result;
    }

    // An RIP relative address that will be fixed up later.
    public static Address RIP(AssemblerFixup fixup) {
        Address result = new Address();
        // RIP addressing is done using RBP as the base register.
        // The value in RBP isn't used. Instead the offset is added to RIP.
        result.setModRM(0, RBP);
        result.setDisp32(0);
        result.setFixup(fixup);
        return result;
    }

    // Break the address into pieces and reassemble it again with a new displacement.
    // Note that it may require a new addressing mode if displacement size is changed.
    public static Address displace(Address addr, int disp) {
        int newDisp = addr.disp() + disp;
        boolean sib = addr.lowRM() == RSP;
        boolean rbp = RBP == (sib ? addr.lowBase() : addr.lowRM());

        Address newAddr = new Address();
        if (addr.mod() == 0 && rbp) {
            // Special case: mod 00b and RBP in r/m or SIB base => 32-bit displacement.
            // This case includes RIP-relative addressing.
            newAddr.setModRM(0, addr.rm());
            if (sib) {
                newAddr.setSIB(addr.scale(), addr.index(), addr.base());
            }
            newAddr.setDisp32(newDisp);
        } else if (newDisp == 0 && !rbp) {
            // Mod 00b (excluding a special case for RBP) => no displacement.
            newAddr.setModRM(0, addr.rm());
            if (sib) {
                newAddr.setSIB(addr.scale(), addr.index(), addr.base());
            }
        } else if (Utils.isInt8(newDisp)) {
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

    @Override
    public String toString() {
        return switch (mod()) {
            case 0 -> {
                if ((lowRM() == RSP ? lowBase() : lowRM()) == RBP) {
                    if (lowRM() == RSP) {
                        yield "%d(,%%%s,%d)".formatted(disp32(), index(), scale().value());
                    }
                    yield "%d".formatted(disp32());
                }
                if (lowRM() != RSP || index() == RSP) {
                    yield "(%%%s)".formatted(rm());
                }
                yield "(%%%s,%%%s,%d)".formatted(base(), index(), scale().value());
            }
            case 1 -> {
                if (lowRM() != RSP || index() == RSP) {
                    yield "%s(%%%s)".formatted(disp8(), rm());
                }
                yield "%s(%%%s,%%%s,%d)".formatted(disp8(), base(), index(), scale().value());
            }
            case 2 -> {
                if (lowRM() != RSP || index() == RSP) {
                    yield "%d(%%%s)".formatted(disp32(), rm());
                }
                yield "%d(%%%s,%%%s,%d)".formatted(disp32(), base(), index(), scale().value());
            }
            // TODO?
            default -> "<address?>";
        };
    }
}
