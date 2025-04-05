package com.v7878.jnasm.x86_64;

import static com.v7878.jnasm.common_x86.X86VEXConstants.GET_REX_B;
import static com.v7878.jnasm.common_x86.X86VEXConstants.GET_REX_X;
import static com.v7878.jnasm.common_x86.X86VEXConstants.SET_VEX_B;
import static com.v7878.jnasm.common_x86.X86VEXConstants.SET_VEX_L_128;
import static com.v7878.jnasm.common_x86.X86VEXConstants.SET_VEX_M_0F;
import static com.v7878.jnasm.common_x86.X86VEXConstants.SET_VEX_M_0F_38;
import static com.v7878.jnasm.common_x86.X86VEXConstants.SET_VEX_PP_66;
import static com.v7878.jnasm.common_x86.X86VEXConstants.SET_VEX_PP_F3;
import static com.v7878.jnasm.common_x86.X86VEXConstants.SET_VEX_PP_NONE;
import static com.v7878.jnasm.common_x86.X86VEXConstants.SET_VEX_R;
import static com.v7878.jnasm.common_x86.X86VEXConstants.SET_VEX_W;
import static com.v7878.jnasm.common_x86.X86VEXConstants.SET_VEX_X;
import static com.v7878.jnasm.common_x86.X86VEXConstants.THREE_BYTE_VEX;
import static com.v7878.jnasm.common_x86.X86VEXConstants.TWO_BYTE_VEX;
import static com.v7878.jnasm.common_x86.X86VEXConstants.VEX_INIT;
import static com.v7878.jnasm.x86_64.X86_64CpuRegister.RAX;
import static com.v7878.jnasm.x86_64.X86_64CpuRegister.RCX;

import com.v7878.jnasm.Assembler;
import com.v7878.jnasm.AssemblerFixup;
import com.v7878.jnasm.Label;
import com.v7878.jnasm.Utils;
import com.v7878.jnasm.common_x86.X86Condition;
import com.v7878.jnasm.common_x86.X86NearLabel;

import java.util.function.Consumer;

public class X86_64Assembler extends Assembler implements X86_64AssemblerI {
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

    @SuppressWarnings("SameParameterValue")
    private static void CHECK_LT(long a, long b) {
        CHECK(a < b);
    }

    @SuppressWarnings("SameParameterValue")
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

    private void EmitRegisterOperand(int rm, int reg) {
        CHECK_GE(rm, 0);
        CHECK_LT(rm, 8);
        emit8((0xC0 | (reg & 7)) + (rm << 3));
    }

    private void EmitXmmRegisterOperand(int rm, X86_64XmmRegister reg) {
        EmitRegisterOperand(rm, reg.index());
    }

    private void EmitOperandSizeOverride() {
        emit8(0x66);
    }

    private void EmitOperand(int reg_or_opcode, X86_64Operand operand) {
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

    private void EmitImmediate(X86_64Immediate imm, boolean is_16_op) {
        if (is_16_op) {
            emit8((int) imm.value() & 0xFF);
            emit8((int) imm.value() >> 8);
        } else if (imm.isInt32()) {
            emit32((int) imm.value());
        } else {
            emit64(imm.value());
        }
    }

    private void EmitImmediate(X86_64Immediate imm) {
        EmitImmediate(imm, false);
    }

    private void EmitComplex(int reg_or_opcode, X86_64Operand operand, X86_64Immediate immediate, boolean is_16_op) {
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

    private void EmitComplex(int rm, X86_64Operand operand, X86_64Immediate immediate) {
        EmitComplex(rm, operand, immediate, false);
    }

    @SuppressWarnings("SameParameterValue")
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

    private void EmitLabelLink(X86NearLabel label) {
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
                                  X86_64CpuRegister reg, X86_64Immediate imm) {
        CHECK(imm.isInt8());
        if (wide) {
            EmitRex64(reg);
        } else {
            EmitOptionalRex32(reg);
        }
        if (imm.value() == 1) {
            emit8(0xD1);
            EmitOperand(reg_or_opcode, new X86_64Operand(reg));
        } else {
            emit8(0xC1);
            EmitOperand(reg_or_opcode, new X86_64Operand(reg));
            emit8((int) imm.value() & 0xFF);
        }
    }

    private void EmitGenericShift(boolean wide, int reg_or_opcode,
                                  X86_64CpuRegister operand, X86_64CpuRegister shifter) {
        CHECK_EQ(shifter.index(), RCX.index());
        if (wide) {
            EmitRex64(operand);
        } else {
            EmitOptionalRex32(operand);
        }
        emit8(0xD3);
        EmitOperand(reg_or_opcode, new X86_64Operand(operand));
    }

    private void EmitMovCpuFpu(X86_64XmmRegister fp_reg, X86_64CpuRegister cpu_reg,
                               boolean is64bit, int opcode) {
        emit8(0x66);
        EmitOptionalRex(false, is64bit, fp_reg.needsRex(), false, cpu_reg.needsRex());
        emit8(0x0F);
        emit8(opcode);
        EmitOperand(fp_reg.lowBits(), new X86_64Operand(cpu_reg));
    }

    @SuppressWarnings("SameParameterValue")
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
            emit8(rex);
        }
    }

    private void EmitOptionalRex32(X86_64CpuRegister reg) {
        EmitOptionalRex(false, false, false, false, reg.needsRex());
    }

    private void EmitOptionalRex32(X86_64CpuRegister dst, X86_64CpuRegister src) {
        EmitOptionalRex(false, false, dst.needsRex(), false, src.needsRex());
    }

    private void EmitOptionalRex32(X86_64XmmRegister dst, X86_64XmmRegister src) {
        EmitOptionalRex(false, false, dst.needsRex(), false, src.needsRex());
    }

    private void EmitOptionalRex32(X86_64CpuRegister dst, X86_64XmmRegister src) {
        EmitOptionalRex(false, false, dst.needsRex(), false, src.needsRex());
    }

    private void EmitOptionalRex32(X86_64XmmRegister dst, X86_64CpuRegister src) {
        EmitOptionalRex(false, false, dst.needsRex(), false, src.needsRex());
    }

    private void EmitOptionalRex32(X86_64Operand operand) {
        int rex = operand.rex();
        if (rex != 0) {
            emit8(rex);
        }
    }

    private void EmitOptionalRex32(X86_64CpuRegister dst, X86_64Operand operand) {
        int rex = operand.rex();
        if (dst.needsRex()) {
            rex |= 0x44;  // REX.0R00
        }
        if (rex != 0) {
            emit8(rex);
        }
    }

    private void EmitOptionalRex32(X86_64XmmRegister dst, X86_64Operand operand) {
        int rex = operand.rex();
        if (dst.needsRex()) {
            rex |= 0x44;  // REX.0R00
        }
        if (rex != 0) {
            emit8(rex);
        }
    }

    private void EmitRex64() {
        EmitOptionalRex(false, true, false, false, false);
    }

    private void EmitRex64(X86_64CpuRegister reg) {
        EmitOptionalRex(false, true, false, false, reg.needsRex());
    }

    private void EmitRex64(X86_64Operand operand) {
        int rex = operand.rex();
        rex |= 0x48;  // REX.W000
        emit8(rex);
    }

    private void EmitRex64(X86_64CpuRegister dst, X86_64CpuRegister src) {
        EmitOptionalRex(false, true, dst.needsRex(), false, src.needsRex());
    }

    private void EmitRex64(X86_64XmmRegister dst, X86_64CpuRegister src) {
        EmitOptionalRex(false, true, dst.needsRex(), false, src.needsRex());
    }

    private void EmitRex64(X86_64CpuRegister dst, X86_64XmmRegister src) {
        EmitOptionalRex(false, true, dst.needsRex(), false, src.needsRex());
    }

    private void EmitRex64(X86_64CpuRegister dst, X86_64Operand operand) {
        int rex = 0x48 | operand.rex();  // REX.W000
        if (dst.needsRex()) {
            rex |= 0x44;  // REX.0R00
        }
        emit8(rex);
    }

    private void EmitRex64(X86_64XmmRegister dst, X86_64Operand operand) {
        int rex = 0x48 | operand.rex();  // REX.W000
        if (dst.needsRex()) {
            rex |= 0x44;  // REX.0R00
        }
        emit8(rex);
    }

