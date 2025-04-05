package com.v7878.jnasm.x86_64;

import static com.v7878.jnasm.common_x86.VEXConstants.GET_REX_B;
import static com.v7878.jnasm.common_x86.VEXConstants.GET_REX_X;
import static com.v7878.jnasm.common_x86.VEXConstants.SET_VEX_B;
import static com.v7878.jnasm.common_x86.VEXConstants.SET_VEX_L_128;
import static com.v7878.jnasm.common_x86.VEXConstants.SET_VEX_M_0F;
import static com.v7878.jnasm.common_x86.VEXConstants.SET_VEX_PP_NONE;
import static com.v7878.jnasm.common_x86.VEXConstants.SET_VEX_R;
import static com.v7878.jnasm.common_x86.VEXConstants.SET_VEX_W;
import static com.v7878.jnasm.common_x86.VEXConstants.SET_VEX_X;
import static com.v7878.jnasm.common_x86.VEXConstants.THREE_BYTE_VEX;
import static com.v7878.jnasm.common_x86.VEXConstants.TWO_BYTE_VEX;
import static com.v7878.jnasm.common_x86.VEXConstants.VEX_INIT;
import static com.v7878.jnasm.x86_64.CpuRegister.RAX;
import static com.v7878.jnasm.x86_64.CpuRegister.RCX;

import com.v7878.jnasm.Assembler;
import com.v7878.jnasm.AssemblerFixup;
import com.v7878.jnasm.Label;
import com.v7878.jnasm.Utils;

public abstract class X86_64Assembler extends Assembler implements X86_64AssemblerI {
    private final boolean has_AVX_or_AVX2;

    public X86_64Assembler(boolean has_AVX_or_AVX2) {
        this.has_AVX_or_AVX2 = has_AVX_or_AVX2;
    }

    private static void CHECK(boolean value) {
        if (!value) {
            // TODO: message
            throw new AssertionError();
        }
    }

    private static void CHECK_EQ(int a, int b) {
        CHECK(a == b);
    }

    private static void CHECK_LT(int a, int b) {
        CHECK(a < b);
    }

    @SuppressWarnings("SameParameterValue")
    private static void CHECK_LE(int a, int b) {
        CHECK(a <= b);
    }

    @SuppressWarnings("SameParameterValue")
    private static void CHECK_GT(int a, int b) {
        CHECK(a > b);
    }

    @SuppressWarnings("SameParameterValue")
    private static void CHECK_GE(int a, int b) {
        CHECK(a >= b);
    }

    public boolean cpuHasAVXorAVX2FeatureFlag() {
        return has_AVX_or_AVX2;
    }

    private void EmitUint8(int value) {
        emit8(value);
    }

    private void EmitInt32(int value) {
        emit32(value);
    }

    private void EmitInt64(long value) {
        emit64(value);
    }

    private void EmitRegisterOperand(int rm, int reg) {
        CHECK_GE(rm, 0);
        CHECK_LT(rm, 8);
        emit8((0xC0 | (reg & 7)) + (rm << 3));
    }

    private void EmitXmmRegisterOperand(int rm, XmmRegister reg) {
        EmitRegisterOperand(rm, reg.index());
    }

    private void EmitFixup(AssemblerFixup fixup) {
        emitFixup(fixup);
    }

    private void EmitOperandSizeOverride() {
        emit8(0x66);
    }

    private void EmitOperand(int reg_or_opcode, Operand operand) {
        CHECK_GE(reg_or_opcode, 0);
        CHECK_LT(reg_or_opcode, 8);
        final int length = operand.length;
        CHECK_GT(length, 0);
        // Emit the ModRM byte updated with the given reg value.
        CHECK_EQ(operand.encoding[0] & 0x38, 0);
        emit8(operand.encoding[0] + (reg_or_opcode << 3));
        // Emit the rest of the encoded operand.
        for (int i = 1; i < length; i++) {
            emit8(operand.encoding[i]);
        }
        AssemblerFixup fixup = operand.getFixup();
        if (fixup != null) {
            emitFixup(fixup);
        }
    }

