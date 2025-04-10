package com.v7878.jnasm.x86;

import static com.v7878.jnasm.Utils.CHECK;
import static com.v7878.jnasm.Utils.CHECK_EQ;
import static com.v7878.jnasm.Utils.CHECK_GE;
import static com.v7878.jnasm.Utils.CHECK_GT;
import static com.v7878.jnasm.Utils.CHECK_LE;
import static com.v7878.jnasm.Utils.CHECK_LT;
import static com.v7878.jnasm.common_x86.X86VEXConstants.SET_VEX_B;
import static com.v7878.jnasm.common_x86.X86VEXConstants.SET_VEX_L_128;
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
import static com.v7878.jnasm.x86.X86CpuRegister.EAX;
import static com.v7878.jnasm.x86.X86CpuRegister.ECX;
import static com.v7878.jnasm.x86.X86CpuRegister.kFirstByteUnsafeRegister;

import com.v7878.jnasm.Assembler;
import com.v7878.jnasm.AssemblerFixup;
import com.v7878.jnasm.Label;
import com.v7878.jnasm.Utils;
import com.v7878.jnasm.common_x86.X86Condition;
import com.v7878.jnasm.common_x86.X86NearLabel;

public class X86Assembler extends Assembler implements X86AssemblerI {
    private final boolean has_AVX_or_AVX2;

    public X86Assembler(boolean has_AVX_or_AVX2) {
        this.has_AVX_or_AVX2 = has_AVX_or_AVX2;
    }

    public boolean cpuHasAVXorAVX2FeatureFlag() {
        return has_AVX_or_AVX2;
    }

    private void EmitRegisterOperand(int rm, int reg) {
        CHECK_GE(rm, 0);
        CHECK_LT(rm, 8);
        emit8(0xC0 + (rm << 3) + reg);
    }

    private void EmitXmmRegisterOperand(int rm, X86XmmRegister reg) {
        EmitRegisterOperand(rm, reg.index());
    }

    private void EmitOperandSizeOverride() {
        emit8(0x66);
    }

    private void EmitOperand(int reg_or_opcode, X86Operand operand) {
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

    private void EmitImmediate(X86Immediate imm, boolean is_16_op) {
        if (is_16_op) {
            emit8(imm.value() & 0xFF);
            emit8(imm.value() >> 8);
        } else {
            emit32(imm.value());
        }
    }

    private void EmitImmediate(X86Immediate imm) {
        EmitImmediate(imm, false);
    }

    private void EmitComplex(int reg_or_opcode, X86Operand operand, X86Immediate immediate, boolean is_16_op) {
        CHECK_GE(reg_or_opcode, 0);
        CHECK_LT(reg_or_opcode, 8);
        if (immediate.isInt8()) {
            // Use sign-extended 8-bit immediate.
            emit8(0x83);
            EmitOperand(reg_or_opcode, operand);
            emit8(immediate.value() & 0xFF);
        } else if (operand.isRegister(EAX)) {
            // Use short form if the destination is eax.
            emit8(0x05 + (reg_or_opcode << 3));
            EmitImmediate(immediate, is_16_op);
        } else {
            emit8(0x81);
            EmitOperand(reg_or_opcode, operand);
            EmitImmediate(immediate, is_16_op);
        }
    }

    private void EmitComplex(int rm, X86Operand operand, X86Immediate immediate) {
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

    private void EmitGenericShift(int reg_or_opcode, X86Operand operand, X86Immediate imm) {
        CHECK(imm.isInt8());
        if (imm.value() == 1) {
            emit8(0xD1);
            EmitOperand(reg_or_opcode, operand);
        } else {
            emit8(0xC1);
            EmitOperand(reg_or_opcode, operand);
            emit8(imm.value() & 0xFF);
        }
    }

    private void EmitGenericShift(int reg_or_opcode, X86Operand operand, X86CpuRegister shifter) {
        CHECK_EQ(shifter.index(), ECX.index());
        emit8(0xD3);
        EmitOperand(reg_or_opcode, operand);
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
        // Bits[4:0],
        vex_prefix |= SET_VEX_M;
        return (byte) vex_prefix;
    }

    @SuppressWarnings("SameParameterValue")
    private byte EmitVexPrefixByteOne(boolean R,
                                      X86ManagedRegister operand,
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
            X86XmmRegister vvvv = operand.asXmmRegister();
            int inverted_reg = 15 - vvvv.index();
            vex_prefix |= ((inverted_reg & 0x0F) << 3);
        } else if (operand.isCpuRegister()) {
            X86CpuRegister vvvv = operand.asCpuRegister();
            int inverted_reg = 15 - vvvv.index();
            vex_prefix |= ((inverted_reg & 0x0F) << 3);
        }
        // Bit[2] - "L" If VEX.L = 1 indicates 256-bit vector operation ,
        // VEX.L = 0 indicates 128 bit vector operation
        vex_prefix |= SET_VEX_L;
        // Bits[1:0] -  "pp"
        vex_prefix |= SET_VEX_PP;
        return (byte) vex_prefix;
    }

    @SuppressWarnings("SameParameterValue")
    private byte EmitVexPrefixByteTwo(boolean W,
                                      X86ManagedRegister operand,
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
            X86XmmRegister vvvv = operand.asXmmRegister();
            int inverted_reg = 15 - vvvv.index();
            vex_prefix |= ((inverted_reg & 0x0F) << 3);
        } else if (operand.isCpuRegister()) {
            X86CpuRegister vvvv = operand.asCpuRegister();
            int inverted_reg = 15 - vvvv.index();
            vex_prefix |= ((inverted_reg & 0x0F) << 3);
        }
        // Bit[2] - "L" If VEX.L = 1 indicates 256-bit vector operation ,
        // VEX.L = 0 indicates 128 bit vector operation
        vex_prefix |= SET_VEX_L;
        // Bits[1:0] -  "pp"
        vex_prefix |= SET_VEX_PP;
        return (byte) vex_prefix;
    }

    public void call(X86CpuRegister reg) {
        emit8(0xFF);
        EmitRegisterOperand(2, reg.index());
    }

    public void call(X86Address address) {
        emit8(0xFF);
        EmitOperand(2, address);
    }

    public void call(Label label) {
        emit8(0xE8);
        final int kSize = 5;
        // Offset by one because we already have emitted the opcode.
        EmitLabel(label, kSize - 1);
    }

    public void call(X86ExternalLabel label) {
        emit8(0xE8);
        emit32(label.address());
    }

    public void pushl(X86CpuRegister reg) {
        emit8(0x50 + reg.index());
    }

    public void pushl(X86Address address) {
        emit8(0xFF);
        EmitOperand(6, address);
    }

    public void pushl(X86Immediate imm) {
        if (imm.isInt8()) {
            emit8(0x6A);
            emit8(imm.value() & 0xFF);
        } else {
            emit8(0x68);
            EmitImmediate(imm);
        }
    }

    public void popl(X86CpuRegister reg) {
        emit8(0x58 + reg.index());
    }

    public void popl(X86Address address) {
        emit8(0x8F);
        EmitOperand(0, address);
    }

    public void movl(X86CpuRegister dst, X86Immediate imm) {
        emit8(0xB8 + dst.index());
        EmitImmediate(imm);
    }

    public void movl(X86CpuRegister dst, X86CpuRegister src) {
        emit8(0x89);
        EmitRegisterOperand(src.index(), dst.index());
    }

    public void movl(X86CpuRegister dst, X86Address src) {
        emit8(0x8B);
        EmitOperand(dst.index(), src);
    }

    public void movl(X86Address dst, X86CpuRegister src) {
        emit8(0x89);
        EmitOperand(src.index(), dst);
    }

    public void movl(X86Address dst, X86Immediate imm) {
        emit8(0xC7);
        EmitOperand(0, dst);
        EmitImmediate(imm);
    }

    public void movl(X86Address dst, Label lbl) {
        emit8(0xC7);
        EmitOperand(0, dst);
        EmitLabel(lbl, dst.length + 5);
    }

    public void movntl(X86Address dst, X86CpuRegister src) {
        emit8(0x0F);
        emit8(0xC3);
        EmitOperand(src.index(), dst);
    }