    private void EmitOptionalByteRegNormalizingRex32(
            X86_64CpuRegister dst, X86_64CpuRegister src, boolean normalize_both) {
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

    private void EmitOptionalByteRegNormalizingRex32(
            X86_64CpuRegister dst, X86_64CpuRegister src) {
        EmitOptionalByteRegNormalizingRex32(dst, src, false);
    }

    private void EmitOptionalByteRegNormalizingRex32(X86_64CpuRegister dst, X86_64Operand operand) {
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
            emit8(rex);
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

    @SuppressWarnings("SameParameterValue")
    private void EmitVexPrefixForAddress(X86_64Address addr, boolean r, int vex_l, int vex_pp) {
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
        emit8(byte_zero);
        emit8(byte_one);
        if (!is_twobyte_form) {
            emit8(byte_two);
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
            X86_64XmmRegister vvvv = operand.asXmmRegister();
            int inverted_reg = 15 - vvvv.index();
            vex_prefix |= ((inverted_reg & 0x0F) << 3);
        } else if (operand.isCpuRegister()) {
            X86_64CpuRegister vvvv = operand.asCpuRegister();
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

    @SuppressWarnings("SameParameterValue")
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
            X86_64XmmRegister vvvv = operand.asXmmRegister();
            int inverted_reg = 15 - vvvv.index();
            vex_prefix |= ((inverted_reg & 0x0F) << 3);
        } else if (operand.isCpuRegister()) {
            X86_64CpuRegister vvvv = operand.asCpuRegister();
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

    @SuppressWarnings("SameParameterValue")
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

    private void EmitVecArithAndLogicalOperation(X86_64XmmRegister dst,
                                                 X86_64XmmRegister src1,
                                                 X86_64XmmRegister src2,
                                                 int opcode,
                                                 int vex_pp,
                                                 boolean is_commutative) {
        if (is_commutative && src2.needsRex() && !src1.needsRex()) {
            //noinspection ConstantValue
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
        emit8(byte_zero);
        emit8(byte_one);
        if (!is_twobyte_form) {
            emit8(byte_two);
        }
        emit8(opcode);
        EmitXmmRegisterOperand(dst.lowBits(), src2);
    }

    private void EmitVecArithAndLogicalOperation(X86_64XmmRegister dst,
                                                 X86_64XmmRegister src1,
                                                 X86_64XmmRegister src2,
                                                 int opcode,
                                                 int vex_pp) {
        EmitVecArithAndLogicalOperation(dst, src1, src2, opcode, vex_pp, false);
    }

    public void call(X86_64CpuRegister reg) {
        EmitOptionalRex32(reg);
        emit8(0xFF);
        EmitRegisterOperand(2, reg.lowBits());
    }

    public void call(X86_64Address address) {
        EmitOptionalRex32(address);
        emit8(0xFF);
        EmitOperand(2, address);
    }

    public void call(Label label) {
        emit8(0xE8);
        final int kSize = 5;
        // Offset by one because we already have emitted the opcode.
        EmitLabel(label, kSize - 1);
    }

    public void pushq(X86_64CpuRegister reg) {
        EmitOptionalRex32(reg);
        emit8(0x50 + reg.lowBits());
    }

    public void pushq(X86_64Address address) {
        EmitOptionalRex32(address);
        emit8(0xFF);
        EmitOperand(6, address);
    }

    public void pushq(X86_64Immediate imm) {
        CHECK(imm.isInt32());  // pushq only supports 32b immediate.
        if (imm.isInt8()) {
            emit8(0x6A);
            emit8((int) imm.value() & 0xFF);
        } else {
            emit8(0x68);
            EmitImmediate(imm);
        }
    }

    public void popq(X86_64CpuRegister reg) {
        EmitOptionalRex32(reg);
        emit8(0x58 + reg.lowBits());
    }

    public void popq(X86_64Address address) {
        EmitOptionalRex32(address);
        emit8(0x8F);
        EmitOperand(0, address);
    }

    public void movq(X86_64CpuRegister dst, X86_64Immediate imm) {
        if (imm.isInt32()) {
            // 32 bit. Note: sign-extends.
            EmitRex64(dst);
            emit8(0xC7);
            EmitRegisterOperand(0, dst.lowBits());
            emit32((int) (imm.value()));
        } else {
            EmitRex64(dst);
            emit8(0xB8 + dst.lowBits());
            emit64(imm.value());
        }
    }

    public void movl(X86_64CpuRegister dst, X86_64Immediate imm) {
        CHECK(imm.isInt32());
        EmitOptionalRex32(dst);
        emit8(0xB8 + dst.lowBits());
        EmitImmediate(imm);
    }

    public void movq(X86_64Address dst, X86_64Immediate imm) {
        CHECK(imm.isInt32());
        EmitRex64(dst);
        emit8(0xC7);
        EmitOperand(0, dst);
        EmitImmediate(imm);
    }

    public void movq(X86_64CpuRegister dst, X86_64CpuRegister src) {
        // 0x89 is movq r/m64 <- r64, with op1 in r/m and op2 in reg: so reverse EmitRex64
        EmitRex64(src, dst);
        emit8(0x89);
        EmitRegisterOperand(src.lowBits(), dst.lowBits());
    }

    public void movl(X86_64CpuRegister dst, X86_64CpuRegister src) {
        EmitOptionalRex32(dst, src);
        emit8(0x8B);
        EmitRegisterOperand(dst.lowBits(), src.lowBits());
    }

    public void movq(X86_64CpuRegister dst, X86_64Address src) {
        EmitRex64(dst, src);
        emit8(0x8B);
        EmitOperand(dst.lowBits(), src);
    }

    public void movl(X86_64CpuRegister dst, X86_64Address src) {
        EmitOptionalRex32(dst, src);
        emit8(0x8B);
        EmitOperand(dst.lowBits(), src);
    }

    public void movq(X86_64Address dst, X86_64CpuRegister src) {
        EmitRex64(src, dst);
        emit8(0x89);
        EmitOperand(src.lowBits(), dst);
    }

    public void movl(X86_64Address dst, X86_64CpuRegister src) {
        EmitOptionalRex32(src, dst);
        emit8(0x89);
        EmitOperand(src.lowBits(), dst);
    }

    public void movl(X86_64Address dst, X86_64Immediate imm) {
        EmitOptionalRex32(dst);
        emit8(0xC7);
        EmitOperand(0, dst);
        EmitImmediate(imm);
    }

    public void movntl(X86_64Address dst, X86_64CpuRegister src) {
        EmitOptionalRex32(src, dst);
        emit8(0x0F);
        emit8(0xC3);
        EmitOperand(src.lowBits(), dst);
    }

    public void movntq(X86_64Address dst, X86_64CpuRegister src) {
        EmitRex64(src, dst);
        emit8(0x0F);
        emit8(0xC3);
        EmitOperand(src.lowBits(), dst);
    }

    public void cmov(X86Condition c, X86_64CpuRegister dst, X86_64CpuRegister src) {
        cmov(c, dst, src, true);
    }

    public void cmov(X86Condition c, X86_64CpuRegister dst, X86_64CpuRegister src, boolean is64bit) {
        EmitOptionalRex(false, is64bit, dst.needsRex(), false, src.needsRex());
        emit8(0x0F);
        emit8(0x40 + c.index());
        EmitRegisterOperand(dst.lowBits(), src.lowBits());
    }

    public void cmov(X86Condition c, X86_64CpuRegister dst, X86_64Address src, boolean is64bit) {
        if (is64bit) {
            EmitRex64(dst, src);
        } else {
            EmitOptionalRex32(dst, src);
        }
        emit8(0x0F);
        emit8(0x40 + c.index());
        EmitOperand(dst.lowBits(), src);
    }

    public void movzxb(X86_64CpuRegister dst, X86_64CpuRegister src) {
        EmitOptionalByteRegNormalizingRex32(dst, src);
        emit8(0x0F);
        emit8(0xB6);
        EmitRegisterOperand(dst.lowBits(), src.lowBits());
    }

    public void movzxb(X86_64CpuRegister dst, X86_64Address src) {
        // Byte register is only in the source register form, so we don't use
        // EmitOptionalByteRegNormalizingRex32(dst, src);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xB6);
        EmitOperand(dst.lowBits(), src);
    }

    public void movsxb(X86_64CpuRegister dst, X86_64CpuRegister src) {
        EmitOptionalByteRegNormalizingRex32(dst, src);
        emit8(0x0F);
        emit8(0xBE);
        EmitRegisterOperand(dst.lowBits(), src.lowBits());
    }

    public void movsxb(X86_64CpuRegister dst, X86_64Address src) {
        // Byte register is only in the source register form, so we don't use
        // EmitOptionalByteRegNormalizingRex32(dst, src);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xBE);
        EmitOperand(dst.lowBits(), src);
    }

    public void movb(X86_64CpuRegister dst, X86_64Address src) {
        throw new IllegalStateException("Use movzxb or movsxb instead");
    }

    public void movb(X86_64Address dst, X86_64CpuRegister src) {
        EmitOptionalByteRegNormalizingRex32(src, dst);
        emit8(0x88);
        EmitOperand(src.lowBits(), dst);
    }

    public void movb(X86_64Address dst, X86_64Immediate imm) {
        EmitOptionalRex32(dst);
        emit8(0xC6);
        EmitOperand(RAX.index(), dst);
        CHECK(imm.isInt8());
        emit8((int) imm.value() & 0xFF);
    }

    public void movzxw(X86_64CpuRegister dst, X86_64CpuRegister src) {
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xB7);
        EmitRegisterOperand(dst.lowBits(), src.lowBits());
    }

    public void movzxw(X86_64CpuRegister dst, X86_64Address src) {
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xB7);
        EmitOperand(dst.lowBits(), src);
    }

    public void movsxw(X86_64CpuRegister dst, X86_64CpuRegister src) {
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xBF);
        EmitRegisterOperand(dst.lowBits(), src.lowBits());
    }

    public void movsxw(X86_64CpuRegister dst, X86_64Address src) {
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xBF);
        EmitOperand(dst.lowBits(), src);
    }

    public void movw(X86_64CpuRegister dst, X86_64Address src) {
        throw new IllegalStateException("Use movzxb or movsxw instead");
    }

    public void movw(X86_64Address dst, X86_64CpuRegister src) {
        EmitOperandSizeOverride();
        EmitOptionalRex32(src, dst);
        emit8(0x89);
        EmitOperand(src.lowBits(), dst);
    }

    public void movw(X86_64Address dst, X86_64Immediate imm) {
        EmitOperandSizeOverride();
        EmitOptionalRex32(dst);
        emit8(0xC7);
        EmitOperand(RAX.index(), dst);
        CHECK(imm.isUInt16() || imm.isInt16());
        emit8((int) imm.value() & 0xFF);
        emit8((int) imm.value() >> 8);
    }

    public void leaq(X86_64CpuRegister dst, X86_64Address src) {
        EmitRex64(dst, src);
        emit8(0x8D);
        EmitOperand(dst.lowBits(), src);
    }

    public void leal(X86_64CpuRegister dst, X86_64Address src) {
        EmitOptionalRex32(dst, src);
        emit8(0x8D);
        EmitOperand(dst.lowBits(), src);
    }

    public void movaps(X86_64XmmRegister dst, X86_64XmmRegister src) {
        if (cpuHasAVXorAVX2FeatureFlag()) {
            vmovaps(dst, src);
            return;
        }
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x28);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    /*VEX.128.0F.WIG 28 /r VMOVAPS xmm1, xmm2 */
    public void vmovaps(X86_64XmmRegister dst, X86_64XmmRegister src) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte byte_zero, byte_one, byte_two = 0;
        boolean is_twobyte_form = true;
        boolean load = dst.needsRex();
        boolean store = !load;
        if (src.needsRex() && dst.needsRex()) {
            is_twobyte_form = false;
        }
        // Instruction VEX Prefix
        byte_zero = EmitVexPrefixByteZero(is_twobyte_form);
        X86_64ManagedRegister vvvv_reg = X86_64ManagedRegister.NoRegister();
        if (is_twobyte_form) {
            boolean rex_bit = (load) ? dst.needsRex() : src.needsRex();
            byte_one = EmitVexPrefixByteOne(rex_bit,
                    vvvv_reg,
                    SET_VEX_L_128,
                    SET_VEX_PP_NONE);
        } else {
            byte_one = EmitVexPrefixByteOne(dst.needsRex(),
                    /*X=*/ false,
                    src.needsRex(),
                    SET_VEX_M_0F);
            byte_two = EmitVexPrefixByteTwo(/*W=*/ false,
                    SET_VEX_L_128,
                    SET_VEX_PP_NONE);
        }
        emit8(byte_zero);
        emit8(byte_one);
        if (!is_twobyte_form) {
            emit8(byte_two);
        }
        // Instruction Opcode
        if (is_twobyte_form && store) {
            emit8(0x29);
        } else {
            emit8(0x28);
        }
        // Instruction Operands
        if (is_twobyte_form && store) {
            EmitXmmRegisterOperand(src.lowBits(), dst);
        } else {
            EmitXmmRegisterOperand(dst.lowBits(), src);
        }
    }