    private void EmitImmediate(Immediate imm, boolean is_16_op) {
        if (is_16_op) {
            emit8((int) imm.value() & 0xFF);
            emit8((int) imm.value() >> 8);
        } else if (imm.isInt32()) {
            emit32((int) imm.value());
        } else {
            EmitInt64(imm.value());
        }
    }

    private void EmitImmediate(Immediate imm) {
        EmitImmediate(imm, false);
    }

    private void EmitComplex(int reg_or_opcode, Operand operand, Immediate immediate, boolean is_16_op) {
        CHECK_GE(reg_or_opcode, 0);
        CHECK_LT(reg_or_opcode, 8);
        if (immediate.isInt8()) {
            // Use sign-extended 8-bit immediate.
            emit8(0x83);
            EmitOperand(reg_or_opcode, operand);
            emit8((int) immediate.value() & 0xFF);
        } else if (operand.isRegister(RAX)) {
            // Use short form if the destination is rax.
            emit8(0x05 + (reg_or_opcode << 3));
            EmitImmediate(immediate, is_16_op);
        } else {
            emit8(0x81);
            EmitOperand(reg_or_opcode, operand);
            EmitImmediate(immediate, is_16_op);
        }
    }

    private void EmitComplex(int rm, Operand operand, Immediate immediate) {
        EmitComplex(rm, operand, immediate, false);
    }

    private void EmitLabel(Label label, int instruction_size) {
        if (label.isBound()) {
            int offset = label.getPosition() - size();
            CHECK_LE(offset, 0);
            emit32(offset - instruction_size);
        } else {
            EmitLabelLink(label);
        }
    }

    private void EmitLabelLink(Label label) {
        CHECK(!label.isBound());
        int position = size();
        emit32(label.position);
        label.linkTo(position);
    }

    private void EmitLabelLink(NearLabel label) {
        CHECK(!label.isBound());
        int position = size();
        if (label.isLinked()) {
            // Save the delta in the byte that we have to play with.
            int delta = position - label.getLinkPosition();
            CHECK(Utils.isUInt8(delta));
            emit8(delta & 0xFF);
        } else {
            emit8(0);
        }
        label.linkTo(position);
    }

    private void EmitGenericShift(boolean wide, int reg_or_opcode,
                                  CpuRegister reg, Immediate imm) {
        CHECK(imm.isInt8());
        if (wide) {
            EmitRex64(reg);
        } else {
            EmitOptionalRex32(reg);
        }
        if (imm.value() == 1) {
            EmitUint8(0xD1);
            EmitOperand(reg_or_opcode, new Operand(reg));
        } else {
            EmitUint8(0xC1);
            EmitOperand(reg_or_opcode, new Operand(reg));
            EmitUint8((int) imm.value() & 0xFF);
        }
    }

    private void EmitGenericShift(boolean wide, int reg_or_opcode,
                                  CpuRegister operand, CpuRegister shifter) {
        CHECK_EQ(shifter.index(), RCX.index());
        if (wide) {
            EmitRex64(operand);
        } else {
            EmitOptionalRex32(operand);
        }
        EmitUint8(0xD3);
        EmitOperand(reg_or_opcode, new Operand(operand));
    }

    private void EmitMovCpuFpu(XmmRegister fp_reg, CpuRegister cpu_reg,
                               boolean is64bit, byte opcode) {
        EmitUint8(0x66);
        EmitOptionalRex(false, is64bit, fp_reg.needsRex(), false, cpu_reg.needsRex());
        EmitUint8(0x0F);
        EmitUint8(opcode);
        EmitOperand(fp_reg.lowBits(), new Operand(cpu_reg));
    }

    private void EmitOptionalRex(boolean force, boolean w, boolean r, boolean x, boolean b) {
        // REX.WRXB
        // W - 64-bit operand
        // R - MODRM.reg
        // X - SIB.index
        // B - MODRM.rm/SIB.base
        int rex = force ? 0x40 : 0;
        if (w) {
            rex |= 0x48;  // REX.W000
        }
        if (r) {
            rex |= 0x44;  // REX.0R00
        }
        if (x) {
            rex |= 0x42;  // REX.00X0
        }
        if (b) {
            rex |= 0x41;  // REX.000B
        }
        if (rex != 0) {
            EmitUint8(rex);
        }
    }