    public void blsi(X86CpuRegister dst, X86CpuRegister src) {
        byte byte_zero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ false);
        byte byte_one = EmitVexPrefixByteOne(false, false, false, SET_VEX_M_0F_38);
        byte byte_two = EmitVexPrefixByteTwo(false,
                X86ManagedRegister.fromCpuRegister(dst),
                SET_VEX_L_128, SET_VEX_PP_NONE);
        emit8(byte_zero);
        emit8(byte_one);
        emit8(byte_two);
        emit8(0xF3);
        EmitRegisterOperand(3, src.index());
    }

    public void blsmsk(X86CpuRegister dst, X86CpuRegister src) {
        byte byte_zero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ false);
        byte byte_one = EmitVexPrefixByteOne(false, false, false, SET_VEX_M_0F_38);
        byte byte_two = EmitVexPrefixByteTwo(false,
                X86ManagedRegister.fromCpuRegister(dst),
                SET_VEX_L_128, SET_VEX_PP_NONE);
        emit8(byte_zero);
        emit8(byte_one);
        emit8(byte_two);
        emit8(0xF3);
        EmitRegisterOperand(2, src.index());
    }

    public void blsr(X86CpuRegister dst, X86CpuRegister src) {
        byte byte_zero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ false);
        byte byte_one = EmitVexPrefixByteOne(false, false, false, SET_VEX_M_0F_38);
        byte byte_two = EmitVexPrefixByteTwo(false,
                X86ManagedRegister.fromCpuRegister(dst),
                SET_VEX_L_128, SET_VEX_PP_NONE);
        emit8(byte_zero);
        emit8(byte_one);
        emit8(byte_two);
        emit8(0xF3);
        EmitRegisterOperand(1, src.index());
    }

    public void bswapl(X86CpuRegister dst) {
        emit8(0x0F);
        emit8(0xC8 + dst.index());
    }

    public void bsfl(X86CpuRegister dst, X86CpuRegister src) {
        emit8(0x0F);
        emit8(0xBC);
        EmitRegisterOperand(dst.index(), src.index());
    }

    public void bsfl(X86CpuRegister dst, X86Address src) {
        emit8(0x0F);
        emit8(0xBC);
        EmitOperand(dst.index(), src);
    }

    public void bsrl(X86CpuRegister dst, X86CpuRegister src) {
        emit8(0x0F);
        emit8(0xBD);
        EmitRegisterOperand(dst.index(), src.index());
    }

    public void bsrl(X86CpuRegister dst, X86Address src) {
        emit8(0x0F);
        emit8(0xBD);
        EmitOperand(dst.index(), src);
    }

    public void popcntl(X86CpuRegister dst, X86CpuRegister src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0xB8);
        EmitRegisterOperand(dst.index(), src.index());
    }

    public void popcntl(X86CpuRegister dst, X86Address src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0xB8);
        EmitOperand(dst.index(), src);
    }

    public void movzxb(X86CpuRegister dst, X86ByteRegister src) {
        emit8(0x0F);
        emit8(0xB6);
        EmitRegisterOperand(dst.index(), src.index());
    }

    public void movzxb(X86CpuRegister dst, X86Address src) {
        emit8(0x0F);
        emit8(0xB6);
        EmitOperand(dst.index(), src);
    }

    public void movsxb(X86CpuRegister dst, X86ByteRegister src) {
        emit8(0x0F);
        emit8(0xBE);
        EmitRegisterOperand(dst.index(), src.index());
    }

    public void movsxb(X86CpuRegister dst, X86Address src) {
        emit8(0x0F);
        emit8(0xBE);
        EmitOperand(dst.index(), src);
    }

    public void movb(X86CpuRegister dst, X86Address src) {
        throw new IllegalStateException("Use movzxb or movsxb instead");
    }

    public void movb(X86Address dst, X86ByteRegister src) {
        emit8(0x88);
        EmitOperand(src.index(), dst);
    }

    public void movb(X86Address dst, X86Immediate imm) {
        emit8(0xC6);
        EmitOperand(EAX.index(), dst);
        CHECK(imm.isInt8());
        emit8(imm.value() & 0xFF);
    }

    public void movzxw(X86CpuRegister dst, X86CpuRegister src) {
        emit8(0x0F);
        emit8(0xB7);
        EmitRegisterOperand(dst.index(), src.index());
    }

    public void movzxw(X86CpuRegister dst, X86Address src) {
        emit8(0x0F);
        emit8(0xB7);
        EmitOperand(dst.index(), src);
    }

    public void movsxw(X86CpuRegister dst, X86CpuRegister src) {
        emit8(0x0F);
        emit8(0xBF);
        EmitRegisterOperand(dst.index(), src.index());
    }

    public void movsxw(X86CpuRegister dst, X86Address src) {
        emit8(0x0F);
        emit8(0xBF);
        EmitOperand(dst.index(), src);
    }

    public void movw(X86CpuRegister dst, X86Address src) {
        throw new IllegalStateException("Use movzxw or movsxw instead");
    }

    public void movw(X86Address dst, X86CpuRegister src) {
        EmitOperandSizeOverride();
        emit8(0x89);
        EmitOperand(src.index(), dst);
    }

    public void movw(X86Address dst, X86Immediate imm) {
        EmitOperandSizeOverride();
        emit8(0xC7);
        EmitOperand(0, dst);
        CHECK(imm.isUInt16() || imm.isInt16());
        emit8(imm.value() & 0xFF);
        emit8(imm.value() >> 8);
    }

    public void leal(X86CpuRegister dst, X86Address src) {
        emit8(0x8D);
        EmitOperand(dst.index(), src);
    }

    public void cmovl(X86Condition condition, X86CpuRegister dst, X86CpuRegister src) {
        emit8(0x0F);
        emit8(0x40 + condition.index());
        EmitRegisterOperand(dst.index(), src.index());
    }

    public void cmovl(X86Condition condition, X86CpuRegister dst, X86Address src) {
        emit8(0x0F);
        emit8(0x40 + condition.index());
        EmitOperand(dst.index(), src);
    }

    public void setb(X86Condition condition, X86CpuRegister dst) {
        emit8(0x0F);
        emit8(0x90 + condition.index());
        EmitOperand(0, new X86Operand(dst));
    }

    public void movaps(X86XmmRegister dst, X86XmmRegister src) {
        if (cpuHasAVXorAVX2FeatureFlag()) {
            vmovaps(dst, src);
            return;
        }
        emit8(0x0F);
        emit8(0x28);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    /*VEX.128.0F.WIG 28 /r VMOVAPS xmm1, xmm2*/
    public void vmovaps(X86XmmRegister dst, X86XmmRegister src) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        /*Instruction VEX Prefix*/
        byte byte_zero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
        X86ManagedRegister vvvv_reg = X86ManagedRegister.NoRegister();
      /*a REX prefix is necessary only if an instruction references one of the
      extended registers or uses a 64-bit operand.*/
        byte byte_one = EmitVexPrefixByteOne(/*R=*/ false,
                vvvv_reg,
                SET_VEX_L_128,
                SET_VEX_PP_NONE);
        emit8(byte_zero);
        emit8(byte_one);
        /*Instruction Opcode*/
        emit8(0x28);
        /*Instruction Operands*/
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void movaps(X86XmmRegister dst, X86Address src) {
        if (cpuHasAVXorAVX2FeatureFlag()) {
            vmovaps(dst, src);
            return;
        }
        emit8(0x0F);
        emit8(0x28);
        EmitOperand(dst.index(), src);
    }

    /*VEX.128.0F.WIG 28 /r VMOVAPS xmm1, m128*/
    public void vmovaps(X86XmmRegister dst, X86Address src) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        /*Instruction VEX Prefix*/
        byte ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
        X86ManagedRegister vvvv_reg = X86ManagedRegister.NoRegister();
      /*a REX prefix is necessary only if an instruction references one of the
      extended registers or uses a 64-bit operand.*/
        byte ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                vvvv_reg,
                SET_VEX_L_128,
                SET_VEX_PP_NONE);
        emit8(ByteZero);
        emit8(ByteOne);
        /*Instruction Opcode*/
        emit8(0x28);
        /*Instruction Operands*/
        EmitOperand(dst.index(), src);
    }

    public void movups(X86XmmRegister dst, X86Address src) {
        if (cpuHasAVXorAVX2FeatureFlag()) {
            vmovups(dst, src);
            return;
        }
        emit8(0x0F);
        emit8(0x10);
        EmitOperand(dst.index(), src);
    }

    /*VEX.128.0F.WIG 10 /r VMOVUPS xmm1, m128*/
    public void vmovups(X86XmmRegister dst, X86Address src) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        /*Instruction VEX Prefix*/
        byte ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
        X86ManagedRegister vvvv_reg = X86ManagedRegister.NoRegister();
      /*a REX prefix is necessary only if an instruction references one of the
      extended registers or uses a 64-bit operand.*/
        byte ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                vvvv_reg,
                SET_VEX_L_128,
                SET_VEX_PP_NONE);
        emit8(ByteZero);
        emit8(ByteOne);
        /*Instruction Opcode*/
        emit8(0x10);
        /*Instruction Operands*/
        EmitOperand(dst.index(), src);
    }

    public void movaps(X86Address dst, X86XmmRegister src) {
        if (cpuHasAVXorAVX2FeatureFlag()) {
            vmovaps(dst, src);
            return;
        }
        emit8(0x0F);
        emit8(0x29);
        EmitOperand(src.index(), dst);
    }

    /*VEX.128.0F.WIG 29 /r VMOVAPS m128, xmm1*/
    public void vmovaps(X86Address dst, X86XmmRegister src) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        /*Instruction VEX Prefix*/
        byte ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
        X86ManagedRegister vvvv_reg = X86ManagedRegister.NoRegister();
      /*a REX prefix is necessary only if an instruction references one of the
      extended registers or uses a 64-bit operand.*/
        byte ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                vvvv_reg,
                SET_VEX_L_128,
                SET_VEX_PP_NONE);
        emit8(ByteZero);
        emit8(ByteOne);
        /*Instruction Opcode*/
        emit8(0x29);
        /*Instruction Operands*/
        EmitOperand(src.index(), dst);
    }

    public void movups(X86Address dst, X86XmmRegister src) {
        if (cpuHasAVXorAVX2FeatureFlag()) {
            vmovups(dst, src);
            return;
        }
        emit8(0x0F);
        emit8(0x11);
        EmitOperand(src.index(), dst);
    }

    /*VEX.128.0F.WIG 11 /r VMOVUPS m128, xmm1*/
    public void vmovups(X86Address dst, X86XmmRegister src) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        /*Instruction VEX Prefix*/
        byte ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
        X86ManagedRegister vvvv_reg = X86ManagedRegister.NoRegister();
      /*a REX prefix is necessary only if an instruction references one of the
      extended registers or uses a 64-bit operand.*/
        byte ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                vvvv_reg,
                SET_VEX_L_128,
                SET_VEX_PP_NONE);
        emit8(ByteZero);
        emit8(ByteOne);
        // Instruction Opcode
        emit8(0x11);
        // Instruction Operands
        EmitOperand(src.index(), dst);
    }

    public void movss(X86XmmRegister dst, X86Address src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0x10);
        EmitOperand(dst.index(), src);
    }

    public void movss(X86Address dst, X86XmmRegister src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0x11);
        EmitOperand(src.index(), dst);
    }

    public void movss(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0x11);
        EmitXmmRegisterOperand(src.index(), dst);
    }

    public void movd(X86XmmRegister dst, X86CpuRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x6E);
        X86Operand operand = new X86Operand(src);
        EmitOperand(dst.index(), operand);
    }

    public void movd(X86CpuRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x7E);
        X86Operand operand = new X86Operand(dst);
        EmitOperand(src.index(), operand);
    }

    public void addss(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0x58);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void addss(X86XmmRegister dst, X86Address src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0x58);
        EmitOperand(dst.index(), src);
    }

    public void subss(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0x5C);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void subss(X86XmmRegister dst, X86Address src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0x5C);
        EmitOperand(dst.index(), src);
    }

    public void mulss(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0x59);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void mulss(X86XmmRegister dst, X86Address src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0x59);
        EmitOperand(dst.index(), src);
    }

    public void divss(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0x5E);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void divss(X86XmmRegister dst, X86Address src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0x5E);
        EmitOperand(dst.index(), src);
    }

    public void addps(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x0F);
        emit8(0x58);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void vaddps(X86XmmRegister dst, X86XmmRegister add_left, X86XmmRegister add_right) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte ByteZero, ByteOne;
        ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
        ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                X86ManagedRegister.fromXmmRegister(add_left),
                SET_VEX_L_128,
                SET_VEX_PP_NONE);
        emit8(ByteZero);
        emit8(ByteOne);
        emit8(0x58);
        EmitXmmRegisterOperand(dst.index(), add_right);
    }

    public void subps(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x0F);
        emit8(0x5C);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void vsubps(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte byte_zero, byte_one;
        byte_zero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
        X86ManagedRegister vvvv_reg = X86ManagedRegister.fromXmmRegister(src1);
        byte_one = EmitVexPrefixByteOne(/*R=*/ false, vvvv_reg, SET_VEX_L_128, SET_VEX_PP_NONE);
        emit8(byte_zero);
        emit8(byte_one);
        emit8(0x5C);
        EmitXmmRegisterOperand(dst.index(), src2);
    }

    public void mulps(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x0F);
        emit8(0x59);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void vmulps(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte ByteZero, ByteOne;
        ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
        ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                X86ManagedRegister.fromXmmRegister(src1),
                SET_VEX_L_128,
                SET_VEX_PP_NONE);
        emit8(ByteZero);
        emit8(ByteOne);
        emit8(0x59);
        EmitXmmRegisterOperand(dst.index(), src2);
    }

    public void divps(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x0F);
        emit8(0x5E);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void vdivps(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte ByteZero, ByteOne;
        ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
        ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                X86ManagedRegister.fromXmmRegister(src1),
                SET_VEX_L_128,
                SET_VEX_PP_NONE);
        emit8(ByteZero);
        emit8(ByteOne);
        emit8(0x5E);
        EmitXmmRegisterOperand(dst.index(), src2);
    }

    public void vfmadd213ss(X86XmmRegister acc, X86XmmRegister left, X86XmmRegister right) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte ByteZero, ByteOne, ByteTwo;
        ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ false);
        X86ManagedRegister vvvv_reg = X86ManagedRegister.fromXmmRegister(left);
        ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                /*X=*/ false,
                /*B=*/ false,
                SET_VEX_M_0F_38);
        ByteTwo = EmitVexPrefixByteTwo(/*W=*/ false, vvvv_reg, SET_VEX_L_128, SET_VEX_PP_66);
        emit8(ByteZero);
        emit8(ByteOne);
        emit8(ByteTwo);
        emit8(0xA9);
        EmitXmmRegisterOperand(acc.index(), right);
    }

    public void vfmadd213sd(X86XmmRegister acc, X86XmmRegister left, X86XmmRegister right) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte ByteZero, ByteOne, ByteTwo;
        ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ false);
        X86ManagedRegister vvvv_reg = X86ManagedRegister.fromXmmRegister(left);
        ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                /*X=*/ false,
                /*B=*/ false,
                SET_VEX_M_0F_38);
        ByteTwo = EmitVexPrefixByteTwo(/*W=*/ true, vvvv_reg, SET_VEX_L_128, SET_VEX_PP_66);
        emit8(ByteZero);
        emit8(ByteOne);
        emit8(ByteTwo);
        emit8(0xA9);
        EmitXmmRegisterOperand(acc.index(), right);
    }

    public void movapd(X86XmmRegister dst, X86XmmRegister src) {
        if (cpuHasAVXorAVX2FeatureFlag()) {
            vmovapd(dst, src);
            return;
        }
        emit8(0x66);
        emit8(0x0F);
        emit8(0x28);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    /*VEX.128.66.0F.WIG 28 /r VMOVAPD xmm1, xmm2*/
    public void vmovapd(X86XmmRegister dst, X86XmmRegister src) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        /*Instruction VEX Prefix*/
        byte ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
        X86ManagedRegister vvvv_reg = X86ManagedRegister.NoRegister();
      /*a REX prefix is necessary only if an instruction references one of the
      extended registers or uses a 64-bit operand.*/
        byte ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                vvvv_reg,
                SET_VEX_L_128,
                SET_VEX_PP_66);
        emit8(ByteZero);
        emit8(ByteOne);
        // Instruction Opcode
        emit8(0x28);
        // Instruction Operands
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void movapd(X86XmmRegister dst, X86Address src) {
        if (cpuHasAVXorAVX2FeatureFlag()) {
            vmovapd(dst, src);
            return;
        }
        emit8(0x66);
        emit8(0x0F);
        emit8(0x28);
        EmitOperand(dst.index(), src);
    }

    /*VEX.128.66.0F.WIG 28 /r VMOVAPD xmm1, m128*/
    public void vmovapd(X86XmmRegister dst, X86Address src) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        /*Instruction VEX Prefix*/
        byte ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
        X86ManagedRegister vvvv_reg = X86ManagedRegister.NoRegister();
      /*a REX prefix is necessary only if an instruction references one of the
      extended registers or uses a 64-bit operand.*/
        byte ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                vvvv_reg,
                SET_VEX_L_128,
                SET_VEX_PP_66);
        emit8(ByteZero);
        emit8(ByteOne);
        // Instruction Opcode
        emit8(0x28);
        // Instruction Operands
        EmitOperand(dst.index(), src);
    }

    public void movupd(X86XmmRegister dst, X86Address src) {
        if (cpuHasAVXorAVX2FeatureFlag()) {
            vmovupd(dst, src);
            return;
        }
        emit8(0x66);
        emit8(0x0F);
        emit8(0x10);
        EmitOperand(dst.index(), src);
    }

    /*VEX.128.66.0F.WIG 10 /r VMOVUPD xmm1, m128*/
    public void vmovupd(X86XmmRegister dst, X86Address src) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        /*Instruction VEX Prefix*/
        byte ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
        X86ManagedRegister vvvv_reg = X86ManagedRegister.NoRegister();
      /*a REX prefix is necessary only if an instruction references one of the
      extended registers or uses a 64-bit operand.*/
        byte ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                vvvv_reg,
                SET_VEX_L_128,
                SET_VEX_PP_66);
        emit8(ByteZero);
        emit8(ByteOne);
        // Instruction Opcode
        emit8(0x10);
        // Instruction Operands
        EmitOperand(dst.index(), src);
    }

    public void movapd(X86Address dst, X86XmmRegister src) {
        if (cpuHasAVXorAVX2FeatureFlag()) {
            vmovapd(dst, src);
            return;
        }
        emit8(0x66);
        emit8(0x0F);
        emit8(0x29);
        EmitOperand(src.index(), dst);
    }

    /*VEX.128.66.0F.WIG 29 /r VMOVAPD m128, xmm1 */
    public void vmovapd(X86Address dst, X86XmmRegister src) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        /*Instruction VEX Prefix */
        byte ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
        X86ManagedRegister vvvv_reg = X86ManagedRegister.NoRegister();
      /*a REX prefix is necessary only if an instruction references one of the
      extended registers or uses a 64-bit operand.*/
        byte ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                vvvv_reg,
                SET_VEX_L_128,
                SET_VEX_PP_66);
        emit8(ByteZero);
        emit8(ByteOne);
        // Instruction Opcode
        emit8(0x29);
        // Instruction Operands
        EmitOperand(src.index(), dst);
    }

    public void movupd(X86Address dst, X86XmmRegister src) {
        if (cpuHasAVXorAVX2FeatureFlag()) {
            vmovupd(dst, src);
            return;
        }
        emit8(0x66);
        emit8(0x0F);
        emit8(0x11);
        EmitOperand(src.index(), dst);
    }

    /*VEX.128.66.0F.WIG 11 /r VMOVUPD m128, xmm1 */
    public void vmovupd(X86Address dst, X86XmmRegister src) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        /*Instruction VEX Prefix */
        byte ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
        X86ManagedRegister vvvv_reg = X86ManagedRegister.NoRegister();
      /*a REX prefix is necessary only if an instruction references one of the
      extended registers or uses a 64-bit operand.**/
        byte ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                vvvv_reg,
                SET_VEX_L_128,
                SET_VEX_PP_66);
        emit8(ByteZero);
        emit8(ByteOne);
        // Instruction Opcode
        emit8(0x11);
        // Instruction Operands
        EmitOperand(src.index(), dst);
    }

    public void flds(X86Address src) {
        emit8(0xD9);
        EmitOperand(0, src);
    }

    public void fsts(X86Address dst) {
        emit8(0xD9);
        EmitOperand(2, dst);
    }

    public void fstps(X86Address dst) {
        emit8(0xD9);
        EmitOperand(3, dst);
    }

    public void movsd(X86XmmRegister dst, X86Address src) {
        emit8(0xF2);
        emit8(0x0F);
        emit8(0x10);
        EmitOperand(dst.index(), src);
    }

    public void movsd(X86Address dst, X86XmmRegister src) {
        emit8(0xF2);
        emit8(0x0F);
        emit8(0x11);
        EmitOperand(src.index(), dst);
    }

    public void movsd(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0xF2);
        emit8(0x0F);
        emit8(0x11);
        EmitXmmRegisterOperand(src.index(), dst);
    }

    public void movhpd(X86XmmRegister dst, X86Address src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x16);
        EmitOperand(dst.index(), src);
    }

    public void movhpd(X86Address dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x17);
        EmitOperand(src.index(), dst);
    }

    public void addsd(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0xF2);
        emit8(0x0F);
        emit8(0x58);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void addsd(X86XmmRegister dst, X86Address src) {
        emit8(0xF2);
        emit8(0x0F);
        emit8(0x58);
        EmitOperand(dst.index(), src);
    }

    public void subsd(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0xF2);
        emit8(0x0F);
        emit8(0x5C);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void subsd(X86XmmRegister dst, X86Address src) {
        emit8(0xF2);
        emit8(0x0F);
        emit8(0x5C);
        EmitOperand(dst.index(), src);
    }

    public void mulsd(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0xF2);
        emit8(0x0F);
        emit8(0x59);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void mulsd(X86XmmRegister dst, X86Address src) {
        emit8(0xF2);
        emit8(0x0F);
        emit8(0x59);
        EmitOperand(dst.index(), src);
    }

    public void divsd(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0xF2);
        emit8(0x0F);
        emit8(0x5E);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void divsd(X86XmmRegister dst, X86Address src) {
        emit8(0xF2);
        emit8(0x0F);
        emit8(0x5E);
        EmitOperand(dst.index(), src);
    }

    public void addpd(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x58);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void vaddpd(X86XmmRegister dst, X86XmmRegister add_left, X86XmmRegister add_right) {
        byte ByteZero, ByteOne;
        ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
        ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                X86ManagedRegister.fromXmmRegister(add_left),
                SET_VEX_L_128,
                SET_VEX_PP_66);
        emit8(ByteZero);
        emit8(ByteOne);
        emit8(0x58);
        EmitXmmRegisterOperand(dst.index(), add_right);
    }

    public void subpd(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x5C);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void vsubpd(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2) {
        byte ByteZero, ByteOne;
        ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form*/ true);
        ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                X86ManagedRegister.fromXmmRegister(src1),
                SET_VEX_L_128,
                SET_VEX_PP_66);
        emit8(ByteZero);
        emit8(ByteOne);
        emit8(0x5C);
        EmitXmmRegisterOperand(dst.index(), src2);
    }

    public void mulpd(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x59);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void vmulpd(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte ByteZero, ByteOne;
        ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
        ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                X86ManagedRegister.fromXmmRegister(src1),
                SET_VEX_L_128,
                SET_VEX_PP_66);
        emit8(ByteZero);
        emit8(ByteOne);
        emit8(0x59);
        EmitXmmRegisterOperand(dst.index(), src2);
    }

    public void divpd(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x5E);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void vdivpd(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte ByteZero, ByteOne;
        ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
        ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                X86ManagedRegister.fromXmmRegister(src1),
                SET_VEX_L_128,
                SET_VEX_PP_66);
        emit8(ByteZero);
        emit8(ByteOne);
        emit8(0x5E);
        EmitXmmRegisterOperand(dst.index(), src2);
    }

    public void movdqa(X86XmmRegister dst, X86XmmRegister src) {
        if (cpuHasAVXorAVX2FeatureFlag()) {
            vmovdqa(dst, src);
            return;
        }
        emit8(0x66);
        emit8(0x0F);
        emit8(0x6F);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    /*VEX.128.66.0F.WIG 6F /r VMOVDQA xmm1, xmm2 */
    public void vmovdqa(X86XmmRegister dst, X86XmmRegister src) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        /*Instruction VEX Prefix */
        byte ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
        X86ManagedRegister vvvv_reg = X86ManagedRegister.NoRegister();
        byte ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                vvvv_reg,
                SET_VEX_L_128,
                SET_VEX_PP_66);
        emit8(ByteZero);
        emit8(ByteOne);
        // Instruction Opcode
        emit8(0x6F);
        // Instruction Operands
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void movdqa(X86XmmRegister dst, X86Address src) {
        if (cpuHasAVXorAVX2FeatureFlag()) {
            vmovdqa(dst, src);
            return;
        }
        emit8(0x66);
        emit8(0x0F);
        emit8(0x6F);
        EmitOperand(dst.index(), src);
    }

    /*VEX.128.66.0F.WIG 6F /r VMOVDQA xmm1, m128 */
    public void vmovdqa(X86XmmRegister dst, X86Address src) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        /*Instruction VEX Prefix */
        byte ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
        X86ManagedRegister vvvv_reg = X86ManagedRegister.NoRegister();
        byte ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                vvvv_reg,
                SET_VEX_L_128,
                SET_VEX_PP_66);
        emit8(ByteZero);
        emit8(ByteOne);
        // Instruction Opcode
        emit8(0x6F);
        // Instruction Operands
        EmitOperand(dst.index(), src);
    }

    public void movdqu(X86XmmRegister dst, X86Address src) {
        if (cpuHasAVXorAVX2FeatureFlag()) {
            vmovdqu(dst, src);
            return;
        }
        emit8(0xF3);
        emit8(0x0F);
        emit8(0x6F);
        EmitOperand(dst.index(), src);
    }

    /*VEX.128.F3.0F.WIG 6F /r VMOVDQU xmm1, m128 */
    public void vmovdqu(X86XmmRegister dst, X86Address src) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        /*Instruction VEX Prefix */
        byte ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
        X86ManagedRegister vvvv_reg = X86ManagedRegister.NoRegister();
        byte ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                vvvv_reg,
                SET_VEX_L_128,
                SET_VEX_PP_F3);
        emit8(ByteZero);
        emit8(ByteOne);
        // Instruction Opcode
        emit8(0x6F);
        // Instruction Operands
        EmitOperand(dst.index(), src);
    }

    public void movdqa(X86Address dst, X86XmmRegister src) {
        if (cpuHasAVXorAVX2FeatureFlag()) {
            vmovdqa(dst, src);
            return;
        }
        emit8(0x66);
        emit8(0x0F);
        emit8(0x7F);
        EmitOperand(src.index(), dst);
    }

    /*VEX.128.66.0F.WIG 7F /r VMOVDQA m128, xmm1 */
    public void vmovdqa(X86Address dst, X86XmmRegister src) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        /*Instruction VEX Prefix */
        byte ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
        X86ManagedRegister vvvv_reg = X86ManagedRegister.NoRegister();
        byte ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                vvvv_reg,
                SET_VEX_L_128,
                SET_VEX_PP_66);
        emit8(ByteZero);
        emit8(ByteOne);
        // Instruction Opcode
        emit8(0x7F);
        // Instruction Operands
        EmitOperand(src.index(), dst);
    }

    public void movdqu(X86Address dst, X86XmmRegister src) {
        if (cpuHasAVXorAVX2FeatureFlag()) {
            vmovdqu(dst, src);
            return;
        }
        emit8(0xF3);
        emit8(0x0F);
        emit8(0x7F);
        EmitOperand(src.index(), dst);
    }

    /*VEX.128.F3.0F.WIG 7F /r VMOVDQU m128, xmm1 */
    public void vmovdqu(X86Address dst, X86XmmRegister src) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        // Instruction VEX Prefix
        byte ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
        X86ManagedRegister vvvv_reg = X86ManagedRegister.NoRegister();
        byte ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                vvvv_reg,
                SET_VEX_L_128,
                SET_VEX_PP_F3);
        emit8(ByteZero);
        emit8(ByteOne);
        // Instruction Opcode
        emit8(0x7F);
        // Instruction Operands
        EmitOperand(src.index(), dst);
    }

    public void paddb(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xFC);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void vpaddb(X86XmmRegister dst, X86XmmRegister add_left, X86XmmRegister add_right) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte ByteOne, ByteZero;
        ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
        X86ManagedRegister vvvv_reg = X86ManagedRegister.fromXmmRegister(add_left);
        ByteOne = EmitVexPrefixByteOne(/*R=*/ false, vvvv_reg, SET_VEX_L_128, SET_VEX_PP_66);
        emit8(ByteZero);
        emit8(ByteOne);
        emit8(0xFC);
        EmitXmmRegisterOperand(dst.index(), add_right);
    }

    public void psubb(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xF8);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void vpsubb(X86XmmRegister dst, X86XmmRegister add_left, X86XmmRegister add_right) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte ByteZero, ByteOne;
        ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
        X86ManagedRegister vvvv_reg = X86ManagedRegister.fromXmmRegister(add_left);
        ByteOne = EmitVexPrefixByteOne(/*R=*/ false, vvvv_reg, SET_VEX_L_128, SET_VEX_PP_66);
        emit8(ByteZero);
        emit8(ByteOne);
        emit8(0xF8);
        EmitXmmRegisterOperand(dst.index(), add_right);
    }

    public void paddw(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xFD);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void vpaddw(X86XmmRegister dst, X86XmmRegister add_left, X86XmmRegister add_right) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte ByteZero, ByteOne;
        ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
        X86ManagedRegister vvvv_reg = X86ManagedRegister.fromXmmRegister(add_left);
        ByteOne = EmitVexPrefixByteOne(/*R=*/ false, vvvv_reg, SET_VEX_L_128, SET_VEX_PP_66);
        emit8(ByteZero);
        emit8(ByteOne);
        emit8(0xFD);
        EmitXmmRegisterOperand(dst.index(), add_right);
    }

    public void psubw(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xF9);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void vpsubw(X86XmmRegister dst, X86XmmRegister add_left, X86XmmRegister add_right) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte ByteZero, ByteOne;
        ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
        X86ManagedRegister vvvv_reg = X86ManagedRegister.fromXmmRegister(add_left);
        ByteOne = EmitVexPrefixByteOne(/*R=*/ false, vvvv_reg, SET_VEX_L_128, SET_VEX_PP_66);
        emit8(ByteZero);
        emit8(ByteOne);
        emit8(0xF9);
        EmitXmmRegisterOperand(dst.index(), add_right);
    }

    public void pmullw(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xD5);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void paddd(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xFE);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void vpaddd(X86XmmRegister dst, X86XmmRegister add_left, X86XmmRegister add_right) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte ByteZero, ByteOne;
        ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
        X86ManagedRegister vvvv_reg = X86ManagedRegister.fromXmmRegister(add_left);
        ByteOne = EmitVexPrefixByteOne(/*R=*/ false, vvvv_reg, SET_VEX_L_128, SET_VEX_PP_66);
        emit8(ByteZero);
        emit8(ByteOne);
        emit8(0xFE);
        EmitXmmRegisterOperand(dst.index(), add_right);
    }

    public void psubd(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xFA);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void vpsubd(X86XmmRegister dst, X86XmmRegister add_left, X86XmmRegister add_right) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte ByteZero, ByteOne;
        ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
        X86ManagedRegister vvvv_reg = X86ManagedRegister.fromXmmRegister(add_left);
        ByteOne = EmitVexPrefixByteOne(/*R=*/ false, vvvv_reg, SET_VEX_L_128, SET_VEX_PP_66);
        emit8(ByteZero);
        emit8(ByteOne);
        emit8(0xFA);
        EmitXmmRegisterOperand(dst.index(), add_right);
    }

    public void pmulld(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x40);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void vpmulld(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte ByteZero, ByteOne, ByteTwo;
        ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ false);
        ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                /*X=*/ false,
                /*B=*/ false,
                SET_VEX_M_0F_38);
        ByteTwo = EmitVexPrefixByteTwo(/*W=*/ false,
                X86ManagedRegister.fromXmmRegister(src1),
                SET_VEX_L_128,
                SET_VEX_PP_66);
        emit8(ByteZero);
        emit8(ByteOne);
        emit8(ByteTwo);
        emit8(0x40);
        EmitRegisterOperand(dst.index(), src2.index());
    }

    public void vpmullw(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte ByteZero, ByteOne;
        ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
        ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                X86ManagedRegister.fromXmmRegister(src1),
                SET_VEX_L_128,
                SET_VEX_PP_66);
        emit8(ByteZero);
        emit8(ByteOne);
        emit8(0xD5);
        EmitRegisterOperand(dst.index(), src2.index());
    }

    public void paddq(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xD4);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void vpaddq(X86XmmRegister dst, X86XmmRegister add_left, X86XmmRegister add_right) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte ByteZero, ByteOne;
        ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
        X86ManagedRegister vvvv_reg = X86ManagedRegister.fromXmmRegister(add_left);
        ByteOne = EmitVexPrefixByteOne(/*R=*/ false, vvvv_reg, SET_VEX_L_128, SET_VEX_PP_66);
        emit8(ByteZero);
        emit8(ByteOne);
        emit8(0xD4);
        EmitXmmRegisterOperand(dst.index(), add_right);
    }

    public void psubq(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xFB);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void vpsubq(X86XmmRegister dst, X86XmmRegister add_left, X86XmmRegister add_right) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte ByteZero, ByteOne;
        ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
        X86ManagedRegister vvvv_reg = X86ManagedRegister.fromXmmRegister(add_left);
        ByteOne = EmitVexPrefixByteOne(/*R=*/ false, vvvv_reg, SET_VEX_L_128, SET_VEX_PP_66);
        emit8(ByteZero);
        emit8(ByteOne);
        emit8(0xFB);
        EmitXmmRegisterOperand(dst.index(), add_right);
    }

    public void paddusb(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xDC);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void paddsb(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xEC);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void paddusw(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xDD);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void paddsw(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xED);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void psubusb(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xD8);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void psubsb(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xE8);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void psubusw(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xD9);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void psubsw(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xE9);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void cvtsi2ss(X86XmmRegister dst, X86CpuRegister src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0x2A);
        X86Operand operand = new X86Operand(src);
        EmitOperand(dst.index(), operand);
    }

    public void cvtsi2sd(X86XmmRegister dst, X86CpuRegister src) {
        emit8(0xF2);
        emit8(0x0F);
        emit8(0x2A);
        X86Operand operand = new X86Operand(src);
        EmitOperand(dst.index(), operand);
    }

    public void cvtss2si(X86CpuRegister dst, X86XmmRegister src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0x2D);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void cvtss2sd(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0x5A);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void cvtsd2si(X86CpuRegister dst, X86XmmRegister src) {
        emit8(0xF2);
        emit8(0x0F);
        emit8(0x2D);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void cvttss2si(X86CpuRegister dst, X86XmmRegister src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0x2C);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void cvttsd2si(X86CpuRegister dst, X86XmmRegister src) {
        emit8(0xF2);
        emit8(0x0F);
        emit8(0x2C);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void cvtsd2ss(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0xF2);
        emit8(0x0F);
        emit8(0x5A);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void cvtdq2ps(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x0F);
        emit8(0x5B);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void cvtdq2pd(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0xE6);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void comiss(X86XmmRegister a, X86XmmRegister b) {
        emit8(0x0F);
        emit8(0x2F);
        EmitXmmRegisterOperand(a.index(), b);
    }

    public void comiss(X86XmmRegister a, X86Address b) {
        emit8(0x0F);
        emit8(0x2F);
        EmitOperand(a.index(), b);
    }

    public void comisd(X86XmmRegister a, X86XmmRegister b) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x2F);
        EmitXmmRegisterOperand(a.index(), b);
    }

    public void comisd(X86XmmRegister a, X86Address b) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x2F);
        EmitOperand(a.index(), b);
    }

    public void ucomiss(X86XmmRegister a, X86XmmRegister b) {
        emit8(0x0F);
        emit8(0x2E);
        EmitXmmRegisterOperand(a.index(), b);
    }

    public void ucomiss(X86XmmRegister a, X86Address b) {
        emit8(0x0F);
        emit8(0x2E);
        EmitOperand(a.index(), b);
    }

    public void ucomisd(X86XmmRegister a, X86XmmRegister b) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x2E);
        EmitXmmRegisterOperand(a.index(), b);
    }

    public void ucomisd(X86XmmRegister a, X86Address b) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x2E);
        EmitOperand(a.index(), b);
    }

    public void roundsd(X86XmmRegister dst, X86XmmRegister src, X86Immediate imm) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x3A);
        emit8(0x0B);
        EmitXmmRegisterOperand(dst.index(), src);
        emit8(imm.value());
    }

    public void roundss(X86XmmRegister dst, X86XmmRegister src, X86Immediate imm) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x3A);
        emit8(0x0A);
        EmitXmmRegisterOperand(dst.index(), src);
        emit8(imm.value());
    }

    public void sqrtsd(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0xF2);
        emit8(0x0F);
        emit8(0x51);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void sqrtss(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0x51);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void xorpd(X86XmmRegister dst, X86Address src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x57);
        EmitOperand(dst.index(), src);
    }

    public void xorpd(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x57);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void xorps(X86XmmRegister dst, X86Address src) {
        emit8(0x0F);
        emit8(0x57);
        EmitOperand(dst.index(), src);
    }

    public void xorps(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x0F);
        emit8(0x57);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pxor(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xEF);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    /* VEX.128.66.0F.WIG EF /r VPXOR xmm1, xmm2, xmm3/m128 */
    public void vpxor(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte ByteZero, ByteOne;
        /* Instruction VEX Prefix */
        ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
      /* REX prefix is necessary only if an instruction references one of extended
      registers or uses a 64-bit operand. */
        ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                X86ManagedRegister.fromXmmRegister(src1),
                SET_VEX_L_128,
                SET_VEX_PP_66);
        emit8(ByteZero);
        emit8(ByteOne);
        // Instruction Opcode
        emit8(0xEF);
        // Instruction Operands
        EmitXmmRegisterOperand(dst.index(), src2);
    }

    /* VEX.128.0F.WIG 57 /r VXORPS xmm1,xmm2, xmm3/m128 */
    public void vxorps(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte ByteZero, ByteOne;
        /* Instruction VEX Prefix */
        ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
      /* REX prefix is necessary only if an instruction references one of extended
      registers or uses a 64-bit operand. */
        ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                X86ManagedRegister.fromXmmRegister(src1),
                SET_VEX_L_128,
                SET_VEX_PP_NONE);
        emit8(ByteZero);
        emit8(ByteOne);
        // Instruction Opcode
        emit8(0x57);
        // Instruction Operands
        EmitXmmRegisterOperand(dst.index(), src2);
    }

    /* VEX.128.66.0F.WIG 57 /r VXORPD xmm1,xmm2, xmm3/m128 */
    public void vxorpd(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte ByteZero, ByteOne;
        /* Instruction VEX Prefix */
        ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
      /* REX prefix is necessary only if an instruction references one of extended
      registers or uses a 64-bit operand. */
        ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                X86ManagedRegister.fromXmmRegister(src1),
                SET_VEX_L_128,
                SET_VEX_PP_66);
        emit8(ByteZero);
        emit8(ByteOne);
        // Instruction Opcode
        emit8(0x57);
        // Instruction Operands
        EmitXmmRegisterOperand(dst.index(), src2);
    }

    public void andpd(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x54);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void andpd(X86XmmRegister dst, X86Address src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x54);
        EmitOperand(dst.index(), src);
    }

    public void andps(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x0F);
        emit8(0x54);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void andps(X86XmmRegister dst, X86Address src) {
        emit8(0x0F);
        emit8(0x54);
        EmitOperand(dst.index(), src);
    }

    public void pand(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xDB);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    /* VEX.128.66.0F.WIG DB /r VPAND xmm1, xmm2, xmm3/m128 */
    public void vpand(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte ByteZero, ByteOne;
        /* Instruction VEX Prefix */
        ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
      /* REX prefix is necessary only if an instruction references one of extended
      registers or uses a 64-bit operand. */
        ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                X86ManagedRegister.fromXmmRegister(src1),
                SET_VEX_L_128,
                SET_VEX_PP_66);
        emit8(ByteZero);
        emit8(ByteOne);
        // Instruction Opcode
        emit8(0xDB);
        // Instruction Operands
        EmitXmmRegisterOperand(dst.index(), src2);
    }

    /* VEX.128.0F 54 /r VANDPS xmm1,xmm2, xmm3/m128 */
    public void vandps(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte ByteZero, ByteOne;
        /* Instruction VEX Prefix */
        ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
        ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                X86ManagedRegister.fromXmmRegister(src1),
                SET_VEX_L_128,
                SET_VEX_PP_NONE);
        emit8(ByteZero);
        emit8(ByteOne);
        // Instruction Opcode
        emit8(0x54);
        // Instruction Operands
        EmitXmmRegisterOperand(dst.index(), src2);
    }

    /* VEX.128.66.0F 54 /r VANDPD xmm1, xmm2, xmm3/m128 */
    public void vandpd(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte ByteZero, ByteOne;
        /* Instruction VEX Prefix */
        ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
      /* REX prefix is necessary only if an instruction references one of extended
      registers or uses a 64-bit operand. */
        ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                X86ManagedRegister.fromXmmRegister(src1),
                SET_VEX_L_128,
                SET_VEX_PP_66);
        emit8(ByteZero);
        emit8(ByteOne);
        // Instruction Opcode
        emit8(0x54);
        // Instruction Operands
        EmitXmmRegisterOperand(dst.index(), src2);
    }

    public void andnpd(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x55);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void andnps(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x0F);
        emit8(0x55);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pandn(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xDF);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    /* VEX.128.66.0F.WIG DF /r VPANDN xmm1, xmm2, xmm3/m128 */
    public void vpandn(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte ByteZero, ByteOne;
        /* Instruction VEX Prefix */
        ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
      /* REX prefix is necessary only if an instruction references one of extended
      registers or uses a 64-bit operand. */
        ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                X86ManagedRegister.fromXmmRegister(src1),
                SET_VEX_L_128,
                SET_VEX_PP_66);
        emit8(ByteZero);
        emit8(ByteOne);
        // Instruction Opcode
        emit8(0xDF);
        // Instruction Operands
        EmitXmmRegisterOperand(dst.index(), src2);
    }

    /* VEX.128.0F 55 /r VANDNPS xmm1, xmm2, xmm3/m128 */
    public void vandnps(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte ByteZero, ByteOne;
        /* Instruction VEX Prefix */
        ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
      /* REX prefix is necessary only if an instruction references one of extended
      registers or uses a 64-bit operand. */
        ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                X86ManagedRegister.fromXmmRegister(src1),
                SET_VEX_L_128,
                SET_VEX_PP_NONE);
        emit8(ByteZero);
        emit8(ByteOne);
        // Instruction Opcode
        emit8(0x55);
        // Instruction Operands
        EmitXmmRegisterOperand(dst.index(), src2);
    }

    /* VEX.128.66.0F 55 /r VANDNPD xmm1, xmm2, xmm3/m128 */
    public void vandnpd(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte ByteZero, ByteOne;
        /* Instruction VEX Prefix */
        ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
      /* REX prefix is necessary only if an instruction references one of extended
      registers or uses a 64-bit operand. */
        ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                X86ManagedRegister.fromXmmRegister(src1),
                SET_VEX_L_128,
                SET_VEX_PP_66);
        emit8(ByteZero);
        emit8(ByteOne);
        // Instruction Opcode
        emit8(0x55);
        // Instruction Operands
        EmitXmmRegisterOperand(dst.index(), src2);
    }

    public void orpd(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x56);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void orps(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x0F);
        emit8(0x56);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void andn(X86CpuRegister dst, X86CpuRegister src1, X86CpuRegister src2) {
        byte byte_zero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ false);
        byte byte_one = EmitVexPrefixByteOne(/*R=*/ false,
                /*X=*/ false,
                /*B=*/ false,
                SET_VEX_M_0F_38);
        byte byte_two = EmitVexPrefixByteTwo(/*W=*/ false,
                X86ManagedRegister.fromCpuRegister(src1),
                SET_VEX_L_128,
                SET_VEX_PP_NONE);
        emit8(byte_zero);
        emit8(byte_one);
        emit8(byte_two);
        // Opcode field
        emit8(0xF2);
        EmitRegisterOperand(dst.index(), src2.index());
    }

    public void por(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xEB);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    /* VEX.128.66.0F.WIG EB /r VPOR xmm1, xmm2, xmm3/m128 */
    public void vpor(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte ByteZero, ByteOne;
        /* Instruction VEX Prefix */
        ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
      /* REX prefix is necessary only if an instruction references one of extended
      registers or uses a 64-bit operand. */
        ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                X86ManagedRegister.fromXmmRegister(src1),
                SET_VEX_L_128,
                SET_VEX_PP_66);
        emit8(ByteZero);
        emit8(ByteOne);
        // Instruction Opcode
        emit8(0xEB);
        // Instruction Operands
        EmitXmmRegisterOperand(dst.index(), src2);
    }

    /* VEX.128.0F 56 /r VORPS xmm1,xmm2, xmm3/m128 */
    public void vorps(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte ByteZero, ByteOne;
        /* Instruction VEX Prefix */
        ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
      /* REX prefix is necessary only if an instruction references one of extended
      registers or uses a 64-bit operand. */
        ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                X86ManagedRegister.fromXmmRegister(src1),
                SET_VEX_L_128,
                SET_VEX_PP_NONE);
        emit8(ByteZero);
        emit8(ByteOne);
        // Instruction Opcode
        emit8(0x56);
        // Instruction Operands
        EmitXmmRegisterOperand(dst.index(), src2);
    }

    /* VEX.128.66.0F 56 /r VORPD xmm1,xmm2, xmm3/m128 */
    public void vorpd(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte ByteZero, ByteOne;
        /* Instruction VEX Prefix */
        ByteZero = EmitVexPrefixByteZero(/*is_twobyte_form=*/ true);
      /* REX prefix is necessary only if an instruction references one of extended
      registers or uses a 64-bit operand. */
        ByteOne = EmitVexPrefixByteOne(/*R=*/ false,
                X86ManagedRegister.fromXmmRegister(src1),
                SET_VEX_L_128,
                SET_VEX_PP_66);
        emit8(ByteZero);
        emit8(ByteOne);
        // Instruction Opcode
        emit8(0x56);
        // Instruction Operands
        EmitXmmRegisterOperand(dst.index(), src2);
    }

    public void pavgb(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xE0);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pavgw(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xE3);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void psadbw(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xF6);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pmaddwd(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xF5);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void vpmaddwd(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2) {
        CHECK(cpuHasAVXorAVX2FeatureFlag());
        byte ByteZero, ByteOne;
        ByteZero = EmitVexPrefixByteZero(/* is_twobyte_form=*/ true);
        X86ManagedRegister vvvv_reg = X86ManagedRegister.fromXmmRegister(src1);
        ByteOne = EmitVexPrefixByteOne(/*R=*/ false, vvvv_reg, SET_VEX_L_128, SET_VEX_PP_66);
        emit8(ByteZero);
        emit8(ByteOne);
        emit8(0xF5);
        EmitXmmRegisterOperand(dst.index(), src2);
    }

    public void phaddw(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x01);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void phaddd(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x02);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void haddps(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0xF2);
        emit8(0x0F);
        emit8(0x7C);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void haddpd(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x7C);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void phsubw(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x05);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void phsubd(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x06);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void hsubps(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0xF2);
        emit8(0x0F);
        emit8(0x7D);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void hsubpd(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x7D);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pminsb(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x38);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pmaxsb(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x3C);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pminsw(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xEA);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pmaxsw(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xEE);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pminsd(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x39);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pmaxsd(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x3D);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pminub(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xDA);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pmaxub(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xDE);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pminuw(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x3A);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pmaxuw(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x3E);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pminud(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x3B);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pmaxud(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x3F);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void minps(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x0F);
        emit8(0x5D);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void maxps(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x0F);
        emit8(0x5F);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void minpd(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x5D);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void maxpd(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x5F);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pcmpeqb(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x74);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pcmpeqw(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x75);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pcmpeqd(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x76);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pcmpeqq(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x29);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pcmpgtb(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x64);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pcmpgtw(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x65);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pcmpgtd(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x66);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pcmpgtq(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x37);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void shufpd(X86XmmRegister dst, X86XmmRegister src, X86Immediate imm) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xC6);
        EmitXmmRegisterOperand(dst.index(), src);
        emit8(imm.value());
    }

    public void shufps(X86XmmRegister dst, X86XmmRegister src, X86Immediate imm) {
        emit8(0x0F);
        emit8(0xC6);
        EmitXmmRegisterOperand(dst.index(), src);
        emit8(imm.value());
    }

    public void pshufd(X86XmmRegister dst, X86XmmRegister src, X86Immediate imm) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x70);
        EmitXmmRegisterOperand(dst.index(), src);
        emit8(imm.value());
    }

    public void punpcklbw(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x60);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void punpcklwd(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x61);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void punpckldq(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x62);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void punpcklqdq(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x6C);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void punpckhbw(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x68);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void punpckhwd(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x69);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void punpckhdq(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x6A);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void punpckhqdq(X86XmmRegister dst, X86XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x6D);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void psllw(X86XmmRegister reg, X86Immediate shift_count) {
        CHECK(shift_count.isUInt8());
        emit8(0x66);
        emit8(0x0F);
        emit8(0x71);
        EmitXmmRegisterOperand(6, reg);
        emit8(shift_count.value());
    }

    public void pslld(X86XmmRegister reg, X86Immediate shift_count) {
        CHECK(shift_count.isUInt8());
        emit8(0x66);
        emit8(0x0F);
        emit8(0x72);
        EmitXmmRegisterOperand(6, reg);
        emit8(shift_count.value());
    }

    public void psllq(X86XmmRegister reg, X86Immediate shift_count) {
        CHECK(shift_count.isUInt8());
        emit8(0x66);
        emit8(0x0F);
        emit8(0x73);
        EmitXmmRegisterOperand(6, reg);
        emit8(shift_count.value());
    }

    public void psraw(X86XmmRegister reg, X86Immediate shift_count) {
        CHECK(shift_count.isUInt8());
        emit8(0x66);
        emit8(0x0F);
        emit8(0x71);
        EmitXmmRegisterOperand(4, reg);
        emit8(shift_count.value());
    }

    public void psrad(X86XmmRegister reg, X86Immediate shift_count) {
        CHECK(shift_count.isUInt8());
        emit8(0x66);
        emit8(0x0F);
        emit8(0x72);
        EmitXmmRegisterOperand(4, reg);
        emit8(shift_count.value());
    }

    public void psrlw(X86XmmRegister reg, X86Immediate shift_count) {
        CHECK(shift_count.isUInt8());
        emit8(0x66);
        emit8(0x0F);
        emit8(0x71);
        EmitXmmRegisterOperand(2, reg);
        emit8(shift_count.value());
    }

    public void psrld(X86XmmRegister reg, X86Immediate shift_count) {
        CHECK(shift_count.isUInt8());
        emit8(0x66);
        emit8(0x0F);
        emit8(0x72);
        EmitXmmRegisterOperand(2, reg);
        emit8(shift_count.value());
    }

    public void psrlq(X86XmmRegister reg, X86Immediate shift_count) {
        CHECK(shift_count.isUInt8());
        emit8(0x66);
        emit8(0x0F);
        emit8(0x73);
        EmitXmmRegisterOperand(2, reg);
        emit8(shift_count.value());
    }

    public void psrldq(X86XmmRegister reg, X86Immediate shift_count) {
        CHECK(shift_count.isUInt8());
        emit8(0x66);
        emit8(0x0F);
        emit8(0x73);
        EmitXmmRegisterOperand(3, reg);
        emit8(shift_count.value());
    }

    public void fldl(X86Address src) {
        emit8(0xDD);
        EmitOperand(0, src);
    }

    public void fstl(X86Address dst) {
        emit8(0xDD);
        EmitOperand(2, dst);
    }

    public void fstpl(X86Address dst) {
        emit8(0xDD);
        EmitOperand(3, dst);
    }

    public void fstsw() {
        emit8(0x9B);
        emit8(0xDF);
        emit8(0xE0);
    }

    public void fnstcw(X86Address dst) {
        emit8(0xD9);
        EmitOperand(7, dst);
    }

    public void fldcw(X86Address src) {
        emit8(0xD9);
        EmitOperand(5, src);
    }

    public void fistpl(X86Address dst) {
        emit8(0xDF);
        EmitOperand(7, dst);
    }

    public void fistps(X86Address dst) {
        emit8(0xDB);
        EmitOperand(3, dst);
    }

    public void fildl(X86Address src) {
        emit8(0xDF);
        EmitOperand(5, src);
    }

    public void filds(X86Address src) {
        emit8(0xDB);
        EmitOperand(0, src);
    }

    public void fincstp() {
        emit8(0xD9);
        emit8(0xF7);
    }

    public void ffree(X86Immediate index) {
        CHECK_LT(index.value(), 7);
        emit8(0xDD);
        emit8(0xC0 + index.value());
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

    private boolean try_xchg_eax(X86CpuRegister dst, X86CpuRegister src) {
        if (src != EAX && dst != EAX) {
            return false;
        }
        if (dst == EAX) {
            dst = src;
        }
        emit8(0x90 + dst.index());
        return true;
    }

    public void xchgb(X86ByteRegister dst, X86ByteRegister src) {
        emit8(0x86);
        EmitRegisterOperand(dst.index(), src.index());
    }

    public void xchgb(X86ByteRegister reg, X86Address address) {
        emit8(0x86);
        EmitOperand(reg.index(), address);
    }

    public void xchgw(X86CpuRegister dst, X86CpuRegister src) {
        EmitOperandSizeOverride();
        if (try_xchg_eax(dst, src)) {
            // A short version for AX.
            return;
        }
        // General case.
        emit8(0x87);
        EmitRegisterOperand(dst.index(), src.index());
    }

    public void xchgw(X86CpuRegister reg, X86Address address) {
        EmitOperandSizeOverride();
        emit8(0x87);
        EmitOperand(reg.index(), address);
    }

    public void xchgl(X86CpuRegister dst, X86CpuRegister src) {
        if (try_xchg_eax(dst, src)) {
            // A short version for EAX.
            return;
        }
        // General case.
        emit8(0x87);
        EmitRegisterOperand(dst.index(), src.index());
    }

    public void xchgl(X86CpuRegister reg, X86Address address) {
        emit8(0x87);
        EmitOperand(reg.index(), address);
    }

    public void cmpb(X86Address address, X86Immediate imm) {
        emit8(0x80);
        EmitOperand(7, address);
        emit8(imm.value() & 0xFF);
    }

    public void cmpw(X86Address address, X86Immediate imm) {
        emit8(0x66);
        EmitComplex(7, address, imm, /* is_16_op= */ true);
    }

    public void cmpl(X86CpuRegister reg, X86Immediate imm) {
        EmitComplex(7, new X86Operand(reg), imm);
    }

    public void cmpl(X86CpuRegister reg0, X86CpuRegister reg1) {
        emit8(0x3B);
        X86Operand operand = new X86Operand(reg1);
        EmitOperand(reg0.index(), operand);
    }

    public void cmpl(X86CpuRegister reg, X86Address address) {
        emit8(0x3B);
        EmitOperand(reg.index(), address);
    }

    public void addl(X86CpuRegister dst, X86CpuRegister src) {
        emit8(0x03);
        EmitRegisterOperand(dst.index(), src.index());
    }

    public void addl(X86CpuRegister reg, X86Address address) {
        emit8(0x03);
        EmitOperand(reg.index(), address);
    }

    public void cmpl(X86Address address, X86CpuRegister reg) {
        emit8(0x39);
        EmitOperand(reg.index(), address);
    }

    public void cmpl(X86Address address, X86Immediate imm) {
        EmitComplex(7, address, imm);
    }

    public void testl(X86CpuRegister reg1, X86CpuRegister reg2) {
        emit8(0x85);
        EmitRegisterOperand(reg1.index(), reg2.index());
    }

    public void testl(X86CpuRegister reg, X86Address address) {
        emit8(0x85);
        EmitOperand(reg.index(), address);
    }

    public void testl(X86CpuRegister reg, X86Immediate immediate) {
        // For registers that have a byte variant (EAX, EBX, ECX, and EDX)
        // we only test the byte register to keep the encoding short.
        if (immediate.isUInt8() && reg.index() < kFirstByteUnsafeRegister) {
            // Use zero-extended 8-bit immediate.
            if (reg == EAX) {
                emit8(0xA8);
            } else {
                emit8(0xF6);
                emit8(0xC0 + reg.index());
            }
            emit8(immediate.value() & 0xFF);
        } else if (reg == EAX) {
            // Use short form if the destination is EAX.
            emit8(0xA9);
            EmitImmediate(immediate);
        } else {
            emit8(0xF7);
            EmitOperand(0, new X86Operand(reg));
            EmitImmediate(immediate);
        }
    }

    public void testb(X86Address dst, X86Immediate imm) {
        emit8(0xF6);
        EmitOperand(EAX.index(), dst);
        CHECK(imm.isInt8());
        emit8(imm.value() & 0xFF);
    }

    public void testl(X86Address dst, X86Immediate imm) {
        emit8(0xF7);
        EmitOperand(0, dst);
        EmitImmediate(imm);
    }

    public void andl(X86CpuRegister dst, X86CpuRegister src) {
        emit8(0x23);
        X86Operand operand = new X86Operand(src);
        EmitOperand(dst.index(), operand);
    }

    public void andl(X86CpuRegister reg, X86Address address) {
        emit8(0x23);
        EmitOperand(reg.index(), address);
    }

    public void andl(X86CpuRegister dst, X86Immediate imm) {
        EmitComplex(4, new X86Operand(dst), imm);
    }

    public void andw(X86Address address, X86Immediate imm) {
        CHECK(imm.isUInt16() || imm.isInt16());
        EmitOperandSizeOverride();
        EmitComplex(4, address, imm, /* is_16_op= */ true);
    }

    public void orl(X86CpuRegister dst, X86CpuRegister src) {
        emit8(0x0B);
        X86Operand operand = new X86Operand(src);
        EmitOperand(dst.index(), operand);
    }

    public void orl(X86CpuRegister reg, X86Address address) {
        emit8(0x0B);
        EmitOperand(reg.index(), address);
    }

    public void orl(X86CpuRegister dst, X86Immediate imm) {
        EmitComplex(1, new X86Operand(dst), imm);
    }

    public void xorl(X86CpuRegister dst, X86CpuRegister src) {
        emit8(0x33);
        X86Operand operand = new X86Operand(src);
        EmitOperand(dst.index(), operand);
    }

    public void xorl(X86CpuRegister reg, X86Address address) {
        emit8(0x33);
        EmitOperand(reg.index(), address);
    }

    public void xorl(X86CpuRegister dst, X86Immediate imm) {
        EmitComplex(6, new X86Operand(dst), imm);
    }

    public void addl(X86CpuRegister reg, X86Immediate imm) {
        EmitComplex(0, new X86Operand(reg), imm);
    }

    public void addl(X86Address address, X86CpuRegister reg) {
        emit8(0x01);
        EmitOperand(reg.index(), address);
    }

    public void addl(X86Address address, X86Immediate imm) {
        EmitComplex(0, address, imm);
    }

    public void addw(X86Address address, X86Immediate imm) {
        CHECK(imm.isUInt16() || imm.isInt16());
        emit8(0x66);
        EmitComplex(0, address, imm, /* is_16_op= */ true);
    }

    public void addw(X86CpuRegister reg, X86Immediate imm) {
        CHECK(imm.isUInt16() || imm.isInt16());
        emit8(0x66);
        EmitComplex(0, new X86Operand(reg), imm, /* is_16_op= */ true);
    }

    public void adcl(X86CpuRegister reg, X86Immediate imm) {
        EmitComplex(2, new X86Operand(reg), imm);
    }

    public void adcl(X86CpuRegister dst, X86CpuRegister src) {
        emit8(0x13);
        X86Operand operand = new X86Operand(src);
        EmitOperand(dst.index(), operand);
    }

    public void adcl(X86CpuRegister dst, X86Address address) {
        emit8(0x13);
        EmitOperand(dst.index(), address);
    }

    public void subl(X86CpuRegister dst, X86CpuRegister src) {
        emit8(0x2B);
        X86Operand operand = new X86Operand(src);
        EmitOperand(dst.index(), operand);
    }

    public void subl(X86CpuRegister reg, X86Immediate imm) {
        EmitComplex(5, new X86Operand(reg), imm);
    }

    public void subl(X86CpuRegister reg, X86Address address) {
        emit8(0x2B);
        EmitOperand(reg.index(), address);
    }

    public void subl(X86Address address, X86CpuRegister reg) {
        emit8(0x29);
        EmitOperand(reg.index(), address);
    }

    public void cdq() {
        emit8(0x99);
    }

    public void idivl(X86CpuRegister reg) {
        emit8(0xF7);
        emit8(0xF8 | reg.index());
    }

    public void divl(X86CpuRegister reg) {
        emit8(0xF7);
        emit8(0xF0 | reg.index());
    }

    public void imull(X86CpuRegister dst, X86CpuRegister src) {
        emit8(0x0F);
        emit8(0xAF);
        X86Operand operand = new X86Operand(src);
        EmitOperand(dst.index(), operand);
    }

    public void imull(X86CpuRegister dst, X86CpuRegister src, X86Immediate imm) {
        // See whether imm can be represented as a sign-extended 8bit value.
        if (imm.isInt8()) {
            // Sign-extension works.
            emit8(0x6B);
            X86Operand operand = new X86Operand(src);
            EmitOperand(dst.index(), operand);
            emit8(imm.value() & 0xFF);
        } else {
            // Not representable, use full immediate.
            emit8(0x69);
            X86Operand operand = new X86Operand(src);
            EmitOperand(dst.index(), operand);
            EmitImmediate(imm);
        }
    }

    public void imull(X86CpuRegister reg, X86Immediate imm) {
        imull(reg, reg, imm);
    }

    public void imull(X86CpuRegister reg, X86Address address) {
        emit8(0x0F);
        emit8(0xAF);
        EmitOperand(reg.index(), address);
    }

    public void imull(X86CpuRegister reg) {
        emit8(0xF7);
        EmitOperand(5, new X86Operand(reg));
    }

    public void imull(X86Address address) {
        emit8(0xF7);
        EmitOperand(5, address);
    }

    public void mull(X86CpuRegister reg) {
        emit8(0xF7);
        EmitOperand(4, new X86Operand(reg));
    }

    public void mull(X86Address address) {
        emit8(0xF7);
        EmitOperand(4, address);
    }

    public void sbbl(X86CpuRegister dst, X86CpuRegister src) {
        emit8(0x1B);
        X86Operand operand = new X86Operand(src);
        EmitOperand(dst.index(), operand);
    }

    public void sbbl(X86CpuRegister reg, X86Immediate imm) {
        EmitComplex(3, new X86Operand(reg), imm);
    }

    public void sbbl(X86CpuRegister dst, X86Address address) {
        emit8(0x1B);
        EmitOperand(dst.index(), address);
    }

    public void sbbl(X86Address address, X86CpuRegister src) {
        emit8(0x19);
        EmitOperand(src.index(), address);
    }

    public void incl(X86CpuRegister reg) {
        emit8(0x40 + reg.index());
    }

    public void incl(X86Address address) {
        emit8(0xFF);
        EmitOperand(0, address);
    }

    public void decl(X86CpuRegister reg) {
        emit8(0x48 + reg.index());
    }

    public void decl(X86Address address) {
        emit8(0xFF);
        EmitOperand(1, address);
    }

    public void shll(X86CpuRegister reg, X86Immediate imm) {
        EmitGenericShift(4, new X86Operand(reg), imm);
    }

    public void shll(X86CpuRegister operand, X86CpuRegister shifter) {
        EmitGenericShift(4, new X86Operand(operand), shifter);
    }

    public void shll(X86Address address, X86Immediate imm) {
        EmitGenericShift(4, address, imm);
    }

    public void shll(X86Address address, X86CpuRegister shifter) {
        EmitGenericShift(4, address, shifter);
    }

    public void shrl(X86CpuRegister reg, X86Immediate imm) {
        EmitGenericShift(5, new X86Operand(reg), imm);
    }

    public void shrl(X86CpuRegister operand, X86CpuRegister shifter) {
        EmitGenericShift(5, new X86Operand(operand), shifter);
    }

    public void shrl(X86Address address, X86Immediate imm) {
        EmitGenericShift(5, address, imm);
    }

    public void shrl(X86Address address, X86CpuRegister shifter) {
        EmitGenericShift(5, address, shifter);
    }

    public void sarl(X86CpuRegister reg, X86Immediate imm) {
        EmitGenericShift(7, new X86Operand(reg), imm);
    }

    public void sarl(X86CpuRegister operand, X86CpuRegister shifter) {
        EmitGenericShift(7, new X86Operand(operand), shifter);
    }

    public void sarl(X86Address address, X86Immediate imm) {
        EmitGenericShift(7, address, imm);
    }

    public void sarl(X86Address address, X86CpuRegister shifter) {
        EmitGenericShift(7, address, shifter);
    }

    public void shld(X86CpuRegister dst, X86CpuRegister src, X86CpuRegister shifter) {
        CHECK_EQ(ECX.index(), shifter.index());
        emit8(0x0F);
        emit8(0xA5);
        EmitRegisterOperand(src.index(), dst.index());
    }

    public void shld(X86CpuRegister dst, X86CpuRegister src, X86Immediate imm) {
        emit8(0x0F);
        emit8(0xA4);
        EmitRegisterOperand(src.index(), dst.index());
        emit8(imm.value() & 0xFF);
    }

    public void shrd(X86CpuRegister dst, X86CpuRegister src, X86CpuRegister shifter) {
        CHECK_EQ(ECX.index(), shifter.index());
        emit8(0x0F);
        emit8(0xAD);
        EmitRegisterOperand(src.index(), dst.index());
    }

    public void shrd(X86CpuRegister dst, X86CpuRegister src, X86Immediate imm) {
        emit8(0x0F);
        emit8(0xAC);
        EmitRegisterOperand(src.index(), dst.index());
        emit8(imm.value() & 0xFF);
    }

    public void roll(X86CpuRegister reg, X86Immediate imm) {
        EmitGenericShift(0, new X86Operand(reg), imm);
    }

    public void roll(X86CpuRegister operand, X86CpuRegister shifter) {
        EmitGenericShift(0, new X86Operand(operand), shifter);
    }

    public void rorl(X86CpuRegister reg, X86Immediate imm) {
        EmitGenericShift(1, new X86Operand(reg), imm);
    }

    public void rorl(X86CpuRegister operand, X86CpuRegister shifter) {
        EmitGenericShift(1, new X86Operand(operand), shifter);
    }

    public void negl(X86CpuRegister reg) {
        emit8(0xF7);
        EmitOperand(3, new X86Operand(reg));
    }

    public void notl(X86CpuRegister reg) {
        emit8(0xF7);
        emit8(0xD0 | reg.index());
    }

    public void enter(X86Immediate imm) {
        emit8(0xC8);
        CHECK(imm.isUInt16());
        emit8(imm.value() & 0xFF);
        emit8((imm.value() >> 8) & 0xFF);
        emit8(0x00);
    }

    public void leave() {
        emit8(0xC9);
    }

    public void ret() {
        emit8(0xC3);
    }

    public void ret(X86Immediate imm) {
        emit8(0xC2);
        CHECK(imm.isUInt16());
        emit8(imm.value() & 0xFF);
        emit8((imm.value() >> 8) & 0xFF);
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

    public void jecxz(X86NearLabel label) {
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

    public void jmp(X86CpuRegister reg) {
        emit8(0xFF);
        EmitRegisterOperand(4, reg.index());
    }

    public void jmp(X86Address address) {
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

    public void repe_cmpsb() {
        emit8(0xF3);
        emit8(0xA6);
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

    public void rep_movsb() {
        emit8(0xF3);
        emit8(0xA4);
    }

    public void rep_movsw() {
        emit8(0x66);
        emit8(0xF3);
        emit8(0xA5);
    }

    public void rep_movsl() {
        emit8(0xF3);
        emit8(0xA5);
    }

    public X86Assembler lock() {
        emit8(0xF0);
        return this;
    }

    public void cmpxchgb(X86Address address, X86ByteRegister reg) {
        emit8(0x0F);
        emit8(0xB0);
        EmitOperand(reg.index(), address);
    }

    public void cmpxchgw(X86Address address, X86CpuRegister reg) {
        EmitOperandSizeOverride();
        emit8(0x0F);
        emit8(0xB1);
        EmitOperand(reg.index(), address);
    }

    public void cmpxchgl(X86Address address, X86CpuRegister reg) {
        emit8(0x0F);
        emit8(0xB1);
        EmitOperand(reg.index(), address);
    }

    public void cmpxchg8b(X86Address address) {
        emit8(0x0F);
        emit8(0xC7);
        EmitOperand(1, address);
    }

    public void xaddb(X86Address address, X86ByteRegister reg) {
        emit8(0x0F);
        emit8(0xC0);
        EmitOperand(reg.index(), address);
    }

    public void xaddw(X86Address address, X86CpuRegister reg) {
        EmitOperandSizeOverride();
        emit8(0x0F);
        emit8(0xC1);
        EmitOperand(reg.index(), address);
    }

    public void xaddl(X86Address address, X86CpuRegister reg) {
        emit8(0x0F);
        emit8(0xC1);
        EmitOperand(reg.index(), address);
    }

    public void mfence() {
        emit8(0x0F);
        emit8(0xAE);
        emit8(0xF0);
    }

    public X86Assembler fs() {
        // TODO: fs is a prefix and not an instruction
        emit8(0x64);
        return this;
    }

    public X86Assembler gs() {
        // TODO: fs is a prefix and not an instruction
        emit8(0x65);
        return this;
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