    public void movaps(X86_64XmmRegister dst, X86_64Address src) {
        if (cpuHasAVXorAVX2FeatureFlag()) {
            vmovaps(dst, src);
            return;
        }
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x28);
        EmitOperand(dst.lowBits(), src);
    }

    /*VEX.128.0F.WIG 28 /r VMOVAPS xmm1, m128 */
    public void vmovaps(X86_64XmmRegister dst, X86_64Address src) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        // Instruction VEX Prefix
        EmitVexPrefixForAddress(src, dst.needsRex(), SET_VEX_L_128, SET_VEX_PP_NONE);
        // Instruction Opcode
        emit8(0x28);
        // Instruction Operands
        EmitOperand(dst.lowBits(), src);
    }

    public void movups(X86_64XmmRegister dst, X86_64Address src) {
        if (cpuHasAVXorAVX2FeatureFlag()) {
            vmovups(dst, src);
            return;
        }
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x10);
        EmitOperand(dst.lowBits(), src);
    }

    /* VEX.128.0F.WIG 10 /r VMOVUPS xmm1, m128 */
    public void vmovups(X86_64XmmRegister dst, X86_64Address src) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        // Instruction VEX Prefix
        EmitVexPrefixForAddress(src, dst.needsRex(), SET_VEX_L_128, SET_VEX_PP_NONE);
        // Instruction Opcode
        emit8(0x10);
        // Instruction Operands
        EmitOperand(dst.lowBits(), src);
    }

    public void movaps(X86_64Address dst, X86_64XmmRegister src) {
        if (cpuHasAVXorAVX2FeatureFlag()) {
            vmovaps(dst, src);
            return;
        }
        EmitOptionalRex32(src, dst);
        emit8(0x0F);
        emit8(0x29);
        EmitOperand(src.lowBits(), dst);
    }

    /* VEX.128.0F.WIG 29 /r VMOVAPS m128, xmm1 */
    public void vmovaps(X86_64Address dst, X86_64XmmRegister src) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        // Instruction VEX Prefix
        EmitVexPrefixForAddress(dst, src.needsRex(), SET_VEX_L_128, SET_VEX_PP_NONE);
        // Instruction Opcode
        emit8(0x29);
        // Instruction Operands
        EmitOperand(src.lowBits(), dst);
    }

    public void movups(X86_64Address dst, X86_64XmmRegister src) {
        if (cpuHasAVXorAVX2FeatureFlag()) {
            vmovups(dst, src);
            return;
        }
        EmitOptionalRex32(src, dst);
        emit8(0x0F);
        emit8(0x11);
        EmitOperand(src.lowBits(), dst);
    }

    /* VEX.128.0F.WIG 11 /r VMOVUPS m128, xmm1 */
    public void vmovups(X86_64Address dst, X86_64XmmRegister src) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        // Instruction VEX Prefix
        EmitVexPrefixForAddress(dst, src.needsRex(), SET_VEX_L_128, SET_VEX_PP_NONE);
        // Instruction Opcode
        emit8(0x11);
        // Instruction Operands
        EmitOperand(src.lowBits(), dst);
    }

    public void movss(X86_64XmmRegister dst, X86_64Address src) {
        emit8(0xF3);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x10);
        EmitOperand(dst.lowBits(), src);
    }

    public void movss(X86_64Address dst, X86_64XmmRegister src) {
        emit8(0xF3);
        EmitOptionalRex32(src, dst);
        emit8(0x0F);
        emit8(0x11);
        EmitOperand(src.lowBits(), dst);
    }

    public void movss(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0xF3);
        EmitOptionalRex32(src, dst);  // Movss is MR encoding instead of the usual RM.
        emit8(0x0F);
        emit8(0x11);
        EmitXmmRegisterOperand(src.lowBits(), dst);
    }

    public void movsxd(X86_64CpuRegister dst, X86_64CpuRegister src) {
        EmitRex64(dst, src);
        emit8(0x63);
        EmitRegisterOperand(dst.lowBits(), src.lowBits());
    }

    public void movsxd(X86_64CpuRegister dst, X86_64Address src) {
        EmitRex64(dst, src);
        emit8(0x63);
        EmitOperand(dst.lowBits(), src);
    }

    public void movq(X86_64XmmRegister dst, X86_64CpuRegister src) {
        EmitMovCpuFpu(dst, src, /*is64bit=*/ true, /*opcode=*/ 0x6E);
    }

    public void movq(X86_64CpuRegister dst, X86_64XmmRegister src) {
        EmitMovCpuFpu(src, dst, /*is64bit=*/ true, /*opcode=*/ 0x7E);
    }

    public void movd(X86_64XmmRegister dst, X86_64CpuRegister src) {
        EmitMovCpuFpu(dst, src, /*is64bit=*/ false, /*opcode=*/ 0x6E);
    }

    public void movd(X86_64CpuRegister dst, X86_64XmmRegister src) {
        EmitMovCpuFpu(src, dst, /*is64bit=*/ false, /*opcode=*/ 0x7E);
    }

    public void addss(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0xF3);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x58);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void addss(X86_64XmmRegister dst, X86_64Address src) {
        emit8(0xF3);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x58);
        EmitOperand(dst.lowBits(), src);
    }

    public void subss(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0xF3);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x5C);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void subss(X86_64XmmRegister dst, X86_64Address src) {
        emit8(0xF3);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x5C);
        EmitOperand(dst.lowBits(), src);
    }

    public void mulss(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0xF3);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x59);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void mulss(X86_64XmmRegister dst, X86_64Address src) {
        emit8(0xF3);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x59);
        EmitOperand(dst.lowBits(), src);
    }

    public void divss(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0xF3);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x5E);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void divss(X86_64XmmRegister dst, X86_64Address src) {
        emit8(0xF3);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x5E);
        EmitOperand(dst.lowBits(), src);
    }

    public void addps(X86_64XmmRegister dst, X86_64XmmRegister src) {
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x58);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void subps(X86_64XmmRegister dst, X86_64XmmRegister src) {
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x5C);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void vaddps(X86_64XmmRegister dst, X86_64XmmRegister add_left, X86_64XmmRegister add_right) {
        EmitVecArithAndLogicalOperation(
                dst, add_left, add_right, /*opcode=*/ 0x58, SET_VEX_PP_NONE, /*is_commutative=*/ true);
    }

    public void vsubps(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2) {
        EmitVecArithAndLogicalOperation(dst, src1, src2, /*opcode=*/ 0x5C, SET_VEX_PP_NONE);
    }

    public void mulps(X86_64XmmRegister dst, X86_64XmmRegister src) {
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x59);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void vmulps(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2) {
        EmitVecArithAndLogicalOperation(
                dst, src1, src2, /*opcode=*/ 0x59, SET_VEX_PP_NONE, /*is_commutative=*/ true);
    }

    public void divps(X86_64XmmRegister dst, X86_64XmmRegister src) {
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x5E);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void vdivps(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2) {
        EmitVecArithAndLogicalOperation(dst, src1, src2, /*opcode=*/ 0x5E, SET_VEX_PP_NONE);
    }

    public void vfmadd213ss(X86_64XmmRegister acc, X86_64XmmRegister left, X86_64XmmRegister right) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte ByteZero, ByteOne, ByteTwo;
        ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ false);
        X86_64ManagedRegister vvvv_reg =
                X86_64ManagedRegister.fromXmmRegister(left);
        ByteOne = EmitVexPrefixByteOne(acc.needsRex(),
                /*X=*/ false,
                right.needsRex(),
                SET_VEX_M_0F_38);
        ByteTwo = EmitVexPrefixByteTwo(/*W=*/ false, vvvv_reg, SET_VEX_L_128, SET_VEX_PP_66);
        emit8(ByteZero);
        emit8(ByteOne);
        emit8(ByteTwo);
        emit8(0xA9);
        EmitXmmRegisterOperand(acc.lowBits(), right);
    }

    public void vfmadd213sd(X86_64XmmRegister acc, X86_64XmmRegister left, X86_64XmmRegister right) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte ByteZero, ByteOne, ByteTwo;
        ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ false);
        X86_64ManagedRegister vvvv_reg =
                X86_64ManagedRegister.fromXmmRegister(left);
        ByteOne = EmitVexPrefixByteOne(acc.needsRex(),
                /*X=*/ false,
                right.needsRex(),
                SET_VEX_M_0F_38);
        ByteTwo = EmitVexPrefixByteTwo(/*W=*/ true, vvvv_reg, SET_VEX_L_128, SET_VEX_PP_66);
        emit8(ByteZero);
        emit8(ByteOne);
        emit8(ByteTwo);
        emit8(0xA9);
        EmitXmmRegisterOperand(acc.lowBits(), right);
    }

    public void flds(X86_64Address src) {
        emit8(0xD9);
        EmitOperand(0, src);
    }

    public void fsts(X86_64Address dst) {
        emit8(0xD9);
        EmitOperand(2, dst);
    }

    public void fstps(X86_64Address dst) {
        emit8(0xD9);
        EmitOperand(3, dst);
    }

    public void movapd(X86_64XmmRegister dst, X86_64XmmRegister src) {
        if (cpuHasAVXorAVX2FeatureFlag()) {
            vmovapd(dst, src);
            return;
        }
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x28);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    /* VEX.128.66.0F.WIG 28 /r VMOVAPD xmm1, xmm2 */
    public void vmovapd(X86_64XmmRegister dst, X86_64XmmRegister src) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte ByteZero, ByteOne, ByteTwo = 0;
        boolean is_twobyte_form = !src.needsRex() || !dst.needsRex();
        // Instruction VEX Prefix
        ByteZero = EmitVexPrefixByteZero(is_twobyte_form);
        boolean load = dst.needsRex();
        if (is_twobyte_form) {
            X86_64ManagedRegister vvvv_reg = X86_64ManagedRegister.NoRegister();
            boolean rex_bit = load ? dst.needsRex() : src.needsRex();
            ByteOne = EmitVexPrefixByteOne(rex_bit,
                    vvvv_reg,
                    SET_VEX_L_128,
                    SET_VEX_PP_66);
        } else {
            //noinspection ConstantValue
            ByteOne = EmitVexPrefixByteOne(dst.needsRex(),
                    /*X=*/ false,
                    src.needsRex(),
                    SET_VEX_M_0F);
            ByteTwo = EmitVexPrefixByteTwo(/*W=*/ false,
                    SET_VEX_L_128,
                    SET_VEX_PP_66);
        }
        emit8(ByteZero);
        emit8(ByteOne);
        if (!is_twobyte_form) {
            emit8(ByteTwo);
        }
        // Instruction Opcode
        if (is_twobyte_form && !load) {
            emit8(0x29);
        } else {
            emit8(0x28);
        }
        // Instruction Operands
        if (is_twobyte_form && !load) {
            EmitXmmRegisterOperand(src.lowBits(), dst);
        } else {
            EmitXmmRegisterOperand(dst.lowBits(), src);
        }
    }

    public void movapd(X86_64XmmRegister dst, X86_64Address src) {
        if (cpuHasAVXorAVX2FeatureFlag()) {
            vmovapd(dst, src);
            return;
        }
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x28);
        EmitOperand(dst.lowBits(), src);
    }

    /* VEX.128.66.0F.WIG 28 /r VMOVAPD xmm1, m128 */
    public void vmovapd(X86_64XmmRegister dst, X86_64Address src) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        // Instruction VEX Prefix
        EmitVexPrefixForAddress(src, dst.needsRex(), SET_VEX_L_128, SET_VEX_PP_66);
        // Instruction Opcode
        emit8(0x28);
        // Instruction Operands
        EmitOperand(dst.lowBits(), src);
    }

    public void movupd(X86_64XmmRegister dst, X86_64Address src) {
        if (cpuHasAVXorAVX2FeatureFlag()) {
            vmovupd(dst, src);
            return;
        }
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x10);
        EmitOperand(dst.lowBits(), src);
    }

    /* VEX.128.66.0F.WIG 10 /r VMOVUPD xmm1, m128 */
    public void vmovupd(X86_64XmmRegister dst, X86_64Address src) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        // Instruction VEX Prefix
        EmitVexPrefixForAddress(src, dst.needsRex(), SET_VEX_L_128, SET_VEX_PP_66);
        // Instruction Opcode
        emit8(0x10);
        // Instruction Operands
        EmitOperand(dst.lowBits(), src);
    }

    public void movapd(X86_64Address dst, X86_64XmmRegister src) {
        if (cpuHasAVXorAVX2FeatureFlag()) {
            vmovapd(dst, src);
            return;
        }
        emit8(0x66);
        EmitOptionalRex32(src, dst);
        emit8(0x0F);
        emit8(0x29);
        EmitOperand(src.lowBits(), dst);
    }

    /* VEX.128.66.0F.WIG 29 /r VMOVAPD m128, xmm1 */
    public void vmovapd(X86_64Address dst, X86_64XmmRegister src) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        // Instruction VEX Prefix
        EmitVexPrefixForAddress(dst, src.needsRex(), SET_VEX_L_128, SET_VEX_PP_66);
        // Instruction Opcode
        emit8(0x29);
        // Instruction Operands
        EmitOperand(src.lowBits(), dst);
    }

    public void movupd(X86_64Address dst, X86_64XmmRegister src) {
        if (cpuHasAVXorAVX2FeatureFlag()) {
            vmovupd(dst, src);
            return;
        }
        emit8(0x66);
        EmitOptionalRex32(src, dst);
        emit8(0x0F);
        emit8(0x11);
        EmitOperand(src.lowBits(), dst);
    }

    /* VEX.128.66.0F.WIG 11 /r VMOVUPD m128, xmm1 */
    public void vmovupd(X86_64Address dst, X86_64XmmRegister src) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        // Instruction VEX Prefix
        EmitVexPrefixForAddress(dst, src.needsRex(), SET_VEX_L_128, SET_VEX_PP_66);
        // Instruction Opcode
        emit8(0x11);
        // Instruction Operands
        EmitOperand(src.lowBits(), dst);
    }

    public void movsd(X86_64XmmRegister dst, X86_64Address src) {
        emit8(0xF2);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x10);
        EmitOperand(dst.lowBits(), src);
    }

    public void movsd(X86_64Address dst, X86_64XmmRegister src) {
        emit8(0xF2);
        EmitOptionalRex32(src, dst);
        emit8(0x0F);
        emit8(0x11);
        EmitOperand(src.lowBits(), dst);
    }

    public void movsd(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0xF2);
        EmitOptionalRex32(src, dst);  // Movsd is MR encoding instead of the usual RM.
        emit8(0x0F);
        emit8(0x11);
        EmitXmmRegisterOperand(src.lowBits(), dst);
    }

    public void addsd(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0xF2);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x58);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void addsd(X86_64XmmRegister dst, X86_64Address src) {
        emit8(0xF2);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x58);
        EmitOperand(dst.lowBits(), src);
    }

    public void subsd(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0xF2);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x5C);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void subsd(X86_64XmmRegister dst, X86_64Address src) {
        emit8(0xF2);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x5C);
        EmitOperand(dst.lowBits(), src);
    }

    public void mulsd(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0xF2);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x59);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void mulsd(X86_64XmmRegister dst, X86_64Address src) {
        emit8(0xF2);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x59);
        EmitOperand(dst.lowBits(), src);
    }

    public void divsd(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0xF2);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x5E);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void divsd(X86_64XmmRegister dst, X86_64Address src) {
        emit8(0xF2);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x5E);
        EmitOperand(dst.lowBits(), src);
    }

    public void addpd(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x58);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void vaddpd(X86_64XmmRegister dst, X86_64XmmRegister add_left, X86_64XmmRegister add_right) {
        EmitVecArithAndLogicalOperation(
                dst, add_left, add_right, /*opcode=*/ 0x58, SET_VEX_PP_66, /*is_commutative=*/ true);
    }

    public void subpd(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x5C);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void vsubpd(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2) {
        EmitVecArithAndLogicalOperation(dst, src1, src2, /*opcode=*/ 0x5C, SET_VEX_PP_66);
    }

    public void mulpd(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x59);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void vmulpd(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2) {
        EmitVecArithAndLogicalOperation(
                dst, src1, src2, /*opcode=*/ 0x59, SET_VEX_PP_66, /*is_commutative=*/ true);
    }

    public void divpd(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x5E);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void vdivpd(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2) {
        EmitVecArithAndLogicalOperation(dst, src1, src2, /*opcode=*/ 0x5E, SET_VEX_PP_66);
    }

    public void movdqa(X86_64XmmRegister dst, X86_64XmmRegister src) {
        if (cpuHasAVXorAVX2FeatureFlag()) {
            vmovdqa(dst, src);
            return;
        }
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x6F);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    /* VEX.128.66.0F.WIG 6F /r VMOVDQA xmm1, xmm2 */
    public void vmovdqa(X86_64XmmRegister dst, X86_64XmmRegister src) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte ByteZero, ByteOne, ByteTwo = 0;
        boolean is_twobyte_form = !src.needsRex() || !dst.needsRex();
        // Instruction VEX Prefix
        boolean load = dst.needsRex();
        ByteZero = EmitVexPrefixByteZero(is_twobyte_form);
        if (is_twobyte_form) {
            X86_64ManagedRegister vvvv_reg = X86_64ManagedRegister.NoRegister();
            boolean rex_bit = load ? dst.needsRex() : src.needsRex();
            ByteOne = EmitVexPrefixByteOne(rex_bit,
                    vvvv_reg,
                    SET_VEX_L_128,
                    SET_VEX_PP_66);
        } else {
            //noinspection ConstantValue
            ByteOne = EmitVexPrefixByteOne(dst.needsRex(),
                    /*X=*/ false,
                    src.needsRex(),
                    SET_VEX_M_0F);
            ByteTwo = EmitVexPrefixByteTwo(/*W=*/ false,
                    SET_VEX_L_128,
                    SET_VEX_PP_66);
        }
        emit8(ByteZero);
        emit8(ByteOne);
        if (!is_twobyte_form) {
            emit8(ByteTwo);
        }
        // Instruction Opcode
        if (is_twobyte_form && !load) {
            emit8(0x7F);
        } else {
            emit8(0x6F);
        }
        // Instruction Operands
        if (is_twobyte_form && !load) {
            EmitXmmRegisterOperand(src.lowBits(), dst);
        } else {
            EmitXmmRegisterOperand(dst.lowBits(), src);
        }
    }

    public void movdqa(X86_64XmmRegister dst, X86_64Address src) {
        if (cpuHasAVXorAVX2FeatureFlag()) {
            vmovdqa(dst, src);
            return;
        }
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x6F);
        EmitOperand(dst.lowBits(), src);
    }

    /* VEX.128.66.0F.WIG 6F /r VMOVDQA xmm1, m128 */
    public void vmovdqa(X86_64XmmRegister dst, X86_64Address src) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        // Instruction VEX Prefix
        EmitVexPrefixForAddress(src, dst.needsRex(), SET_VEX_L_128, SET_VEX_PP_66);
        // Instruction Opcode
        emit8(0x6F);
        // Instruction Operands
        EmitOperand(dst.lowBits(), src);
    }

    public void movdqu(X86_64XmmRegister dst, X86_64Address src) {
        if (cpuHasAVXorAVX2FeatureFlag()) {
            vmovdqu(dst, src);
            return;
        }
        emit8(0xF3);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x6F);
        EmitOperand(dst.lowBits(), src);
    }

    /* VEX.128.F3.0F.WIG 6F /r VMOVDQU xmm1, m128
Load Unaligned */
    public void vmovdqu(X86_64XmmRegister dst, X86_64Address src) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        // Instruction VEX Prefix
        EmitVexPrefixForAddress(src, dst.needsRex(), SET_VEX_L_128, SET_VEX_PP_F3);
        // Instruction Opcode
        emit8(0x6F);
        // Instruction Operands
        EmitOperand(dst.lowBits(), src);
    }

    public void movdqa(X86_64Address dst, X86_64XmmRegister src) {
        if (cpuHasAVXorAVX2FeatureFlag()) {
            vmovdqa(dst, src);
            return;
        }
        emit8(0x66);
        EmitOptionalRex32(src, dst);
        emit8(0x0F);
        emit8(0x7F);
        EmitOperand(src.lowBits(), dst);
    }

    /* VEX.128.66.0F.WIG 7F /r VMOVDQA m128, xmm1 */
    public void vmovdqa(X86_64Address dst, X86_64XmmRegister src) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        // Instruction VEX Prefix
        EmitVexPrefixForAddress(dst, src.needsRex(), SET_VEX_L_128, SET_VEX_PP_66);
        // Instruction Opcode
        emit8(0x7F);
        // Instruction Operands
        EmitOperand(src.lowBits(), dst);
    }

    public void movdqu(X86_64Address dst, X86_64XmmRegister src) {
        if (cpuHasAVXorAVX2FeatureFlag()) {
            vmovdqu(dst, src);
            return;
        }
        emit8(0xF3);
        EmitOptionalRex32(src, dst);
        emit8(0x0F);
        emit8(0x7F);
        EmitOperand(src.lowBits(), dst);
    }

    /* VEX.128.F3.0F.WIG 7F /r VMOVDQU m128, xmm1 */
    public void vmovdqu(X86_64Address dst, X86_64XmmRegister src) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        // Instruction VEX Prefix
        EmitVexPrefixForAddress(dst, src.needsRex(), SET_VEX_L_128, SET_VEX_PP_F3);
        // Instruction Opcode
        emit8(0x7F);
        // Instruction Operands
        EmitOperand(src.lowBits(), dst);
    }

    public void paddb(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xFC);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void vpaddb(X86_64XmmRegister dst, X86_64XmmRegister add_left, X86_64XmmRegister add_right) {
        EmitVecArithAndLogicalOperation(
                dst, add_left, add_right, /*opcode=*/ 0xFC, SET_VEX_PP_66, /*is_commutative=*/ true);
    }

    public void psubb(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xF8);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void vpsubb(X86_64XmmRegister dst, X86_64XmmRegister add_left, X86_64XmmRegister add_right) {
        EmitVecArithAndLogicalOperation(dst, add_left, add_right, /*opcode=*/ 0xF8, SET_VEX_PP_66);
    }

    public void paddw(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xFD);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void vpaddw(X86_64XmmRegister dst, X86_64XmmRegister add_left, X86_64XmmRegister add_right) {
        EmitVecArithAndLogicalOperation(
                dst, add_left, add_right, /*opcode=*/ 0xFD, SET_VEX_PP_66, /*is_commutative=*/ true);
    }

    public void psubw(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xF9);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void vpsubw(X86_64XmmRegister dst, X86_64XmmRegister add_left, X86_64XmmRegister add_right) {
        EmitVecArithAndLogicalOperation(dst, add_left, add_right, /*opcode=*/ 0xF9, SET_VEX_PP_66);
    }

    public void pmullw(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xD5);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void vpmullw(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2) {
        EmitVecArithAndLogicalOperation(
                dst, src1, src2, /*opcode=*/ 0xD5, SET_VEX_PP_66, /*is_commutative=*/ true);
    }

    public void paddd(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xFE);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void vpaddd(X86_64XmmRegister dst, X86_64XmmRegister add_left, X86_64XmmRegister add_right) {
        EmitVecArithAndLogicalOperation(
                dst, add_left, add_right, /*opcode=*/ 0xFE, SET_VEX_PP_66, /*is_commutative=*/ true);
    }

    public void psubd(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xFA);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void pmulld(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x40);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void vpmulld(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte ByteZero, ByteOne, ByteTwo;
        ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form*/ false);
        X86_64ManagedRegister vvvv_reg =
                X86_64ManagedRegister.fromXmmRegister(src1);
        ByteOne = EmitVexPrefixByteOne(dst.needsRex(),
                /*X=*/ false,
                src2.needsRex(),
                SET_VEX_M_0F_38);
        ByteTwo = EmitVexPrefixByteTwo(/*W=*/ false, vvvv_reg, SET_VEX_L_128, SET_VEX_PP_66);
        emit8(ByteZero);
        emit8(ByteOne);
        emit8(ByteTwo);
        emit8(0x40);
        EmitXmmRegisterOperand(dst.lowBits(), src2);
    }

    public void paddq(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xD4);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void vpaddq(X86_64XmmRegister dst, X86_64XmmRegister add_left, X86_64XmmRegister add_right) {
        EmitVecArithAndLogicalOperation(
                dst, add_left, add_right, /*opcode=*/ 0xD4, SET_VEX_PP_66, /*is_commutative=*/ true);
    }

    public void psubq(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xFB);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void vpsubq(X86_64XmmRegister dst, X86_64XmmRegister add_left, X86_64XmmRegister add_right) {
        EmitVecArithAndLogicalOperation(dst, add_left, add_right, /*opcode=*/ 0xFB, SET_VEX_PP_66);
    }

    public void paddusb(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xDC);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void paddsb(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xEC);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void paddusw(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xDD);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void paddsw(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xED);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void psubusb(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xD8);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void psubsb(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xE8);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void vpsubd(X86_64XmmRegister dst, X86_64XmmRegister add_left, X86_64XmmRegister add_right) {
        EmitVecArithAndLogicalOperation(dst, add_left, add_right, /*opcode=*/ 0xFA, SET_VEX_PP_66);
    }

    public void psubusw(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xD9);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void psubsw(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xE9);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void cvtsi2ss(X86_64XmmRegister dst, X86_64CpuRegister src) {
        cvtsi2ss(dst, src, false);
    }

    public void cvtsi2ss(X86_64XmmRegister dst, X86_64CpuRegister src, boolean is64bit) {
        emit8(0xF3);
        if (is64bit) {
            // Emit a REX.W prefix if the operand size is 64 bits.
            EmitRex64(dst, src);
        } else {
            EmitOptionalRex32(dst, src);
        }
        emit8(0x0F);
        emit8(0x2A);
        EmitOperand(dst.lowBits(), new X86_64Operand(src));
    }

    public void cvtsi2ss(X86_64XmmRegister dst, X86_64Address src, boolean is64bit) {
        emit8(0xF3);
        if (is64bit) {
            // Emit a REX.W prefix if the operand size is 64 bits.
            EmitRex64(dst, src);
        } else {
            EmitOptionalRex32(dst, src);
        }
        emit8(0x0F);
        emit8(0x2A);
        EmitOperand(dst.lowBits(), src);
    }

    public void cvtsi2sd(X86_64XmmRegister dst, X86_64CpuRegister src) {
        cvtsi2sd(dst, src, false);
    }

    public void cvtsi2sd(X86_64XmmRegister dst, X86_64CpuRegister src, boolean is64bit) {
        emit8(0xF2);
        if (is64bit) {
            // Emit a REX.W prefix if the operand size is 64 bits.
            EmitRex64(dst, src);
        } else {
            EmitOptionalRex32(dst, src);
        }
        emit8(0x0F);
        emit8(0x2A);
        EmitOperand(dst.lowBits(), new X86_64Operand(src));
    }

    public void cvtsi2sd(X86_64XmmRegister dst, X86_64Address src, boolean is64bit) {
        emit8(0xF2);
        if (is64bit) {
            // Emit a REX.W prefix if the operand size is 64 bits.
            EmitRex64(dst, src);
        } else {
            EmitOptionalRex32(dst, src);
        }
        emit8(0x0F);
        emit8(0x2A);
        EmitOperand(dst.lowBits(), src);
    }

    public void cvtss2si(X86_64CpuRegister dst, X86_64XmmRegister src) {
        emit8(0xF3);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x2D);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void cvtss2sd(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0xF3);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x5A);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void cvtss2sd(X86_64XmmRegister dst, X86_64Address src) {
        emit8(0xF3);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x5A);
        EmitOperand(dst.lowBits(), src);
    }

    public void cvtsd2si(X86_64CpuRegister dst, X86_64XmmRegister src) {
        emit8(0xF2);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x2D);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void cvttss2si(X86_64CpuRegister dst, X86_64XmmRegister src) {
        cvttss2si(dst, src, false);
    }

    public void cvttss2si(X86_64CpuRegister dst, X86_64XmmRegister src, boolean is64bit) {
        emit8(0xF3);
        if (is64bit) {
            // Emit a REX.W prefix if the operand size is 64 bits.
            EmitRex64(dst, src);
        } else {
            EmitOptionalRex32(dst, src);
        }
        emit8(0x0F);
        emit8(0x2C);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void cvttsd2si(X86_64CpuRegister dst, X86_64XmmRegister src) {
        cvttsd2si(dst, src, false);
    }

    public void cvttsd2si(X86_64CpuRegister dst, X86_64XmmRegister src, boolean is64bit) {
        emit8(0xF2);
        if (is64bit) {
            // Emit a REX.W prefix if the operand size is 64 bits.
            EmitRex64(dst, src);
        } else {
            EmitOptionalRex32(dst, src);
        }
        emit8(0x0F);
        emit8(0x2C);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void cvtsd2ss(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0xF2);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x5A);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void cvtsd2ss(X86_64XmmRegister dst, X86_64Address src) {
        emit8(0xF2);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x5A);
        EmitOperand(dst.lowBits(), src);
    }

    public void cvtdq2ps(X86_64XmmRegister dst, X86_64XmmRegister src) {
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x5B);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void cvtdq2pd(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0xF3);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xE6);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void comiss(X86_64XmmRegister a, X86_64XmmRegister b) {
        EmitOptionalRex32(a, b);
        emit8(0x0F);
        emit8(0x2F);
        EmitXmmRegisterOperand(a.lowBits(), b);
    }

    public void comiss(X86_64XmmRegister a, X86_64Address b) {
        EmitOptionalRex32(a, b);
        emit8(0x0F);
        emit8(0x2F);
        EmitOperand(a.lowBits(), b);
    }

    public void comisd(X86_64XmmRegister a, X86_64XmmRegister b) {
        emit8(0x66);
        EmitOptionalRex32(a, b);
        emit8(0x0F);
        emit8(0x2F);
        EmitXmmRegisterOperand(a.lowBits(), b);
    }

    public void comisd(X86_64XmmRegister a, X86_64Address b) {
        emit8(0x66);
        EmitOptionalRex32(a, b);
        emit8(0x0F);
        emit8(0x2F);
        EmitOperand(a.lowBits(), b);
    }

    public void ucomiss(X86_64XmmRegister a, X86_64XmmRegister b) {
        EmitOptionalRex32(a, b);
        emit8(0x0F);
        emit8(0x2E);
        EmitXmmRegisterOperand(a.lowBits(), b);
    }

    public void ucomiss(X86_64XmmRegister a, X86_64Address b) {
        EmitOptionalRex32(a, b);
        emit8(0x0F);
        emit8(0x2E);
        EmitOperand(a.lowBits(), b);
    }

    public void ucomisd(X86_64XmmRegister a, X86_64XmmRegister b) {
        emit8(0x66);
        EmitOptionalRex32(a, b);
        emit8(0x0F);
        emit8(0x2E);
        EmitXmmRegisterOperand(a.lowBits(), b);
    }

    public void ucomisd(X86_64XmmRegister a, X86_64Address b) {
        emit8(0x66);
        EmitOptionalRex32(a, b);
        emit8(0x0F);
        emit8(0x2E);
        EmitOperand(a.lowBits(), b);
    }

    public void roundsd(X86_64XmmRegister dst, X86_64XmmRegister src, X86_64Immediate imm) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x3A);
        emit8(0x0B);
        EmitXmmRegisterOperand(dst.lowBits(), src);
        emit8((int) imm.value());
    }

    public void roundss(X86_64XmmRegister dst, X86_64XmmRegister src, X86_64Immediate imm) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x3A);
        emit8(0x0A);
        EmitXmmRegisterOperand(dst.lowBits(), src);
        emit8((int) imm.value());
    }

    public void sqrtsd(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0xF2);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x51);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void sqrtss(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0xF3);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x51);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void xorpd(X86_64XmmRegister dst, X86_64Address src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x57);
        EmitOperand(dst.lowBits(), src);
    }

    public void xorpd(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x57);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void xorps(X86_64XmmRegister dst, X86_64Address src) {
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x57);
        EmitOperand(dst.lowBits(), src);
    }

    public void xorps(X86_64XmmRegister dst, X86_64XmmRegister src) {
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x57);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void pxor(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xEF);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    /* VEX.128.66.0F.WIG EF /r VPXOR xmm1, xmm2, xmm3/m128 */
    public void vpxor(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2) {
        EmitVecArithAndLogicalOperation(
                dst, src1, src2, /*opcode=*/ 0xEF, SET_VEX_PP_66, /*is_commutative=*/ true);
    }

    /* VEX.128.0F.WIG 57 /r VXORPS xmm1,xmm2, xmm3/m128 */
    public void vxorps(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2) {
        EmitVecArithAndLogicalOperation(
                dst, src1, src2, /*opcode=*/ 0x57, SET_VEX_PP_NONE, /*is_commutative=*/ true);
    }

    /* VEX.128.66.0F.WIG 57 /r VXORPD xmm1,xmm2, xmm3/m128 */
    public void vxorpd(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2) {
        EmitVecArithAndLogicalOperation(
                dst, src1, src2, /*opcode=*/ 0x57, SET_VEX_PP_66, /*is_commutative=*/ true);
    }

    public void andpd(X86_64XmmRegister dst, X86_64Address src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x54);
        EmitOperand(dst.lowBits(), src);
    }

    public void andpd(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x54);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void andps(X86_64XmmRegister dst, X86_64XmmRegister src) {
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x54);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void pand(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xDB);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    /* VEX.128.66.0F.WIG DB /r VPAND xmm1, xmm2, xmm3/m128 */
    public void vpand(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2) {
        EmitVecArithAndLogicalOperation(
                dst, src1, src2, /*opcode=*/ 0xDB, SET_VEX_PP_66, /*is_commutative=*/ true);
    }

    /* VEX.128.0F 54 /r VANDPS xmm1,xmm2, xmm3/m128 */
    public void vandps(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2) {
        EmitVecArithAndLogicalOperation(
                dst, src1, src2, /*opcode=*/ 0x54, SET_VEX_PP_NONE, /*is_commutative=*/ true);
    }

    /* VEX.128.66.0F 54 /r VANDPD xmm1, xmm2, xmm3/m128 */
    public void vandpd(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2) {
        EmitVecArithAndLogicalOperation(
                dst, src1, src2, /*opcode=*/ 0x54, SET_VEX_PP_66, /*is_commutative=*/ true);
    }

    public void andn(X86_64CpuRegister dst, X86_64CpuRegister src1, X86_64CpuRegister src2) {
        byte byte_zero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ false);
        byte byte_one = EmitVexPrefixByteOne(dst.needsRex(),
                /*X=*/ false,
                src2.needsRex(),
                SET_VEX_M_0F_38);
        byte byte_two = EmitVexPrefixByteTwo(/*W=*/ true,
                X86_64ManagedRegister.fromCpuRegister(src1),
                SET_VEX_L_128,
                SET_VEX_PP_NONE);
        emit8(byte_zero);
        emit8(byte_one);
        emit8(byte_two);
        // Opcode field
        emit8(0xF2);
        EmitRegisterOperand(dst.lowBits(), src2.lowBits());
    }

    public void andnpd(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x55);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void andnps(X86_64XmmRegister dst, X86_64XmmRegister src) {
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x55);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void pandn(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xDF);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    /* VEX.128.66.0F.WIG DF /r VPANDN xmm1, xmm2, xmm3/m128 */
    public void vpandn(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2) {
        EmitVecArithAndLogicalOperation(dst, src1, src2, /*opcode=*/ 0xDF, SET_VEX_PP_66);
    }

    /* VEX.128.0F 55 /r VANDNPS xmm1, xmm2, xmm3/m128 */
    public void vandnps(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2) {
        EmitVecArithAndLogicalOperation(dst, src1, src2, /*opcode=*/ 0x55, SET_VEX_PP_NONE);
    }

    /* VEX.128.66.0F 55 /r VANDNPD xmm1, xmm2, xmm3/m128 */
    public void vandnpd(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2) {
        EmitVecArithAndLogicalOperation(dst, src1, src2, /*opcode=*/ 0x55, SET_VEX_PP_66);
    }

    public void orpd(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x56);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void orps(X86_64XmmRegister dst, X86_64XmmRegister src) {
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x56);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void por(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xEB);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    /* VEX.128.66.0F.WIG EB /r VPOR xmm1, xmm2, xmm3/m128 */
    public void vpor(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2) {
        EmitVecArithAndLogicalOperation(
                dst, src1, src2, /*opcode=*/ 0xEB, SET_VEX_PP_66, /*is_commutative=*/ true);
    }

    /* VEX.128.0F 56 /r VORPS xmm1,xmm2, xmm3/m128 */
    public void vorps(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2) {
        EmitVecArithAndLogicalOperation(
                dst, src1, src2, /*opcode=*/ 0x56, SET_VEX_PP_NONE, /*is_commutative=*/ true);
    }

    /* VEX.128.66.0F 56 /r VORPD xmm1,xmm2, xmm3/m128 */
    public void vorpd(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2) {
        EmitVecArithAndLogicalOperation(
                dst, src1, src2, /*opcode=*/ 0x56, SET_VEX_PP_66, /*is_commutative=*/ true);
    }

    public void pavgb(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xE0);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void pavgw(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xE3);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void psadbw(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xF6);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void pmaddwd(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xF5);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void vpmaddwd(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2) {
        EmitVecArithAndLogicalOperation(
                dst, src1, src2, /*opcode=*/ 0xF5, SET_VEX_PP_66, /*is_commutative=*/ true);
    }

    public void phaddw(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x01);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void phaddd(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x02);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void haddps(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0xF2);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x7C);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void haddpd(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x7C);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void phsubw(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x05);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void phsubd(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x06);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void hsubps(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0xF2);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x7D);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void hsubpd(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x7D);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void pminsb(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x38);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void pmaxsb(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x3C);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void pminsw(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xEA);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void pmaxsw(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xEE);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void pminsd(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x39);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void pmaxsd(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x3D);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void pminub(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xDA);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void pmaxub(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xDE);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void pminuw(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x3A);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void pmaxuw(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x3E);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void pminud(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x3B);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void pmaxud(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x3F);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void minps(X86_64XmmRegister dst, X86_64XmmRegister src) {
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x5D);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void maxps(X86_64XmmRegister dst, X86_64XmmRegister src) {
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x5F);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void minpd(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x5D);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void maxpd(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x5F);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void pcmpeqb(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x74);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void pcmpeqw(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x75);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void pcmpeqd(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x76);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void pcmpeqq(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x29);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void pcmpgtb(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x64);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void pcmpgtw(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x65);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void pcmpgtd(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x66);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void pcmpgtq(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x37);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void shufpd(X86_64XmmRegister dst, X86_64XmmRegister src, X86_64Immediate imm) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xC6);
        EmitXmmRegisterOperand(dst.lowBits(), src);
        emit8((int) imm.value());
    }

    public void shufps(X86_64XmmRegister dst, X86_64XmmRegister src, X86_64Immediate imm) {
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xC6);
        EmitXmmRegisterOperand(dst.lowBits(), src);
        emit8((int) imm.value());
    }

    public void pshufd(X86_64XmmRegister dst, X86_64XmmRegister src, X86_64Immediate imm) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x70);
        EmitXmmRegisterOperand(dst.lowBits(), src);
        emit8((int) imm.value());
    }

    public void punpcklbw(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x60);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void punpcklwd(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x61);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void punpckldq(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x62);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void punpcklqdq(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x6C);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void punpckhbw(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x68);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void punpckhwd(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x69);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void punpckhdq(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x6A);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void punpckhqdq(X86_64XmmRegister dst, X86_64XmmRegister src) {
        emit8(0x66);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0x6D);
        EmitXmmRegisterOperand(dst.lowBits(), src);
    }

    public void psllw(X86_64XmmRegister reg, X86_64Immediate shift_count) {
        CHECK(shift_count.isUInt8());
        emit8(0x66);
        EmitOptionalRex(false, false, false, false, reg.needsRex());
        emit8(0x0F);
        emit8(0x71);
        EmitXmmRegisterOperand(6, reg);
        emit8((int) shift_count.value());
    }

    public void pslld(X86_64XmmRegister reg, X86_64Immediate shift_count) {
        CHECK(shift_count.isUInt8());
        emit8(0x66);
        EmitOptionalRex(false, false, false, false, reg.needsRex());
        emit8(0x0F);
        emit8(0x72);
        EmitXmmRegisterOperand(6, reg);
        emit8((int) shift_count.value());
    }

    public void psllq(X86_64XmmRegister reg, X86_64Immediate shift_count) {
        CHECK(shift_count.isUInt8());
        emit8(0x66);
        EmitOptionalRex(false, false, false, false, reg.needsRex());
        emit8(0x0F);
        emit8(0x73);
        EmitXmmRegisterOperand(6, reg);
        emit8((int) shift_count.value());
    }

    public void psraw(X86_64XmmRegister reg, X86_64Immediate shift_count) {
        CHECK(shift_count.isUInt8());
        emit8(0x66);
        EmitOptionalRex(false, false, false, false, reg.needsRex());
        emit8(0x0F);
        emit8(0x71);
        EmitXmmRegisterOperand(4, reg);
        emit8((int) shift_count.value());
    }

    public void psrad(X86_64XmmRegister reg, X86_64Immediate shift_count) {
        CHECK(shift_count.isUInt8());
        emit8(0x66);
        EmitOptionalRex(false, false, false, false, reg.needsRex());
        emit8(0x0F);
        emit8(0x72);
        EmitXmmRegisterOperand(4, reg);
        emit8((int) shift_count.value());
    }

    public void psrlw(X86_64XmmRegister reg, X86_64Immediate shift_count) {
        CHECK(shift_count.isUInt8());
        emit8(0x66);
        EmitOptionalRex(false, false, false, false, reg.needsRex());
        emit8(0x0F);
        emit8(0x71);
        EmitXmmRegisterOperand(2, reg);
        emit8((int) shift_count.value());
    }

    public void psrld(X86_64XmmRegister reg, X86_64Immediate shift_count) {
        CHECK(shift_count.isUInt8());
        emit8(0x66);
        EmitOptionalRex(false, false, false, false, reg.needsRex());
        emit8(0x0F);
        emit8(0x72);
        EmitXmmRegisterOperand(2, reg);
        emit8((int) shift_count.value());
    }

    public void psrlq(X86_64XmmRegister reg, X86_64Immediate shift_count) {
        CHECK(shift_count.isUInt8());
        emit8(0x66);
        EmitOptionalRex(false, false, false, false, reg.needsRex());
        emit8(0x0F);
        emit8(0x73);
        EmitXmmRegisterOperand(2, reg);
        emit8((int) shift_count.value());
    }

    public void psrldq(X86_64XmmRegister reg, X86_64Immediate shift_count) {
        CHECK(shift_count.isUInt8());
        emit8(0x66);
        EmitOptionalRex(false, false, false, false, reg.needsRex());
        emit8(0x0F);
        emit8(0x73);
        EmitXmmRegisterOperand(3, reg);
        emit8((int) shift_count.value());
    }

    public void fldl(X86_64Address src) {
        emit8(0xDD);
        EmitOperand(0, src);
    }

    public void fstl(X86_64Address dst) {
        emit8(0xDD);
        EmitOperand(2, dst);
    }

    public void fstpl(X86_64Address dst) {
        emit8(0xDD);
        EmitOperand(3, dst);
    }

    public void fstsw() {
        emit8(0x9B);
        emit8(0xDF);
        emit8(0xE0);
    }

    public void fnstcw(X86_64Address dst) {
        emit8(0xD9);
        EmitOperand(7, dst);
    }

    public void fldcw(X86_64Address src) {
        emit8(0xD9);
        EmitOperand(5, src);
    }

    public void fistpl(X86_64Address dst) {
        emit8(0xDF);
        EmitOperand(7, dst);
    }

    public void fistps(X86_64Address dst) {
        emit8(0xDB);
        EmitOperand(3, dst);
    }

    public void fildl(X86_64Address src) {
        emit8(0xDF);
        EmitOperand(5, src);
    }

    public void filds(X86_64Address src) {
        emit8(0xDB);
        EmitOperand(0, src);
    }

    public void fincstp() {
        emit8(0xD9);
        emit8(0xF7);
    }

    public void ffree(X86_64Immediate index) {
        CHECK_LT(index.value(), 7);
        emit8(0xDD);
        emit8((int) (0xC0 + index.value()));
    }

    public void fsin() {
        emit8(0xD9);
        emit8(0xFE);
    }

    public void fcos() {
        emit8(0xD9);
        emit8(0xFF);
    }

    public void fptan() {
        emit8(0xD9);
        emit8(0xF2);
    }

    public void fucompp() {
        emit8(0xDA);
        emit8(0xE9);
    }

    public void fprem() {
        emit8(0xD9);
        emit8(0xF8);
    }

    public boolean try_xchg_rax(X86_64CpuRegister dst, X86_64CpuRegister src, Consumer<X86_64CpuRegister> prefix_fn) {
        if (src != RAX && dst != RAX) {
            return false;
        }
        if (dst == RAX) {
            dst = src;
        }
        if (dst != RAX) {
            // Prefix is needed only if one of the registers is not RAX, otherwise it's a pure NOP.
            prefix_fn.accept(dst);
        }
        emit8(0x90 + dst.lowBits());
        return true;
    }

    public void xchgb(X86_64CpuRegister dst, X86_64CpuRegister src) {
        // There is no short version for AL.
        EmitOptionalByteRegNormalizingRex32(dst, src, /*normalize_both=*/ true);
        emit8(0x86);
        EmitRegisterOperand(dst.lowBits(), src.lowBits());
    }

    public void xchgb(X86_64CpuRegister reg, X86_64Address address) {
        EmitOptionalByteRegNormalizingRex32(reg, address);
        emit8(0x86);
        EmitOperand(reg.lowBits(), address);
    }

    public void xchgw(X86_64CpuRegister dst, X86_64CpuRegister src) {
        EmitOperandSizeOverride();
        if (try_xchg_rax(dst, src, this::EmitOptionalRex32)) {
            // A short version for AX.
            return;
        }
        // General case.
        EmitOptionalRex32(dst, src);
        emit8(0x87);
        EmitRegisterOperand(dst.lowBits(), src.lowBits());
    }

    public void xchgw(X86_64CpuRegister reg, X86_64Address address) {
        EmitOperandSizeOverride();
        EmitOptionalRex32(reg, address);
        emit8(0x87);
        EmitOperand(reg.lowBits(), address);
    }

    public void xchgl(X86_64CpuRegister dst, X86_64CpuRegister src) {
        if (try_xchg_rax(dst, src, this::EmitOptionalRex32)) {
            // A short version for EAX.
            return;
        }
        // General case.
        EmitOptionalRex32(dst, src);
        emit8(0x87);
        EmitRegisterOperand(dst.lowBits(), src.lowBits());
    }

    public void xchgl(X86_64CpuRegister reg, X86_64Address address) {
        EmitOptionalRex32(reg, address);
        emit8(0x87);
        EmitOperand(reg.lowBits(), address);
    }

    public void xchgq(X86_64CpuRegister dst, X86_64CpuRegister src) {
        if (try_xchg_rax(dst, src, this::EmitRex64)) {
            // A short version for RAX.
            return;
        }
        // General case.
        EmitRex64(dst, src);
        emit8(0x87);
        EmitRegisterOperand(dst.lowBits(), src.lowBits());
    }

    public void xchgq(X86_64CpuRegister reg, X86_64Address address) {
        EmitRex64(reg, address);
        emit8(0x87);
        EmitOperand(reg.lowBits(), address);
    }

    public void xaddb(X86_64CpuRegister dst, X86_64CpuRegister src) {
        EmitOptionalByteRegNormalizingRex32(src, dst, /*normalize_both=*/ true);
        emit8(0x0F);
        emit8(0xC0);
        EmitRegisterOperand(src.lowBits(), dst.lowBits());
    }

    public void xaddb(X86_64Address address, X86_64CpuRegister reg) {
        EmitOptionalByteRegNormalizingRex32(reg, address);
        emit8(0x0F);
        emit8(0xC0);
        EmitOperand(reg.lowBits(), address);
    }

    public void xaddw(X86_64CpuRegister dst, X86_64CpuRegister src) {
        EmitOperandSizeOverride();
        EmitOptionalRex32(src, dst);
        emit8(0x0F);
        emit8(0xC1);
        EmitRegisterOperand(src.lowBits(), dst.lowBits());
    }

    public void xaddw(X86_64Address address, X86_64CpuRegister reg) {
        EmitOperandSizeOverride();
        EmitOptionalRex32(reg, address);
        emit8(0x0F);
        emit8(0xC1);
        EmitOperand(reg.lowBits(), address);
    }

    public void xaddl(X86_64CpuRegister dst, X86_64CpuRegister src) {
        EmitOptionalRex32(src, dst);
        emit8(0x0F);
        emit8(0xC1);
        EmitRegisterOperand(src.lowBits(), dst.lowBits());
    }

    public void xaddl(X86_64Address address, X86_64CpuRegister reg) {
        EmitOptionalRex32(reg, address);
        emit8(0x0F);
        emit8(0xC1);
        EmitOperand(reg.lowBits(), address);
    }

    public void xaddq(X86_64CpuRegister dst, X86_64CpuRegister src) {
        EmitRex64(src, dst);
        emit8(0x0F);
        emit8(0xC1);
        EmitRegisterOperand(src.lowBits(), dst.lowBits());
    }

    public void xaddq(X86_64Address address, X86_64CpuRegister reg) {
        EmitRex64(reg, address);
        emit8(0x0F);
        emit8(0xC1);
        EmitOperand(reg.lowBits(), address);
    }

    public void cmpb(X86_64Address address, X86_64Immediate imm) {
        CHECK(imm.isInt32());
        EmitOptionalRex32(address);
        emit8(0x80);
        EmitOperand(7, address);
        emit8((int) imm.value() & 0xFF);
    }

    public void cmpw(X86_64Address address, X86_64Immediate imm) {
        CHECK(imm.isInt32());
        EmitOperandSizeOverride();
        EmitOptionalRex32(address);
        EmitComplex(7, address, imm, /* is_16_op= */ true);
    }

    public void cmpl(X86_64CpuRegister reg, X86_64Immediate imm) {
        CHECK(imm.isInt32());
        EmitOptionalRex32(reg);
        EmitComplex(7, new X86_64Operand(reg), imm);
    }

    public void cmpl(X86_64CpuRegister reg0, X86_64CpuRegister reg1) {
        EmitOptionalRex32(reg0, reg1);
        emit8(0x3B);
        EmitOperand(reg0.lowBits(), new X86_64Operand(reg1));
    }

    public void cmpl(X86_64CpuRegister reg, X86_64Address address) {
        EmitOptionalRex32(reg, address);
        emit8(0x3B);
        EmitOperand(reg.lowBits(), address);
    }

    public void cmpl(X86_64Address address, X86_64CpuRegister reg) {
        EmitOptionalRex32(reg, address);
        emit8(0x39);
        EmitOperand(reg.lowBits(), address);
    }

    public void cmpl(X86_64Address address, X86_64Immediate imm) {
        CHECK(imm.isInt32());
        EmitOptionalRex32(address);
        EmitComplex(7, address, imm);
    }

    public void cmpq(X86_64CpuRegister reg0, X86_64CpuRegister reg1) {
        EmitRex64(reg0, reg1);
        emit8(0x3B);
        EmitOperand(reg0.lowBits(), new X86_64Operand(reg1));
    }

    public void cmpq(X86_64CpuRegister reg, X86_64Immediate imm) {
        CHECK(imm.isInt32());  // cmpq only supports 32b immediate.
        EmitRex64(reg);
        EmitComplex(7, new X86_64Operand(reg), imm);
    }

    public void cmpq(X86_64CpuRegister reg, X86_64Address address) {
        EmitRex64(reg, address);
        emit8(0x3B);
        EmitOperand(reg.lowBits(), address);
    }

    public void cmpq(X86_64Address address, X86_64Immediate imm) {
        CHECK(imm.isInt32());  // cmpq only supports 32b immediate.
        EmitRex64(address);
        EmitComplex(7, address, imm);
    }

    public void addl(X86_64CpuRegister dst, X86_64CpuRegister src) {
        EmitOptionalRex32(dst, src);
        emit8(0x03);
        EmitRegisterOperand(dst.lowBits(), src.lowBits());
    }

    public void addl(X86_64CpuRegister reg, X86_64Address address) {
        EmitOptionalRex32(reg, address);
        emit8(0x03);
        EmitOperand(reg.lowBits(), address);
    }

    public void testl(X86_64CpuRegister reg1, X86_64CpuRegister reg2) {
        EmitOptionalRex32(reg1, reg2);
        emit8(0x85);
        EmitRegisterOperand(reg1.lowBits(), reg2.lowBits());
    }

    public void testl(X86_64CpuRegister reg, X86_64Address address) {
        EmitOptionalRex32(reg, address);
        emit8(0x85);
        EmitOperand(reg.lowBits(), address);
    }

    public void testl(X86_64CpuRegister reg, X86_64Immediate immediate) {
        // For registers that have a byte variant (RAX, RBX, RCX, and RDX)
        // we only test the byte X86_64CpuRegister to keep the encoding short.
        if (immediate.isUInt8() && reg.index() < 4) {
            // Use zero-extended 8-bit immediate.
            if (reg == RAX) {
                emit8(0xA8);
            } else {
                emit8(0xF6);
                emit8(0xC0 + reg.index());
            }
            emit8((int) (immediate.value() & 0xFF));
        } else if (reg == RAX) {
            // Use short form if the destination is RAX.
            emit8(0xA9);
            EmitImmediate(immediate);
        } else {
            EmitOptionalRex32(reg);
            emit8(0xF7);
            EmitOperand(0, new X86_64Operand(reg));
            EmitImmediate(immediate);
        }
    }

    public void testq(X86_64CpuRegister reg1, X86_64CpuRegister reg2) {
        EmitRex64(reg1, reg2);
        emit8(0x85);
        EmitRegisterOperand(reg1.lowBits(), reg2.lowBits());
    }

    public void testq(X86_64CpuRegister reg, X86_64Address address) {
        EmitRex64(reg, address);
        emit8(0x85);
        EmitOperand(reg.lowBits(), address);
    }

    public void testb(X86_64Address dst, X86_64Immediate imm) {
        EmitOptionalRex32(dst);
        emit8(0xF6);
        EmitOperand(RAX.index(), dst);
        CHECK(imm.isInt8());
        emit8((int) imm.value() & 0xFF);
    }

    public void testl(X86_64Address dst, X86_64Immediate imm) {
        EmitOptionalRex32(dst);
        emit8(0xF7);
        EmitOperand(0, dst);
        EmitImmediate(imm);
    }

    public void andl(X86_64CpuRegister dst, X86_64CpuRegister src) {
        EmitOptionalRex32(dst, src);
        emit8(0x23);
        EmitOperand(dst.lowBits(), new X86_64Operand(src));
    }

    public void andl(X86_64CpuRegister reg, X86_64Address address) {
        EmitOptionalRex32(reg, address);
        emit8(0x23);
        EmitOperand(reg.lowBits(), address);
    }

    public void andl(X86_64CpuRegister dst, X86_64Immediate imm) {
        CHECK(imm.isInt32());  // andl only supports 32b immediate.
        EmitOptionalRex32(dst);
        EmitComplex(4, new X86_64Operand(dst), imm);
    }

    public void andq(X86_64CpuRegister reg, X86_64Immediate imm) {
        CHECK(imm.isInt32());  // andq only supports 32b immediate.
        EmitRex64(reg);
        EmitComplex(4, new X86_64Operand(reg), imm);
    }

    public void andq(X86_64CpuRegister dst, X86_64CpuRegister src) {
        EmitRex64(dst, src);
        emit8(0x23);
        EmitOperand(dst.lowBits(), new X86_64Operand(src));
    }

    public void andq(X86_64CpuRegister dst, X86_64Address src) {
        EmitRex64(dst, src);
        emit8(0x23);
        EmitOperand(dst.lowBits(), src);
    }

    public void andw(X86_64Address address, X86_64Immediate imm) {
        CHECK(imm.isUInt16() || imm.isInt16());
        emit8(0x66);
        EmitOptionalRex32(address);
        EmitComplex(4, address, imm, /* is_16_op= */ true);
    }

    public void orl(X86_64CpuRegister dst, X86_64CpuRegister src) {
        EmitOptionalRex32(dst, src);
        emit8(0x0B);
        EmitOperand(dst.lowBits(), new X86_64Operand(src));
    }

    public void orl(X86_64CpuRegister reg, X86_64Address address) {
        EmitOptionalRex32(reg, address);
        emit8(0x0B);
        EmitOperand(reg.lowBits(), address);
    }

    public void orl(X86_64CpuRegister dst, X86_64Immediate imm) {
        EmitOptionalRex32(dst);
        EmitComplex(1, new X86_64Operand(dst), imm);
    }

    public void orq(X86_64CpuRegister dst, X86_64Immediate imm) {
        CHECK(imm.isInt32());  // orq only supports 32b immediate.
        EmitRex64(dst);
        EmitComplex(1, new X86_64Operand(dst), imm);
    }

    public void orq(X86_64CpuRegister dst, X86_64CpuRegister src) {
        EmitRex64(dst, src);
        emit8(0x0B);
        EmitOperand(dst.lowBits(), new X86_64Operand(src));
    }

    public void orq(X86_64CpuRegister dst, X86_64Address src) {
        EmitRex64(dst, src);
        emit8(0x0B);
        EmitOperand(dst.lowBits(), src);
    }

    public void xorl(X86_64CpuRegister dst, X86_64CpuRegister src) {
        EmitOptionalRex32(dst, src);
        emit8(0x33);
        EmitOperand(dst.lowBits(), new X86_64Operand(src));
    }

    public void xorl(X86_64CpuRegister reg, X86_64Address address) {
        EmitOptionalRex32(reg, address);
        emit8(0x33);
        EmitOperand(reg.lowBits(), address);
    }

    public void xorl(X86_64CpuRegister dst, X86_64Immediate imm) {
        EmitOptionalRex32(dst);
        EmitComplex(6, new X86_64Operand(dst), imm);
    }

    public void xorq(X86_64CpuRegister dst, X86_64CpuRegister src) {
        EmitRex64(dst, src);
        emit8(0x33);
        EmitOperand(dst.lowBits(), new X86_64Operand(src));
    }

    public void xorq(X86_64CpuRegister dst, X86_64Immediate imm) {
        CHECK(imm.isInt32());  // xorq only supports 32b immediate.
        EmitRex64(dst);
        EmitComplex(6, new X86_64Operand(dst), imm);
    }

    public void xorq(X86_64CpuRegister dst, X86_64Address src) {
        EmitRex64(dst, src);
        emit8(0x33);
        EmitOperand(dst.lowBits(), src);
    }

    public void addl(X86_64CpuRegister reg, X86_64Immediate imm) {
        EmitOptionalRex32(reg);
        EmitComplex(0, new X86_64Operand(reg), imm);
    }

    public void addw(X86_64CpuRegister reg, X86_64Immediate imm) {
        CHECK(imm.isUInt16() || imm.isInt16());
        emit8(0x66);
        EmitOptionalRex32(reg);
        EmitComplex(0, new X86_64Operand(reg), imm, /* is_16_op= */ true);
    }

    public void addq(X86_64CpuRegister reg, X86_64Immediate imm) {
        CHECK(imm.isInt32());  // addq only supports 32b immediate.
        EmitRex64(reg);
        EmitComplex(0, new X86_64Operand(reg), imm);
    }

    public void addq(X86_64CpuRegister dst, X86_64Address address) {
        EmitRex64(dst, address);
        emit8(0x03);
        EmitOperand(dst.lowBits(), address);
    }

    public void addq(X86_64CpuRegister dst, X86_64CpuRegister src) {
        // 0x01 is addq r/m64 <- r/m64 + r64, with op1 in r/m and op2 in reg: so reverse EmitRex64
        EmitRex64(src, dst);
        emit8(0x01);
        EmitRegisterOperand(src.lowBits(), dst.lowBits());
    }

    public void addl(X86_64Address address, X86_64CpuRegister reg) {
        EmitOptionalRex32(reg, address);
        emit8(0x01);
        EmitOperand(reg.lowBits(), address);
    }

    public void addl(X86_64Address address, X86_64Immediate imm) {
        EmitOptionalRex32(address);
        EmitComplex(0, address, imm);
    }

    public void addw(X86_64Address address, X86_64Immediate imm) {
        CHECK(imm.isUInt16() || imm.isInt16());
        emit8(0x66);
        EmitOptionalRex32(address);
        EmitComplex(0, address, imm, /* is_16_op= */ true);
    }

    public void addw(X86_64Address address, X86_64CpuRegister reg) {
        EmitOperandSizeOverride();
        EmitOptionalRex32(reg, address);
        emit8(0x01);
        EmitOperand(reg.lowBits(), address);
    }

    public void subl(X86_64CpuRegister dst, X86_64CpuRegister src) {
        EmitOptionalRex32(dst, src);
        emit8(0x2B);
        EmitOperand(dst.lowBits(), new X86_64Operand(src));
    }

    public void subl(X86_64CpuRegister reg, X86_64Immediate imm) {
        EmitOptionalRex32(reg);
        EmitComplex(5, new X86_64Operand(reg), imm);
    }

    public void subq(X86_64CpuRegister reg, X86_64Immediate imm) {
        CHECK(imm.isInt32());  // subq only supports 32b immediate.
        EmitRex64(reg);
        EmitComplex(5, new X86_64Operand(reg), imm);
    }

    public void subq(X86_64CpuRegister dst, X86_64CpuRegister src) {
        EmitRex64(dst, src);
        emit8(0x2B);
        EmitRegisterOperand(dst.lowBits(), src.lowBits());
    }

    public void subq(X86_64CpuRegister reg, X86_64Address address) {
        EmitRex64(reg, address);
        emit8(0x2B);
        EmitOperand(reg.lowBits() & 7, address);
    }

    public void subl(X86_64CpuRegister reg, X86_64Address address) {
        EmitOptionalRex32(reg, address);
        emit8(0x2B);
        EmitOperand(reg.lowBits(), address);
    }

    public void cdq() {
        emit8(0x99);
    }

    public void cqo() {
        EmitRex64();
        emit8(0x99);
    }

    public void idivl(X86_64CpuRegister reg) {
        EmitOptionalRex32(reg);
        emit8(0xF7);
        emit8(0xF8 | reg.lowBits());
    }

    public void idivq(X86_64CpuRegister reg) {
        EmitRex64(reg);
        emit8(0xF7);
        emit8(0xF8 | reg.lowBits());
    }

    public void divl(X86_64CpuRegister reg) {
        EmitOptionalRex32(reg);
        emit8(0xF7);
        emit8(0xF0 | reg.lowBits());
    }

    public void divq(X86_64CpuRegister reg) {
        EmitRex64(reg);
        emit8(0xF7);
        emit8(0xF0 | reg.lowBits());
    }

    public void imull(X86_64CpuRegister dst, X86_64CpuRegister src) {
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xAF);
        EmitOperand(dst.lowBits(), new X86_64Operand(src));
    }

    public void imull(X86_64CpuRegister dst, X86_64CpuRegister src, X86_64Immediate imm) {
        CHECK(imm.isInt32());  // imull only supports 32b immediate.
        EmitOptionalRex32(dst, src);
        // See whether imm can be represented as a sign-extended 8bit value.
        int v32 = (int) (imm.value());
        if (Utils.isInt8(v32)) {
            // Sign-extension works.
            emit8(0x6B);
            EmitOperand(dst.lowBits(), new X86_64Operand(src));
            emit8(v32 & 0xFF);
        } else {
            // Not representable, use full immediate.
            emit8(0x69);
            EmitOperand(dst.lowBits(), new X86_64Operand(src));
            EmitImmediate(imm);
        }
    }

    public void imull(X86_64CpuRegister reg, X86_64Immediate imm) {
        imull(reg, reg, imm);
    }

    public void imull(X86_64CpuRegister reg, X86_64Address address) {
        EmitOptionalRex32(reg, address);
        emit8(0x0F);
        emit8(0xAF);
        EmitOperand(reg.lowBits(), address);
    }

    public void imulq(X86_64CpuRegister dst, X86_64CpuRegister src) {
        EmitRex64(dst, src);
        emit8(0x0F);
        emit8(0xAF);
        EmitRegisterOperand(dst.lowBits(), src.lowBits());
    }

    public void imulq(X86_64CpuRegister reg, X86_64Immediate imm) {
        imulq(reg, reg, imm);
    }

    public void imulq(X86_64CpuRegister dst, X86_64CpuRegister reg, X86_64Immediate imm) {
        CHECK(imm.isInt32());  // imulq only supports 32b immediate.
        EmitRex64(dst, reg);
        // See whether imm can be represented as a sign-extended 8bit value.
        long v64 = imm.value();
        if (Utils.isInt8(v64)) {
            // Sign-extension works.
            emit8(0x6B);
            EmitOperand(dst.lowBits(), new X86_64Operand(reg));
            emit8((int) (v64 & 0xFF));
        } else {
            // Not representable, use full immediate.
            emit8(0x69);
            EmitOperand(dst.lowBits(), new X86_64Operand(reg));
            EmitImmediate(imm);
        }
    }

    public void imulq(X86_64CpuRegister reg, X86_64Address address) {
        EmitRex64(reg, address);
        emit8(0x0F);
        emit8(0xAF);
        EmitOperand(reg.lowBits(), address);
    }

    public void imull(X86_64CpuRegister reg) {
        EmitOptionalRex32(reg);
        emit8(0xF7);
        EmitOperand(5, new X86_64Operand(reg));
    }

    public void imulq(X86_64CpuRegister reg) {
        EmitRex64(reg);
        emit8(0xF7);
        EmitOperand(5, new X86_64Operand(reg));
    }

    public void imull(X86_64Address address) {
        EmitOptionalRex32(address);
        emit8(0xF7);
        EmitOperand(5, address);
    }

    public void mull(X86_64CpuRegister reg) {
        EmitOptionalRex32(reg);
        emit8(0xF7);
        EmitOperand(4, new X86_64Operand(reg));
    }

    public void mull(X86_64Address address) {
        EmitOptionalRex32(address);
        emit8(0xF7);
        EmitOperand(4, address);
    }

    public void shll(X86_64CpuRegister reg, X86_64Immediate imm) {
        EmitGenericShift(false, 4, reg, imm);
    }

    public void shlq(X86_64CpuRegister reg, X86_64Immediate imm) {
        EmitGenericShift(true, 4, reg, imm);
    }

    public void shll(X86_64CpuRegister operand, X86_64CpuRegister shifter) {
        EmitGenericShift(false, 4, operand, shifter);
    }

    public void shlq(X86_64CpuRegister operand, X86_64CpuRegister shifter) {
        EmitGenericShift(true, 4, operand, shifter);
    }

    public void shrl(X86_64CpuRegister reg, X86_64Immediate imm) {
        EmitGenericShift(false, 5, reg, imm);
    }

    public void shrq(X86_64CpuRegister reg, X86_64Immediate imm) {
        EmitGenericShift(true, 5, reg, imm);
    }

    public void shrl(X86_64CpuRegister operand, X86_64CpuRegister shifter) {
        EmitGenericShift(false, 5, operand, shifter);
    }

    public void shrq(X86_64CpuRegister operand, X86_64CpuRegister shifter) {
        EmitGenericShift(true, 5, operand, shifter);
    }

    public void sarl(X86_64CpuRegister reg, X86_64Immediate imm) {
        EmitGenericShift(false, 7, reg, imm);
    }

    public void sarl(X86_64CpuRegister operand, X86_64CpuRegister shifter) {
        EmitGenericShift(false, 7, operand, shifter);
    }

    public void sarq(X86_64CpuRegister reg, X86_64Immediate imm) {
        EmitGenericShift(true, 7, reg, imm);
    }

    public void sarq(X86_64CpuRegister operand, X86_64CpuRegister shifter) {
        EmitGenericShift(true, 7, operand, shifter);
    }

    public void roll(X86_64CpuRegister reg, X86_64Immediate imm) {
        EmitGenericShift(false, 0, reg, imm);
    }

    public void roll(X86_64CpuRegister operand, X86_64CpuRegister shifter) {
        EmitGenericShift(false, 0, operand, shifter);
    }

    public void rorl(X86_64CpuRegister reg, X86_64Immediate imm) {
        EmitGenericShift(false, 1, reg, imm);
    }

    public void rorl(X86_64CpuRegister operand, X86_64CpuRegister shifter) {
        EmitGenericShift(false, 1, operand, shifter);
    }

    public void rolq(X86_64CpuRegister reg, X86_64Immediate imm) {
        EmitGenericShift(true, 0, reg, imm);
    }

    public void rolq(X86_64CpuRegister operand, X86_64CpuRegister shifter) {
        EmitGenericShift(true, 0, operand, shifter);
    }

    public void rorq(X86_64CpuRegister reg, X86_64Immediate imm) {
        EmitGenericShift(true, 1, reg, imm);
    }

    public void rorq(X86_64CpuRegister operand, X86_64CpuRegister shifter) {
        EmitGenericShift(true, 1, operand, shifter);
    }

    public void negl(X86_64CpuRegister reg) {
        EmitOptionalRex32(reg);
        emit8(0xF7);
        EmitOperand(3, new X86_64Operand(reg));
    }

    public void negq(X86_64CpuRegister reg) {
        EmitRex64(reg);
        emit8(0xF7);
        EmitOperand(3, new X86_64Operand(reg));
    }

    public void notl(X86_64CpuRegister reg) {
        EmitOptionalRex32(reg);
        emit8(0xF7);
        emit8(0xD0 | reg.lowBits());
    }

    public void notq(X86_64CpuRegister reg) {
        EmitRex64(reg);
        emit8(0xF7);
        EmitOperand(2, new X86_64Operand(reg));
    }

    public void enter(X86_64Immediate imm) {
        emit8(0xC8);
        CHECK(imm.isUInt16());
        emit8((int) imm.value() & 0xFF);
        emit8(((int) imm.value() >> 8) & 0xFF);
        emit8(0x00);
    }

    public void leave() {
        emit8(0xC9);
    }

    public void ret() {
        emit8(0xC3);
    }

    public void ret(X86_64Immediate imm) {
        emit8(0xC2);
        CHECK(imm.isUInt16());
        emit8((int) imm.value() & 0xFF);
        emit8(((int) imm.value() >> 8) & 0xFF);
    }

    public void nop() {
        emit8(0x90);
    }

    public void int3() {
        emit8(0xCC);
    }

    public void hlt() {
        emit8(0xF4);
    }

    public void j(X86Condition condition, Label label) {
        if (label.isBound()) {
            final int kShortSize = 2;
            final int kLongSize = 6;
            int offset = label.getPosition() - size();
            CHECK_LE(offset, 0);
            if (Utils.isInt8(offset - kShortSize)) {
                emit8(0x70 + condition.index());
                emit8((offset - kShortSize) & 0xFF);
            } else {
                emit8(0x0F);
                emit8(0x80 + condition.index());
                emit32(offset - kLongSize);
            }
        } else {
            emit8(0x0F);
            emit8(0x80 + condition.index());
            EmitLabelLink(label);
        }
    }

    public void j(X86Condition condition, X86NearLabel label) {
        if (label.isBound()) {
            final int kShortSize = 2;
            int offset = label.getPosition() - size();
            CHECK_LE(offset, 0);
            CHECK(Utils.isInt8(offset - kShortSize));
            emit8(0x70 + condition.index());
            emit8((offset - kShortSize) & 0xFF);
        } else {
            emit8(0x70 + condition.index());
            EmitLabelLink(label);
        }
    }

    public void jrcxz(X86NearLabel label) {
        if (label.isBound()) {
            final int kShortSize = 2;
            int offset = label.getPosition() - size();
            CHECK_LE(offset, 0);
            CHECK(Utils.isInt8(offset - kShortSize));
            emit8(0xE3);
            emit8((offset - kShortSize) & 0xFF);
        } else {
            emit8(0xE3);
            EmitLabelLink(label);
        }
    }

    public void jmp(X86_64CpuRegister reg) {
        EmitOptionalRex32(reg);
        emit8(0xFF);
        EmitRegisterOperand(4, reg.lowBits());
    }

    public void jmp(X86_64Address address) {
        EmitOptionalRex32(address);
        emit8(0xFF);
        EmitOperand(4, address);
    }

    public void jmp(Label label) {
        if (label.isBound()) {
            final int kShortSize = 2;
            final int kLongSize = 5;
            int offset = label.getPosition() - size();
            CHECK_LE(offset, 0);
            if (Utils.isInt8(offset - kShortSize)) {
                emit8(0xEB);
                emit8((offset - kShortSize) & 0xFF);
            } else {
                emit8(0xE9);
                emit32(offset - kLongSize);
            }
        } else {
            emit8(0xE9);
            EmitLabelLink(label);
        }
    }

    public void jmp(X86NearLabel label) {
        if (label.isBound()) {
            final int kShortSize = 2;
            int offset = label.getPosition() - size();
            CHECK_LE(offset, 0);
            CHECK(Utils.isInt8(offset - kShortSize));
            emit8(0xEB);
            emit8((offset - kShortSize) & 0xFF);
        } else {
            emit8(0xEB);
            EmitLabelLink(label);
        }
    }

    public void rep_movsw() {
        emit8(0x66);
        emit8(0xF3);
        emit8(0xA5);
    }

    public void rep_movsb() {
        emit8(0xF3);
        emit8(0xA4);
    }

    public void rep_movsl() {
        emit8(0xF3);
        emit8(0xA5);
    }

    public X86_64Assembler lock() {
        emit8(0xF0);
        return this;
    }

    public void cmpxchgb(X86_64Address address, X86_64CpuRegister reg) {
        EmitOptionalByteRegNormalizingRex32(reg, address);
        emit8(0x0F);
        emit8(0xB0);
        EmitOperand(reg.lowBits(), address);
    }

    public void cmpxchgw(X86_64Address address, X86_64CpuRegister reg) {
        EmitOperandSizeOverride();
        EmitOptionalRex32(reg, address);
        emit8(0x0F);
        emit8(0xB1);
        EmitOperand(reg.lowBits(), address);
    }

    public void cmpxchgl(X86_64Address address, X86_64CpuRegister reg) {
        EmitOptionalRex32(reg, address);
        emit8(0x0F);
        emit8(0xB1);
        EmitOperand(reg.lowBits(), address);
    }

    public void cmpxchgq(X86_64Address address, X86_64CpuRegister reg) {
        EmitRex64(reg, address);
        emit8(0x0F);
        emit8(0xB1);
        EmitOperand(reg.lowBits(), address);
    }

    public void mfence() {
        emit8(0x0F);
        emit8(0xAE);
        emit8(0xF0);
    }

    public X86_64Assembler gs() {
        // TODO: gs is a prefix and not an instruction
        emit8(0x65);
        return this;
    }

    public void setcc(X86Condition condition, X86_64CpuRegister dst) {
        // RSP, RBP, RDI, RSI need rex prefix (else the pattern encodes ah/bh/ch/dh).
        if (dst.needsRex() || dst.index() > 3) {
            EmitOptionalRex(true, false, false, false, dst.needsRex());
        }
        emit8(0x0F);
        emit8(0x90 + condition.index());
        emit8(0xC0 + dst.lowBits());
    }

    public void blsi(X86_64CpuRegister dst, X86_64CpuRegister src) {
        byte byte_zero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ false);
        byte byte_one = EmitVexPrefixByteOne(/*R=*/ false,
                /*X=*/ false,
                src.needsRex(),
                SET_VEX_M_0F_38);
        byte byte_two = EmitVexPrefixByteTwo(/*W=*/true,
                X86_64ManagedRegister.fromCpuRegister(dst),
                SET_VEX_L_128,
                SET_VEX_PP_NONE);
        emit8(byte_zero);
        emit8(byte_one);
        emit8(byte_two);
        emit8(0xF3);
        EmitRegisterOperand(3, src.lowBits());
    }

    public void blsmsk(X86_64CpuRegister dst, X86_64CpuRegister src) {
        byte byte_zero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ false);
        byte byte_one = EmitVexPrefixByteOne(/*R=*/ false,
                /*X=*/ false,
                src.needsRex(),
                SET_VEX_M_0F_38);
        byte byte_two = EmitVexPrefixByteTwo(/*W=*/ true,
                X86_64ManagedRegister.fromCpuRegister(dst),
                SET_VEX_L_128,
                SET_VEX_PP_NONE);
        emit8(byte_zero);
        emit8(byte_one);
        emit8(byte_two);
        emit8(0xF3);
        EmitRegisterOperand(2, src.lowBits());
    }

    public void blsr(X86_64CpuRegister dst, X86_64CpuRegister src) {
        byte byte_zero = EmitVexPrefixByteZero(/*is_twobyte_form=*/false);
        byte byte_one = EmitVexPrefixByteOne(/*R=*/ false,
                /*X=*/ false,
                src.needsRex(),
                SET_VEX_M_0F_38);
        byte byte_two = EmitVexPrefixByteTwo(/*W=*/ true,
                X86_64ManagedRegister.fromCpuRegister(dst),
                SET_VEX_L_128,
                SET_VEX_PP_NONE);
        emit8(byte_zero);
        emit8(byte_one);
        emit8(byte_two);
        emit8(0xF3);
        EmitRegisterOperand(1, src.lowBits());
    }

    public void bswapl(X86_64CpuRegister dst) {
        EmitOptionalRex(false, false, false, false, dst.needsRex());
        emit8(0x0F);
        emit8(0xC8 + dst.lowBits());
    }

    public void bswapq(X86_64CpuRegister dst) {
        EmitOptionalRex(false, true, false, false, dst.needsRex());
        emit8(0x0F);
        emit8(0xC8 + dst.lowBits());
    }

    public void bsfl(X86_64CpuRegister dst, X86_64CpuRegister src) {
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xBC);
        EmitRegisterOperand(dst.lowBits(), src.lowBits());
    }

    public void bsfl(X86_64CpuRegister dst, X86_64Address src) {
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xBC);
        EmitOperand(dst.lowBits(), src);
    }

    public void bsfq(X86_64CpuRegister dst, X86_64CpuRegister src) {
        EmitRex64(dst, src);
        emit8(0x0F);
        emit8(0xBC);
        EmitRegisterOperand(dst.lowBits(), src.lowBits());
    }

    public void bsfq(X86_64CpuRegister dst, X86_64Address src) {
        EmitRex64(dst, src);
        emit8(0x0F);
        emit8(0xBC);
        EmitOperand(dst.lowBits(), src);
    }

    public void bsrl(X86_64CpuRegister dst, X86_64CpuRegister src) {
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xBD);
        EmitRegisterOperand(dst.lowBits(), src.lowBits());
    }

    public void bsrl(X86_64CpuRegister dst, X86_64Address src) {
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xBD);
        EmitOperand(dst.lowBits(), src);
    }

    public void bsrq(X86_64CpuRegister dst, X86_64CpuRegister src) {
        EmitRex64(dst, src);
        emit8(0x0F);
        emit8(0xBD);
        EmitRegisterOperand(dst.lowBits(), src.lowBits());
    }

    public void bsrq(X86_64CpuRegister dst, X86_64Address src) {
        EmitRex64(dst, src);
        emit8(0x0F);
        emit8(0xBD);
        EmitOperand(dst.lowBits(), src);
    }

    public void popcntl(X86_64CpuRegister dst, X86_64CpuRegister src) {
        emit8(0xF3);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xB8);
        EmitRegisterOperand(dst.lowBits(), src.lowBits());
    }

    public void popcntl(X86_64CpuRegister dst, X86_64Address src) {
        emit8(0xF3);
        EmitOptionalRex32(dst, src);
        emit8(0x0F);
        emit8(0xB8);
        EmitOperand(dst.lowBits(), src);
    }

    public void popcntq(X86_64CpuRegister dst, X86_64CpuRegister src) {
        emit8(0xF3);
        EmitRex64(dst, src);
        emit8(0x0F);
        emit8(0xB8);
        EmitRegisterOperand(dst.lowBits(), src.lowBits());
    }

    public void popcntq(X86_64CpuRegister dst, X86_64Address src) {
        emit8(0xF3);
        EmitRex64(dst, src);
        emit8(0x0F);
        emit8(0xB8);
        EmitOperand(dst.lowBits(), src);
    }

    public void rdtsc() {
        emit8(0x0F);
        emit8(0x31);
    }

    public void repne_scasb() {
        emit8(0xF2);
        emit8(0xAE);
    }

    public void repne_scasw() {
        emit8(0x66);
        emit8(0xF2);
        emit8(0xAF);
    }

    public void repe_cmpsw() {
        emit8(0x66);
        emit8(0xF3);
        emit8(0xA7);
    }

    public void repe_cmpsl() {
        emit8(0xF3);
        emit8(0xA7);
    }

    public void repe_cmpsq() {
        emit8(0xF3);
        EmitRex64();
        emit8(0xA7);
    }

    public void ud2() {
        emit8(0x0F);
        emit8(0x0B);
    }

    @Override
    public void bind(Label label) {
        int bound = size();
        CHECK(!label.isBound());  // Labels can only be bound once.
        while (label.isLinked()) {
            int position = label.getLinkPosition();
            int next = load32(position);
            store32(position, bound - (position + 4));
            label.position = next;
        }
        label.bindTo(bound);
    }

    public void bind(X86NearLabel label) {
        int bound = size();
        CHECK(!label.isBound());  // Labels can only be bound once.
        while (label.isLinked()) {
            int position = label.getLinkPosition();
            int delta = loadU8(position);
            int offset = bound - (position + 1);
            CHECK(Utils.isInt8(offset));
            store8(position, offset);
            label.position = delta != 0 ? label.position - delta : 0;
        }
        label.bindTo(bound);
    }

    @Override
    public void jump(Label label) {
        jmp(label);
    }
}