    private void EmitOptionalRex32(CpuRegister reg) {
        EmitOptionalRex(false, false, false, false, reg.needsRex());
    }

    private void EmitOptionalRex32(CpuRegister dst, CpuRegister src) {
        EmitOptionalRex(false, false, dst.needsRex(), false, src.needsRex());
    }

    private void EmitOptionalRex32(XmmRegister dst, XmmRegister src) {
        EmitOptionalRex(false, false, dst.needsRex(), false, src.needsRex());
    }

    private void EmitOptionalRex32(CpuRegister dst, XmmRegister src) {
        EmitOptionalRex(false, false, dst.needsRex(), false, src.needsRex());
    }

    private void EmitOptionalRex32(XmmRegister dst, CpuRegister src) {
        EmitOptionalRex(false, false, dst.needsRex(), false, src.needsRex());
    }

    private void EmitOptionalRex32(Operand operand) {
        int rex = operand.rex();
        if (rex != 0) {
            EmitUint8(rex);
        }
    }

    private void EmitOptionalRex32(CpuRegister dst, Operand operand) {
        int rex = operand.rex();
        if (dst.needsRex()) {
            rex |= 0x44;  // REX.0R00
        }
        if (rex != 0) {
            EmitUint8(rex);
        }
    }

    private void EmitOptionalRex32(XmmRegister dst, Operand operand) {
        int rex = operand.rex();
        if (dst.needsRex()) {
            rex |= 0x44;  // REX.0R00
        }
        if (rex != 0) {
            EmitUint8(rex);
        }
    }

    private void EmitRex64() {
        EmitOptionalRex(false, true, false, false, false);
    }

    private void EmitRex64(CpuRegister reg) {
        EmitOptionalRex(false, true, false, false, reg.needsRex());
    }

    private void EmitRex64(Operand operand) {
        int rex = operand.rex();
        rex |= 0x48;  // REX.W000
        EmitUint8(rex);
    }

    private void EmitRex64(CpuRegister dst, CpuRegister src) {
        EmitOptionalRex(false, true, dst.needsRex(), false, src.needsRex());
    }

    private void EmitRex64(XmmRegister dst, CpuRegister src) {
        EmitOptionalRex(false, true, dst.needsRex(), false, src.needsRex());
    }

    private void EmitRex64(CpuRegister dst, XmmRegister src) {
        EmitOptionalRex(false, true, dst.needsRex(), false, src.needsRex());
    }

    private void EmitRex64(CpuRegister dst, Operand operand) {
        int rex = 0x48 | operand.rex();  // REX.W000
        if (dst.needsRex()) {
            rex |= 0x44;  // REX.0R00
        }
        EmitUint8(rex);
    }

    private void EmitRex64(XmmRegister dst, Operand operand) {
        int rex = 0x48 | operand.rex();  // REX.W000
        if (dst.needsRex()) {
            rex |= 0x44;  // REX.0R00
        }
        EmitUint8(rex);
    }

    private void EmitOptionalByteRegNormalizingRex32(
            CpuRegister dst, CpuRegister src, boolean normalize_both) {
        // SPL, BPL, SIL, DIL need the REX prefix.
        boolean force = src.index() > 3;
        if (normalize_both) {
            // Some instructions take two byte registers, such as `xchg bpl, al`, so they need the REX
            // prefix if either `src` or `dst` needs it.
            force |= dst.index() > 3;
        } else {
            // Other instructions take one byte register and one full register, such as `movzxb rax, bpl`.
            // They need REX prefix only if `src` needs it, but not `dst`.
        }
        EmitOptionalRex(force, false, dst.needsRex(), false, src.needsRex());
    }

    private void EmitOptionalByteRegNormalizingRex32(CpuRegister dst, Operand operand) {
        int rex = operand.rex();
        // For dst, SPL, BPL, SIL, DIL need the rex prefix.
        boolean force = dst.index() > 3;
        if (force) {
            rex |= 0x40;  // REX.0000
        }
        if (dst.needsRex()) {
            rex |= 0x44;  // REX.0R00
        }
        if (rex != 0) {
            EmitUint8(rex);
        }
    }

    private byte EmitVexPrefixByteZero(boolean is_twobyte_form) {
        // Vex Byte 0,
        // Bits [7:0] must contain the value 11000101b (0xC5) for 2-byte Vex
        // Bits [7:0] must contain the value 11000100b (0xC4) for 3-byte Vex
        int vex_prefix = 0xC0;
        if (is_twobyte_form) {
            vex_prefix |= TWO_BYTE_VEX;  // 2-Byte Vex
        } else {
            vex_prefix |= THREE_BYTE_VEX;  // 3-Byte Vex
        }
        return (byte) vex_prefix;
    }

    private void EmitVexPrefixForAddress(Address addr, boolean r, int vex_l, int vex_pp) {
        int rex = addr.rex();
        boolean rex_x = (rex & GET_REX_X) != 0;
        boolean rex_b = (rex & GET_REX_B) != 0;
        boolean is_twobyte_form = (!rex_b && !rex_x);
        int byte_zero = EmitVexPrefixByteZero(is_twobyte_form);
        int byte_one, byte_two = 0;
        if (is_twobyte_form) {
            X86_64ManagedRegister vvvv_reg = X86_64ManagedRegister.NoRegister();
            byte_one = EmitVexPrefixByteOne(r, vvvv_reg, vex_l, vex_pp);
        } else {
            byte_one = EmitVexPrefixByteOne(r, rex_x, rex_b, SET_VEX_M_0F);
            byte_two = EmitVexPrefixByteTwo(/*W=*/ false, vex_l, vex_pp);
        }
        EmitUint8(byte_zero);
        EmitUint8(byte_one);
        if (!is_twobyte_form) {
            EmitUint8(byte_two);
        }
    }

    private byte EmitVexPrefixByteOne(boolean R, boolean X, boolean B, int SET_VEX_M) {
        // Vex Byte 1,
        int vex_prefix = VEX_INIT;
        // Bit[7] This bit needs to be set to '1'
        // otherwise the instruction is LES or LDS
        if (!R) {
            // R .
            vex_prefix |= SET_VEX_R;
        }
        // Bit[6] This bit needs to be set to '1'
        // otherwise the instruction is LES or LDS
        if (!X) {
            // X .
            vex_prefix |= SET_VEX_X;
        }
        // Bit[5] This bit needs to be set to '1'
        if (!B) {
            // B .
            vex_prefix |= SET_VEX_B;
        }
        // Bits[4:0], Based on the instruction documentaion
        vex_prefix |= SET_VEX_M;
        return (byte) vex_prefix;
    }

    private byte EmitVexPrefixByteOne(boolean R,
                                      X86_64ManagedRegister operand,
                                      int SET_VEX_L,
                                      int SET_VEX_PP) {
        // Vex Byte 1,
        int vex_prefix = VEX_INIT;
        // Bit[7] This bit needs to be set to '1'
        // otherwise the instruction is LES or LDS
        if (!R) {
            // R .
            vex_prefix |= SET_VEX_R;
        }
        // Bits[6:3] - 'vvvv' the source or dest register specifier
        if (operand.isNoRegister()) {
            vex_prefix |= 0x78;
        } else if (operand.isXmmRegister()) {
            XmmRegister vvvv = operand.asXmmRegister();
            int inverted_reg = 15 - vvvv.index();
            vex_prefix |= ((inverted_reg & 0x0F) << 3);
        } else if (operand.isCpuRegister()) {
            CpuRegister vvvv = operand.asCpuRegister();
            int inverted_reg = 15 - vvvv.index();
            vex_prefix |= ((inverted_reg & 0x0F) << 3);
        }
        // Bit[2] - "L" If VEX.L = 1 indicates 256-bit vector operation,
        // VEX.L = 0 indicates 128 bit vector operation
        vex_prefix |= SET_VEX_L;
        // Bits[1:0] - "pp"
        vex_prefix |= SET_VEX_PP;
        return (byte) vex_prefix;
    }

    private byte EmitVexPrefixByteTwo(boolean W,
                                      X86_64ManagedRegister operand,
                                      int SET_VEX_L,
                                      int SET_VEX_PP) {
        // Vex Byte 2,
        int vex_prefix = VEX_INIT;
        // Bit[7] This bits needs to be set to '1' with default value.
        // When using C4H form of VEX prefix, W value is ignored
        if (W) {
            vex_prefix |= SET_VEX_W;
        }
        // Bits[6:3] - 'vvvv' the source or dest register specifier
        if (operand.isXmmRegister()) {
            XmmRegister vvvv = operand.asXmmRegister();
            int inverted_reg = 15 - vvvv.index();
            vex_prefix |= ((inverted_reg & 0x0F) << 3);
        } else if (operand.isCpuRegister()) {
            CpuRegister vvvv = operand.asCpuRegister();
            int inverted_reg = 15 - vvvv.index();
            vex_prefix |= ((inverted_reg & 0x0F) << 3);
        }
        // Bit[2] - "L" If VEX.L = 1 indicates 256-bit vector operation ,
        // VEX.L = 0 indicates 128 bit vector operation
        vex_prefix |= SET_VEX_L;
        // Bits[1:0] - "pp"
        vex_prefix |= SET_VEX_PP;
        return (byte) vex_prefix;
    }

    private byte EmitVexPrefixByteTwo(boolean W,
                                      int SET_VEX_L,
                                      int SET_VEX_PP) {
        // Vex Byte 2,
        int vex_prefix = VEX_INIT;
        // Bit[7] This bits needs to be set to '1' with default value.
        // When using C4H form of VEX prefix, REX.W value is ignored
        if (W) {
            vex_prefix |= SET_VEX_W;
        }
        // Bits[6:3] - 'vvvv' the source or dest register specifier */
        vex_prefix |= (0x0F << 3);
        // Bit[2] - "L" If VEX.L = 1 indicates 256-bit vector operation,
        // VEX.L = 0 indicates 128 bit vector operation
        vex_prefix |= SET_VEX_L;

        // Bits[1:0] - "pp"
        if (SET_VEX_PP != SET_VEX_PP_NONE) {
            vex_prefix |= SET_VEX_PP;
        }
        return (byte) vex_prefix;
    }

    private void EmitVecArithAndLogicalOperation(XmmRegister dst,
                                                 XmmRegister src1,
                                                 XmmRegister src2,
                                                 byte opcode,
                                                 int vex_pp,
                                                 boolean is_commutative) {
        if (is_commutative && src2.needsRex() && !src1.needsRex()) {
            EmitVecArithAndLogicalOperation(dst, src2, src1, opcode, vex_pp, is_commutative);
            return;
        }
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        X86_64ManagedRegister vvvv_reg = X86_64ManagedRegister.fromXmmRegister(src1);
        boolean is_twobyte_form = !src2.needsRex();
        int byte_zero = EmitVexPrefixByteZero(is_twobyte_form);
        int byte_one, byte_two = 0;
        if (is_twobyte_form) {
            byte_one = EmitVexPrefixByteOne(dst.needsRex(), vvvv_reg, SET_VEX_L_128, vex_pp);
        } else {
            //noinspection ConstantValue
            byte_one = EmitVexPrefixByteOne(dst.needsRex(), /*X=*/ false, src2.needsRex(), SET_VEX_M_0F);
            byte_two = EmitVexPrefixByteTwo(/*W=*/ false, vvvv_reg, SET_VEX_L_128, vex_pp);
        }
        EmitUint8(byte_zero);
        EmitUint8(byte_one);
        if (!is_twobyte_form) {
            EmitUint8(byte_two);
        }
        EmitUint8(opcode);
        EmitXmmRegisterOperand(dst.lowBits(), src2);
    }
}
