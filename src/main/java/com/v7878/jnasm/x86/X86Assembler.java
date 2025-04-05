package com.v7878.jnasm.x86;

import static com.v7878.jnasm.x86.CpuRegister.EAX;
import static com.v7878.jnasm.x86.CpuRegister.ECX;
import static com.v7878.jnasm.x86.CpuRegister.kFirstByteUnsafeRegister;

import com.v7878.jnasm.Assembler;
import com.v7878.jnasm.AssemblerFixup;
import com.v7878.jnasm.ExternalLabel;
import com.v7878.jnasm.Label;
import com.v7878.jnasm.Utils;

public class X86Assembler extends Assembler implements X86AssemblerI {
    private static final int GET_REX_R = 0x04;
    private static final int GET_REX_X = 0x02;
    private static final int GET_REX_B = 0x01;
    private static final int SET_VEX_R = 0x80;
    private static final int SET_VEX_X = 0x40;
    private static final int SET_VEX_B = 0x20;
    private static final int SET_VEX_M_0F = 0x01;
    private static final int SET_VEX_M_0F_38 = 0x02;
    private static final int SET_VEX_M_0F_3A = 0x03;
    private static final int SET_VEX_W = 0x80;
    private static final int SET_VEX_L_128 = 0x00;
    private static final int SET_VEL_L_256 = 0x04;
    private static final int SET_VEX_PP_NONE = 0x00;
    private static final int SET_VEX_PP_66 = 0x01;
    private static final int SET_VEX_PP_F3 = 0x02;
    private static final int SET_VEX_PP_F2 = 0x03;
    private static final int TWO_BYTE_VEX = 0xC5;
    private static final int THREE_BYTE_VEX = 0xC4;
    private static final int VEX_INIT = 0x00;

    private final boolean has_AVX_or_AVX2;

    public X86Assembler(boolean has_AVX_or_AVX2) {
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

    private void EmitRegisterOperand(int rm, int reg) {
        CHECK_GE(rm, 0);
        CHECK_LT(rm, 8);
        emit8(0xC0 + (rm << 3) + reg);
    }

    private void EmitXmmRegisterOperand(int rm, XmmRegister reg) {
        EmitRegisterOperand(rm, reg.index());
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
            emit8(imm.value() & 0xFF);
            emit8(imm.value() >> 8);
        } else {
            emit32(imm.value());
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

    private void EmitGenericShift(int reg_or_opcode, Operand operand, Immediate imm) {
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

    private void EmitGenericShift(int reg_or_opcode, Operand operand, CpuRegister shifter) {
        CHECK_EQ(shifter.index(), ECX.index());
        emit8(0xD3);
        EmitOperand(reg_or_opcode, operand);
    }

    private byte EmitVexPrefixByteZero(boolean is_twobyte_form) {
         /* Vex Byte 0,
          Bits [7:0] must contain the value 11000101b (0xC5) for 2-byte Vex
          Bits [7:0] must contain the value 11000100b (0xC4) for 3-byte Vex */
        int vex_prefix = 0xC0;
        if (is_twobyte_form) {
            // 2-Byte Vex
            vex_prefix |= TWO_BYTE_VEX;
        } else {
            // 3-Byte Vex
            vex_prefix |= THREE_BYTE_VEX;
        }
        return (byte) vex_prefix;
    }

    @SuppressWarnings("SameParameterValue")
    private byte EmitVexPrefixByteOne(boolean R, boolean X, boolean B, int SET_VEX_M) {
        /* Vex Byte 1, */
        int vex_prefix = VEX_INIT;
         /* Bit[7] This bit needs to be set to '1'
          otherwise the instruction is LES or LDS */
        if (!R) {
            // R .
            vex_prefix |= SET_VEX_R;
        }
         /* Bit[6] This bit needs to be set to '1'
          otherwise the instruction is LES or LDS */
        if (!X) {
            // X .
            vex_prefix |= SET_VEX_X;
        }
        /* Bit[5] This bit needs to be set to '1' */
        if (!B) {
            // B .
            vex_prefix |= SET_VEX_B;
        }
        /* Bits[4:0], */
        vex_prefix |= SET_VEX_M;
        return (byte) vex_prefix;
    }

    @SuppressWarnings("SameParameterValue")
    private byte EmitVexPrefixByteOne(boolean R,
                                      X86ManagedRegister operand,
                                      int SET_VEX_L,
                                      int SET_VEX_PP) {
        /* Vex Byte 1, */
        int vex_prefix = VEX_INIT;
         /* Bit[7] This bit needs to be set to '1'
          otherwise the instruction is LES or LDS */
        if (!R) {
            // R .
            vex_prefix |= SET_VEX_R;
        }
        /* Bits[6:3] - 'vvvv' the source or dest register specifier */
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
         /* Bit[2] - "L" If VEX.L = 1 indicates 256-bit vector operation ,
          VEX.L = 0 indicates 128 bit vector operation */
        vex_prefix |= SET_VEX_L;
        /* Bits[1:0] -  "pp" */
        vex_prefix |= SET_VEX_PP;
        return (byte) vex_prefix;
    }

    @SuppressWarnings("SameParameterValue")
    private byte EmitVexPrefixByteTwo(boolean W,
                                      X86ManagedRegister operand,
                                      int SET_VEX_L,
                                      int SET_VEX_PP) {
        /* Vex Byte 2, */
        int vex_prefix = VEX_INIT;
         /* Bit[7] This bits needs to be set to '1' with default value.
          When using C4H form of VEX prefix, W value is ignored */
        if (W) {
            vex_prefix |= SET_VEX_W;
        }
        /* Bits[6:3] - 'vvvv' the source or dest register specifier */
        if (operand.isXmmRegister()) {
            XmmRegister vvvv = operand.asXmmRegister();
            int inverted_reg = 15 - vvvv.index();
            vex_prefix |= ((inverted_reg & 0x0F) << 3);
        } else if (operand.isCpuRegister()) {
            CpuRegister vvvv = operand.asCpuRegister();
            int inverted_reg = 15 - vvvv.index();
            vex_prefix |= ((inverted_reg & 0x0F) << 3);
        }
         /* Bit[2] - "L" If VEX.L = 1 indicates 256-bit vector operation ,
          VEX.L = 0 indicates 128 bit vector operation */
        vex_prefix |= SET_VEX_L;
        // Bits[1:0] -  "pp"
        vex_prefix |= SET_VEX_PP;
        return (byte) vex_prefix;
    }

    public void call(CpuRegister reg) {
        emit8(0xFF);
        EmitRegisterOperand(2, reg.index());
    }

    public void call(Address address) {
        emit8(0xFF);
        EmitOperand(2, address);
    }

    public void call(Label label) {
        emit8(0xE8);
        final int kSize = 5;
        // Offset by one because we already have emitted the opcode.
        EmitLabel(label, kSize - 1);
    }

    public void call(ExternalLabel label) {
        // TODO assert label.address() is 32 bit
        emit8(0xE8);
        emit32((int) label.address());
    }

    public void pushl(CpuRegister reg) {
        emit8(0x50 + reg.index());
    }

    public void pushl(Address address) {
        emit8(0xFF);
        EmitOperand(6, address);
    }

    public void pushl(Immediate imm) {
        if (imm.isInt8()) {
            emit8(0x6A);
            emit8(imm.value() & 0xFF);
        } else {
            emit8(0x68);
            EmitImmediate(imm);
        }
    }

    public void popl(CpuRegister reg) {
        emit8(0x58 + reg.index());
    }

    public void popl(Address address) {
        emit8(0x8F);
        EmitOperand(0, address);
    }

    public void movl(CpuRegister dst, Immediate imm) {
        emit8(0xB8 + dst.index());
        EmitImmediate(imm);
    }

    public void movl(CpuRegister dst, CpuRegister src) {
        emit8(0x89);
        EmitRegisterOperand(src.index(), dst.index());
    }

    public void movl(CpuRegister dst, Address src) {
        emit8(0x8B);
        EmitOperand(dst.index(), src);
    }

    public void movl(Address dst, CpuRegister src) {
        emit8(0x89);
        EmitOperand(src.index(), dst);
    }

    public void movl(Address dst, Immediate imm) {
        emit8(0xC7);
        EmitOperand(0, dst);
        EmitImmediate(imm);
    }

    public void movl(Address dst, Label lbl) {
        emit8(0xC7);
        EmitOperand(0, dst);
        EmitLabel(lbl, dst.length + 5);
    }

    public void movntl(Address dst, CpuRegister src) {
        emit8(0x0F);
        emit8(0xC3);
        EmitOperand(src.index(), dst);
    }

    public void blsi(CpuRegister dst, CpuRegister src) {
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

    public void blsmsk(CpuRegister dst, CpuRegister src) {
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

    public void blsr(CpuRegister dst, CpuRegister src) {
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

    public void bswapl(CpuRegister dst) {
        emit8(0x0F);
        emit8(0xC8 + dst.index());
    }

    public void bsfl(CpuRegister dst, CpuRegister src) {
        emit8(0x0F);
        emit8(0xBC);
        EmitRegisterOperand(dst.index(), src.index());
    }

    public void bsfl(CpuRegister dst, Address src) {
        emit8(0x0F);
        emit8(0xBC);
        EmitOperand(dst.index(), src);
    }

    public void bsrl(CpuRegister dst, CpuRegister src) {
        emit8(0x0F);
        emit8(0xBD);
        EmitRegisterOperand(dst.index(), src.index());
    }

    public void bsrl(CpuRegister dst, Address src) {
        emit8(0x0F);
        emit8(0xBD);
        EmitOperand(dst.index(), src);
    }

    public void popcntl(CpuRegister dst, CpuRegister src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0xB8);
        EmitRegisterOperand(dst.index(), src.index());
    }

    public void popcntl(CpuRegister dst, Address src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0xB8);
        EmitOperand(dst.index(), src);
    }

    public void movzxb(CpuRegister dst, ByteRegister src) {
        emit8(0x0F);
        emit8(0xB6);
        EmitRegisterOperand(dst.index(), src.index());
    }

    public void movzxb(CpuRegister dst, Address src) {
        emit8(0x0F);
        emit8(0xB6);
        EmitOperand(dst.index(), src);
    }

    public void movsxb(CpuRegister dst, ByteRegister src) {
        emit8(0x0F);
        emit8(0xBE);
        EmitRegisterOperand(dst.index(), src.index());
    }

    public void movsxb(CpuRegister dst, Address src) {
        emit8(0x0F);
        emit8(0xBE);
        EmitOperand(dst.index(), src);
    }

    public void movb(CpuRegister dst, Address src) {
        throw new IllegalStateException("Use movzxb or movsxb instead");
    }

    public void movb(Address dst, ByteRegister src) {
        emit8(0x88);
        EmitOperand(src.index(), dst);
    }

    public void movb(Address dst, Immediate imm) {
        emit8(0xC6);
        EmitOperand(EAX.index(), dst);
        CHECK(imm.isInt8());
        emit8(imm.value() & 0xFF);
    }

    public void movzxw(CpuRegister dst, CpuRegister src) {
        emit8(0x0F);
        emit8(0xB7);
        EmitRegisterOperand(dst.index(), src.index());
    }

    public void movzxw(CpuRegister dst, Address src) {
        emit8(0x0F);
        emit8(0xB7);
        EmitOperand(dst.index(), src);
    }

    public void movsxw(CpuRegister dst, CpuRegister src) {
        emit8(0x0F);
        emit8(0xBF);
        EmitRegisterOperand(dst.index(), src.index());
    }

    public void movsxw(CpuRegister dst, Address src) {
        emit8(0x0F);
        emit8(0xBF);
        EmitOperand(dst.index(), src);
    }

    public void movw(CpuRegister dst, Address src) {
        throw new IllegalStateException("Use movzxw or movsxw instead");
    }

    public void movw(Address dst, CpuRegister src) {
        EmitOperandSizeOverride();
        emit8(0x89);
        EmitOperand(src.index(), dst);
    }

    public void movw(Address dst, Immediate imm) {
        EmitOperandSizeOverride();
        emit8(0xC7);
        EmitOperand(0, dst);
        CHECK(imm.isUInt16() || imm.isInt16());
        emit8(imm.value() & 0xFF);
        emit8(imm.value() >> 8);
    }

    public void leal(CpuRegister dst, Address src) {
        emit8(0x8D);
        EmitOperand(dst.index(), src);
    }

    public void cmovl(Condition condition, CpuRegister dst, CpuRegister src) {
        emit8(0x0F);
        emit8(0x40 + condition.index());
        EmitRegisterOperand(dst.index(), src.index());
    }

    public void cmovl(Condition condition, CpuRegister dst, Address src) {
        emit8(0x0F);
        emit8(0x40 + condition.index());
        EmitOperand(dst.index(), src);
    }

    public void setb(Condition condition, CpuRegister dst) {
        emit8(0x0F);
        emit8(0x90 + condition.index());
        EmitOperand(0, new Operand(dst));
    }

    public void movaps(XmmRegister dst, XmmRegister src) {
        if (cpuHasAVXorAVX2FeatureFlag()) {
            vmovaps(dst, src);
            return;
        }
        emit8(0x0F);
        emit8(0x28);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    /*VEX.128.0F.WIG 28 /r VMOVAPS xmm1, xmm2*/
    public void vmovaps(XmmRegister dst, XmmRegister src) {
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

    public void movaps(XmmRegister dst, Address src) {
        if (cpuHasAVXorAVX2FeatureFlag()) {
            vmovaps(dst, src);
            return;
        }
        emit8(0x0F);
        emit8(0x28);
        EmitOperand(dst.index(), src);
    }

    /*VEX.128.0F.WIG 28 /r VMOVAPS xmm1, m128*/
    public void vmovaps(XmmRegister dst, Address src) {
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

    public void movups(XmmRegister dst, Address src) {
        if (cpuHasAVXorAVX2FeatureFlag()) {
            vmovups(dst, src);
            return;
        }
        emit8(0x0F);
        emit8(0x10);
        EmitOperand(dst.index(), src);
    }

    /*VEX.128.0F.WIG 10 /r VMOVUPS xmm1, m128*/
    public void vmovups(XmmRegister dst, Address src) {
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

    public void movaps(Address dst, XmmRegister src) {
        if (cpuHasAVXorAVX2FeatureFlag()) {
            vmovaps(dst, src);
            return;
        }
        emit8(0x0F);
        emit8(0x29);
        EmitOperand(src.index(), dst);
    }

    /*VEX.128.0F.WIG 29 /r VMOVAPS m128, xmm1*/
    public void vmovaps(Address dst, XmmRegister src) {
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

    public void movups(Address dst, XmmRegister src) {
        if (cpuHasAVXorAVX2FeatureFlag()) {
            vmovups(dst, src);
            return;
        }
        emit8(0x0F);
        emit8(0x11);
        EmitOperand(src.index(), dst);
    }

    /*VEX.128.0F.WIG 11 /r VMOVUPS m128, xmm1*/
    public void vmovups(Address dst, XmmRegister src) {
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

    public void movss(XmmRegister dst, Address src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0x10);
        EmitOperand(dst.index(), src);
    }

    public void movss(Address dst, XmmRegister src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0x11);
        EmitOperand(src.index(), dst);
    }

    public void movss(XmmRegister dst, XmmRegister src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0x11);
        EmitXmmRegisterOperand(src.index(), dst);
    }

    public void movd(XmmRegister dst, CpuRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x6E);
        Operand operand = new Operand(src);
        EmitOperand(dst.index(), operand);
    }

    public void movd(CpuRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x7E);
        Operand operand = new Operand(dst);
        EmitOperand(src.index(), operand);
    }

    public void addss(XmmRegister dst, XmmRegister src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0x58);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void addss(XmmRegister dst, Address src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0x58);
        EmitOperand(dst.index(), src);
    }

    public void subss(XmmRegister dst, XmmRegister src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0x5C);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void subss(XmmRegister dst, Address src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0x5C);
        EmitOperand(dst.index(), src);
    }

    public void mulss(XmmRegister dst, XmmRegister src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0x59);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void mulss(XmmRegister dst, Address src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0x59);
        EmitOperand(dst.index(), src);
    }

    public void divss(XmmRegister dst, XmmRegister src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0x5E);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void divss(XmmRegister dst, Address src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0x5E);
        EmitOperand(dst.index(), src);
    }

    public void addps(XmmRegister dst, XmmRegister src) {
        emit8(0x0F);
        emit8(0x58);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void vaddps(XmmRegister dst, XmmRegister add_left, XmmRegister add_right) {
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

    public void subps(XmmRegister dst, XmmRegister src) {
        emit8(0x0F);
        emit8(0x5C);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void vsubps(XmmRegister dst, XmmRegister src1, XmmRegister src2) {
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

    public void mulps(XmmRegister dst, XmmRegister src) {
        emit8(0x0F);
        emit8(0x59);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void vmulps(XmmRegister dst, XmmRegister src1, XmmRegister src2) {
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

    public void divps(XmmRegister dst, XmmRegister src) {
        emit8(0x0F);
        emit8(0x5E);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void vdivps(XmmRegister dst, XmmRegister src1, XmmRegister src2) {
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

    public void vfmadd213ss(XmmRegister acc, XmmRegister left, XmmRegister right) {
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

    public void vfmadd213sd(XmmRegister acc, XmmRegister left, XmmRegister right) {
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

    public void movapd(XmmRegister dst, XmmRegister src) {
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
    public void vmovapd(XmmRegister dst, XmmRegister src) {
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

    public void movapd(XmmRegister dst, Address src) {
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
    public void vmovapd(XmmRegister dst, Address src) {
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

    public void movupd(XmmRegister dst, Address src) {
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
    public void vmovupd(XmmRegister dst, Address src) {
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

    public void movapd(Address dst, XmmRegister src) {
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
    public void vmovapd(Address dst, XmmRegister src) {
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

    public void movupd(Address dst, XmmRegister src) {
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
    public void vmovupd(Address dst, XmmRegister src) {
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

    public void flds(Address src) {
        emit8(0xD9);
        EmitOperand(0, src);
    }

    public void fsts(Address dst) {
        emit8(0xD9);
        EmitOperand(2, dst);
    }

    public void fstps(Address dst) {
        emit8(0xD9);
        EmitOperand(3, dst);
    }

    public void movsd(XmmRegister dst, Address src) {
        emit8(0xF2);
        emit8(0x0F);
        emit8(0x10);
        EmitOperand(dst.index(), src);
    }

    public void movsd(Address dst, XmmRegister src) {
        emit8(0xF2);
        emit8(0x0F);
        emit8(0x11);
        EmitOperand(src.index(), dst);
    }

    public void movsd(XmmRegister dst, XmmRegister src) {
        emit8(0xF2);
        emit8(0x0F);
        emit8(0x11);
        EmitXmmRegisterOperand(src.index(), dst);
    }

    public void movhpd(XmmRegister dst, Address src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x16);
        EmitOperand(dst.index(), src);
    }

    public void movhpd(Address dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x17);
        EmitOperand(src.index(), dst);
    }

    public void addsd(XmmRegister dst, XmmRegister src) {
        emit8(0xF2);
        emit8(0x0F);
        emit8(0x58);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void addsd(XmmRegister dst, Address src) {
        emit8(0xF2);
        emit8(0x0F);
        emit8(0x58);
        EmitOperand(dst.index(), src);
    }

    public void subsd(XmmRegister dst, XmmRegister src) {
        emit8(0xF2);
        emit8(0x0F);
        emit8(0x5C);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void subsd(XmmRegister dst, Address src) {
        emit8(0xF2);
        emit8(0x0F);
        emit8(0x5C);
        EmitOperand(dst.index(), src);
    }

    public void mulsd(XmmRegister dst, XmmRegister src) {
        emit8(0xF2);
        emit8(0x0F);
        emit8(0x59);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void mulsd(XmmRegister dst, Address src) {
        emit8(0xF2);
        emit8(0x0F);
        emit8(0x59);
        EmitOperand(dst.index(), src);
    }

    public void divsd(XmmRegister dst, XmmRegister src) {
        emit8(0xF2);
        emit8(0x0F);
        emit8(0x5E);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void divsd(XmmRegister dst, Address src) {
        emit8(0xF2);
        emit8(0x0F);
        emit8(0x5E);
        EmitOperand(dst.index(), src);
    }

    public void addpd(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x58);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void vaddpd(XmmRegister dst, XmmRegister add_left, XmmRegister add_right) {
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

    public void subpd(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x5C);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void vsubpd(XmmRegister dst, XmmRegister src1, XmmRegister src2) {
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

    public void mulpd(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x59);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void vmulpd(XmmRegister dst, XmmRegister src1, XmmRegister src2) {
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

    public void divpd(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x5E);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void vdivpd(XmmRegister dst, XmmRegister src1, XmmRegister src2) {
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

    public void movdqa(XmmRegister dst, XmmRegister src) {
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
    public void vmovdqa(XmmRegister dst, XmmRegister src) {
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

    public void movdqa(XmmRegister dst, Address src) {
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
    public void vmovdqa(XmmRegister dst, Address src) {
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

    public void movdqu(XmmRegister dst, Address src) {
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
    public void vmovdqu(XmmRegister dst, Address src) {
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

    public void movdqa(Address dst, XmmRegister src) {
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
    public void vmovdqa(Address dst, XmmRegister src) {
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

    public void movdqu(Address dst, XmmRegister src) {
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
    public void vmovdqu(Address dst, XmmRegister src) {
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

    public void paddb(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xFC);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void vpaddb(XmmRegister dst, XmmRegister add_left, XmmRegister add_right) {
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

    public void psubb(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xF8);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void vpsubb(XmmRegister dst, XmmRegister add_left, XmmRegister add_right) {
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

    public void paddw(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xFD);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void vpaddw(XmmRegister dst, XmmRegister add_left, XmmRegister add_right) {
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

    public void psubw(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xF9);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void vpsubw(XmmRegister dst, XmmRegister add_left, XmmRegister add_right) {
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

    public void pmullw(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xD5);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void paddd(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xFE);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void vpaddd(XmmRegister dst, XmmRegister add_left, XmmRegister add_right) {
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

    public void psubd(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xFA);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void vpsubd(XmmRegister dst, XmmRegister add_left, XmmRegister add_right) {
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

    public void pmulld(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x40);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void vpmulld(XmmRegister dst, XmmRegister src1, XmmRegister src2) {
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

    public void vpmullw(XmmRegister dst, XmmRegister src1, XmmRegister src2) {
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

    public void paddq(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xD4);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void vpaddq(XmmRegister dst, XmmRegister add_left, XmmRegister add_right) {
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

    public void psubq(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xFB);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void vpsubq(XmmRegister dst, XmmRegister add_left, XmmRegister add_right) {
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

    public void paddusb(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xDC);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void paddsb(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xEC);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void paddusw(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xDD);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void paddsw(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xED);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void psubusb(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xD8);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void psubsb(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xE8);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void psubusw(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xD9);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void psubsw(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xE9);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void cvtsi2ss(XmmRegister dst, CpuRegister src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0x2A);
        Operand operand = new Operand(src);
        EmitOperand(dst.index(), operand);
    }

    public void cvtsi2sd(XmmRegister dst, CpuRegister src) {
        emit8(0xF2);
        emit8(0x0F);
        emit8(0x2A);
        Operand operand = new Operand(src);
        EmitOperand(dst.index(), operand);
    }

    public void cvtss2si(CpuRegister dst, XmmRegister src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0x2D);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void cvtss2sd(XmmRegister dst, XmmRegister src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0x5A);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void cvtsd2si(CpuRegister dst, XmmRegister src) {
        emit8(0xF2);
        emit8(0x0F);
        emit8(0x2D);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void cvttss2si(CpuRegister dst, XmmRegister src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0x2C);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void cvttsd2si(CpuRegister dst, XmmRegister src) {
        emit8(0xF2);
        emit8(0x0F);
        emit8(0x2C);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void cvtsd2ss(XmmRegister dst, XmmRegister src) {
        emit8(0xF2);
        emit8(0x0F);
        emit8(0x5A);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void cvtdq2ps(XmmRegister dst, XmmRegister src) {
        emit8(0x0F);
        emit8(0x5B);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void cvtdq2pd(XmmRegister dst, XmmRegister src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0xE6);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void comiss(XmmRegister a, XmmRegister b) {
        emit8(0x0F);
        emit8(0x2F);
        EmitXmmRegisterOperand(a.index(), b);
    }

    public void comiss(XmmRegister a, Address b) {
        emit8(0x0F);
        emit8(0x2F);
        EmitOperand(a.index(), b);
    }

    public void comisd(XmmRegister a, XmmRegister b) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x2F);
        EmitXmmRegisterOperand(a.index(), b);
    }

    public void comisd(XmmRegister a, Address b) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x2F);
        EmitOperand(a.index(), b);
    }

    public void ucomiss(XmmRegister a, XmmRegister b) {
        emit8(0x0F);
        emit8(0x2E);
        EmitXmmRegisterOperand(a.index(), b);
    }

    public void ucomiss(XmmRegister a, Address b) {
        emit8(0x0F);
        emit8(0x2E);
        EmitOperand(a.index(), b);
    }

    public void ucomisd(XmmRegister a, XmmRegister b) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x2E);
        EmitXmmRegisterOperand(a.index(), b);
    }

    public void ucomisd(XmmRegister a, Address b) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x2E);
        EmitOperand(a.index(), b);
    }

    public void roundsd(XmmRegister dst, XmmRegister src, Immediate imm) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x3A);
        emit8(0x0B);
        EmitXmmRegisterOperand(dst.index(), src);
        emit8(imm.value());
    }

    public void roundss(XmmRegister dst, XmmRegister src, Immediate imm) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x3A);
        emit8(0x0A);
        EmitXmmRegisterOperand(dst.index(), src);
        emit8(imm.value());
    }

    public void sqrtsd(XmmRegister dst, XmmRegister src) {
        emit8(0xF2);
        emit8(0x0F);
        emit8(0x51);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void sqrtss(XmmRegister dst, XmmRegister src) {
        emit8(0xF3);
        emit8(0x0F);
        emit8(0x51);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void xorpd(XmmRegister dst, Address src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x57);
        EmitOperand(dst.index(), src);
    }

    public void xorpd(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x57);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void xorps(XmmRegister dst, Address src) {
        emit8(0x0F);
        emit8(0x57);
        EmitOperand(dst.index(), src);
    }

    public void xorps(XmmRegister dst, XmmRegister src) {
        emit8(0x0F);
        emit8(0x57);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pxor(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xEF);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    /* VEX.128.66.0F.WIG EF /r VPXOR xmm1, xmm2, xmm3/m128 */
    public void vpxor(XmmRegister dst, XmmRegister src1, XmmRegister src2) {
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
    public void vxorps(XmmRegister dst, XmmRegister src1, XmmRegister src2) {
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
    public void vxorpd(XmmRegister dst, XmmRegister src1, XmmRegister src2) {
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

    public void andpd(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x54);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void andpd(XmmRegister dst, Address src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x54);
        EmitOperand(dst.index(), src);
    }

    public void andps(XmmRegister dst, XmmRegister src) {
        emit8(0x0F);
        emit8(0x54);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void andps(XmmRegister dst, Address src) {
        emit8(0x0F);
        emit8(0x54);
        EmitOperand(dst.index(), src);
    }

    public void pand(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xDB);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    /* VEX.128.66.0F.WIG DB /r VPAND xmm1, xmm2, xmm3/m128 */
    public void vpand(XmmRegister dst, XmmRegister src1, XmmRegister src2) {
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
    public void vandps(XmmRegister dst, XmmRegister src1, XmmRegister src2) {
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
    public void vandpd(XmmRegister dst, XmmRegister src1, XmmRegister src2) {
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

    public void andnpd(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x55);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void andnps(XmmRegister dst, XmmRegister src) {
        emit8(0x0F);
        emit8(0x55);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pandn(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xDF);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    /* VEX.128.66.0F.WIG DF /r VPANDN xmm1, xmm2, xmm3/m128 */
    public void vpandn(XmmRegister dst, XmmRegister src1, XmmRegister src2) {
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
    public void vandnps(XmmRegister dst, XmmRegister src1, XmmRegister src2) {
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
    public void vandnpd(XmmRegister dst, XmmRegister src1, XmmRegister src2) {
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

    public void orpd(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x56);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void orps(XmmRegister dst, XmmRegister src) {
        emit8(0x0F);
        emit8(0x56);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void andn(CpuRegister dst, CpuRegister src1, CpuRegister src2) {
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

    public void por(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xEB);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    /* VEX.128.66.0F.WIG EB /r VPOR xmm1, xmm2, xmm3/m128 */
    public void vpor(XmmRegister dst, XmmRegister src1, XmmRegister src2) {
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
    public void vorps(XmmRegister dst, XmmRegister src1, XmmRegister src2) {
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
    public void vorpd(XmmRegister dst, XmmRegister src1, XmmRegister src2) {
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

    public void pavgb(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xE0);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pavgw(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xE3);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void psadbw(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xF6);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pmaddwd(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xF5);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void vpmaddwd(XmmRegister dst, XmmRegister src1, XmmRegister src2) {
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

    public void phaddw(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x01);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void phaddd(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x02);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void haddps(XmmRegister dst, XmmRegister src) {
        emit8(0xF2);
        emit8(0x0F);
        emit8(0x7C);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void haddpd(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x7C);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void phsubw(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x05);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void phsubd(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x06);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void hsubps(XmmRegister dst, XmmRegister src) {
        emit8(0xF2);
        emit8(0x0F);
        emit8(0x7D);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void hsubpd(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x7D);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pminsb(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x38);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pmaxsb(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x3C);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pminsw(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xEA);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pmaxsw(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xEE);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pminsd(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x39);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pmaxsd(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x3D);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pminub(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xDA);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pmaxub(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xDE);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pminuw(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x3A);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pmaxuw(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x3E);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pminud(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x3B);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pmaxud(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x3F);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void minps(XmmRegister dst, XmmRegister src) {
        emit8(0x0F);
        emit8(0x5D);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void maxps(XmmRegister dst, XmmRegister src) {
        emit8(0x0F);
        emit8(0x5F);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void minpd(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x5D);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void maxpd(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x5F);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pcmpeqb(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x74);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pcmpeqw(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x75);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pcmpeqd(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x76);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pcmpeqq(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x29);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pcmpgtb(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x64);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pcmpgtw(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x65);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pcmpgtd(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x66);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void pcmpgtq(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x38);
        emit8(0x37);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void shufpd(XmmRegister dst, XmmRegister src, Immediate imm) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0xC6);
        EmitXmmRegisterOperand(dst.index(), src);
        emit8(imm.value());
    }

    public void shufps(XmmRegister dst, XmmRegister src, Immediate imm) {
        emit8(0x0F);
        emit8(0xC6);
        EmitXmmRegisterOperand(dst.index(), src);
        emit8(imm.value());
    }

    public void pshufd(XmmRegister dst, XmmRegister src, Immediate imm) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x70);
        EmitXmmRegisterOperand(dst.index(), src);
        emit8(imm.value());
    }

    public void punpcklbw(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x60);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void punpcklwd(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x61);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void punpckldq(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x62);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void punpcklqdq(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x6C);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void punpckhbw(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x68);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void punpckhwd(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x69);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void punpckhdq(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x6A);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void punpckhqdq(XmmRegister dst, XmmRegister src) {
        emit8(0x66);
        emit8(0x0F);
        emit8(0x6D);
        EmitXmmRegisterOperand(dst.index(), src);
    }

    public void psllw(XmmRegister reg, Immediate shift_count) {
        CHECK(shift_count.isUInt8());
        emit8(0x66);
        emit8(0x0F);
        emit8(0x71);
        EmitXmmRegisterOperand(6, reg);
        emit8(shift_count.value());
    }

    public void pslld(XmmRegister reg, Immediate shift_count) {
        CHECK(shift_count.isUInt8());
        emit8(0x66);
        emit8(0x0F);
        emit8(0x72);
        EmitXmmRegisterOperand(6, reg);
        emit8(shift_count.value());
    }

    public void psllq(XmmRegister reg, Immediate shift_count) {
        CHECK(shift_count.isUInt8());
        emit8(0x66);
        emit8(0x0F);
        emit8(0x73);
        EmitXmmRegisterOperand(6, reg);
        emit8(shift_count.value());
    }

    public void psraw(XmmRegister reg, Immediate shift_count) {
        CHECK(shift_count.isUInt8());
        emit8(0x66);
        emit8(0x0F);
        emit8(0x71);
        EmitXmmRegisterOperand(4, reg);
        emit8(shift_count.value());
    }

    public void psrad(XmmRegister reg, Immediate shift_count) {
        CHECK(shift_count.isUInt8());
        emit8(0x66);
        emit8(0x0F);
        emit8(0x72);
        EmitXmmRegisterOperand(4, reg);
        emit8(shift_count.value());
    }

    public void psrlw(XmmRegister reg, Immediate shift_count) {
        CHECK(shift_count.isUInt8());
        emit8(0x66);
        emit8(0x0F);
        emit8(0x71);
        EmitXmmRegisterOperand(2, reg);
        emit8(shift_count.value());
    }

    public void psrld(XmmRegister reg, Immediate shift_count) {
        CHECK(shift_count.isUInt8());
        emit8(0x66);
        emit8(0x0F);
        emit8(0x72);
        EmitXmmRegisterOperand(2, reg);
        emit8(shift_count.value());
    }

    public void psrlq(XmmRegister reg, Immediate shift_count) {
        CHECK(shift_count.isUInt8());
        emit8(0x66);
        emit8(0x0F);
        emit8(0x73);
        EmitXmmRegisterOperand(2, reg);
        emit8(shift_count.value());
    }

    public void psrldq(XmmRegister reg, Immediate shift_count) {
        CHECK(shift_count.isUInt8());
        emit8(0x66);
        emit8(0x0F);
        emit8(0x73);
        EmitXmmRegisterOperand(3, reg);
        emit8(shift_count.value());
    }

    public void fldl(Address src) {
        emit8(0xDD);
        EmitOperand(0, src);
    }

    public void fstl(Address dst) {
        emit8(0xDD);
        EmitOperand(2, dst);
    }

    public void fstpl(Address dst) {
        emit8(0xDD);
        EmitOperand(3, dst);
    }

    public void fstsw() {
        emit8(0x9B);
        emit8(0xDF);
        emit8(0xE0);
    }

    public void fnstcw(Address dst) {
        emit8(0xD9);
        EmitOperand(7, dst);
    }

    public void fldcw(Address src) {
        emit8(0xD9);
        EmitOperand(5, src);
    }

    public void fistpl(Address dst) {
        emit8(0xDF);
        EmitOperand(7, dst);
    }

    public void fistps(Address dst) {
        emit8(0xDB);
        EmitOperand(3, dst);
    }

    public void fildl(Address src) {
        emit8(0xDF);
        EmitOperand(5, src);
    }

    public void filds(Address src) {
        emit8(0xDB);
        EmitOperand(0, src);
    }

    public void fincstp() {
        emit8(0xD9);
        emit8(0xF7);
    }

    public void ffree(Immediate index) {
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

    private boolean try_xchg_eax(CpuRegister dst, CpuRegister src) {
        if (src != EAX && dst != EAX) {
            return false;
        }
        if (dst == EAX) {
            dst = src;
        }
        emit8(0x90 + dst.index());
        return true;
    }

    public void xchgb(ByteRegister dst, ByteRegister src) {
        emit8(0x86);
        EmitRegisterOperand(dst.index(), src.index());
    }

    public void xchgb(ByteRegister reg, Address address) {
        emit8(0x86);
        EmitOperand(reg.index(), address);
    }

    public void xchgw(CpuRegister dst, CpuRegister src) {
        EmitOperandSizeOverride();
        if (try_xchg_eax(dst, src)) {
            // A short version for AX.
            return;
        }
        // General case.
        emit8(0x87);
        EmitRegisterOperand(dst.index(), src.index());
    }

    public void xchgw(CpuRegister reg, Address address) {
        EmitOperandSizeOverride();
        emit8(0x87);
        EmitOperand(reg.index(), address);
    }

    public void xchgl(CpuRegister dst, CpuRegister src) {
        if (try_xchg_eax(dst, src)) {
            // A short version for EAX.
            return;
        }
        // General case.
        emit8(0x87);
        EmitRegisterOperand(dst.index(), src.index());
    }

    public void xchgl(CpuRegister reg, Address address) {
        emit8(0x87);
        EmitOperand(reg.index(), address);
    }

    public void cmpb(Address address, Immediate imm) {
        emit8(0x80);
        EmitOperand(7, address);
        emit8(imm.value() & 0xFF);
    }

    public void cmpw(Address address, Immediate imm) {
        emit8(0x66);
        EmitComplex(7, address, imm, /* is_16_op= */ true);
    }

    public void cmpl(CpuRegister reg, Immediate imm) {
        EmitComplex(7, new Operand(reg), imm);
    }

    public void cmpl(CpuRegister reg0, CpuRegister reg1) {
        emit8(0x3B);
        Operand operand = new Operand(reg1);
        EmitOperand(reg0.index(), operand);
    }

    public void cmpl(CpuRegister reg, Address address) {
        emit8(0x3B);
        EmitOperand(reg.index(), address);
    }

    public void addl(CpuRegister dst, CpuRegister src) {
        emit8(0x03);
        EmitRegisterOperand(dst.index(), src.index());
    }

    public void addl(CpuRegister reg, Address address) {
        emit8(0x03);
        EmitOperand(reg.index(), address);
    }

    public void cmpl(Address address, CpuRegister reg) {
        emit8(0x39);
        EmitOperand(reg.index(), address);
    }

    public void cmpl(Address address, Immediate imm) {
        EmitComplex(7, address, imm);
    }

    public void testl(CpuRegister reg1, CpuRegister reg2) {
        emit8(0x85);
        EmitRegisterOperand(reg1.index(), reg2.index());
    }

    public void testl(CpuRegister reg, Address address) {
        emit8(0x85);
        EmitOperand(reg.index(), address);
    }

    public void testl(CpuRegister reg, Immediate immediate) {
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
            EmitOperand(0, new Operand(reg));
            EmitImmediate(immediate);
        }
    }

    public void testb(Address dst, Immediate imm) {
        emit8(0xF6);
        EmitOperand(EAX.index(), dst);
        CHECK(imm.isInt8());
        emit8(imm.value() & 0xFF);
    }

    public void testl(Address dst, Immediate imm) {
        emit8(0xF7);
        EmitOperand(0, dst);
        EmitImmediate(imm);
    }

    public void andl(CpuRegister dst, CpuRegister src) {
        emit8(0x23);
        Operand operand = new Operand(src);
        EmitOperand(dst.index(), operand);
    }

    public void andl(CpuRegister reg, Address address) {
        emit8(0x23);
        EmitOperand(reg.index(), address);
    }

    public void andl(CpuRegister dst, Immediate imm) {
        EmitComplex(4, new Operand(dst), imm);
    }

    public void andw(Address address, Immediate imm) {
        CHECK(imm.isUInt16() || imm.isInt16());
        EmitOperandSizeOverride();
        EmitComplex(4, address, imm, /* is_16_op= */ true);
    }

    public void orl(CpuRegister dst, CpuRegister src) {
        emit8(0x0B);
        Operand operand = new Operand(src);
        EmitOperand(dst.index(), operand);
    }

    public void orl(CpuRegister reg, Address address) {
        emit8(0x0B);
        EmitOperand(reg.index(), address);
    }

    public void orl(CpuRegister dst, Immediate imm) {
        EmitComplex(1, new Operand(dst), imm);
    }

    public void xorl(CpuRegister dst, CpuRegister src) {
        emit8(0x33);
        Operand operand = new Operand(src);
        EmitOperand(dst.index(), operand);
    }

    public void xorl(CpuRegister reg, Address address) {
        emit8(0x33);
        EmitOperand(reg.index(), address);
    }

    public void xorl(CpuRegister dst, Immediate imm) {
        EmitComplex(6, new Operand(dst), imm);
    }

    public void addl(CpuRegister reg, Immediate imm) {
        EmitComplex(0, new Operand(reg), imm);
    }

    public void addl(Address address, CpuRegister reg) {
        emit8(0x01);
        EmitOperand(reg.index(), address);
    }

    public void addl(Address address, Immediate imm) {
        EmitComplex(0, address, imm);
    }

    public void addw(Address address, Immediate imm) {
        CHECK(imm.isUInt16() || imm.isInt16());
        emit8(0x66);
        EmitComplex(0, address, imm, /* is_16_op= */ true);
    }

    public void adcl(CpuRegister reg, Immediate imm) {
        EmitComplex(2, new Operand(reg), imm);
    }

    public void adcl(CpuRegister dst, CpuRegister src) {
        emit8(0x13);
        Operand operand = new Operand(src);
        EmitOperand(dst.index(), operand);
    }

    public void adcl(CpuRegister dst, Address address) {
        emit8(0x13);
        EmitOperand(dst.index(), address);
    }

    public void subl(CpuRegister dst, CpuRegister src) {
        emit8(0x2B);
        Operand operand = new Operand(src);
        EmitOperand(dst.index(), operand);
    }

    public void subl(CpuRegister reg, Immediate imm) {
        EmitComplex(5, new Operand(reg), imm);
    }

    public void subl(CpuRegister reg, Address address) {
        emit8(0x2B);
        EmitOperand(reg.index(), address);
    }

    public void subl(Address address, CpuRegister reg) {
        emit8(0x29);
        EmitOperand(reg.index(), address);
    }

    public void cdq() {
        emit8(0x99);
    }

    public void idivl(CpuRegister reg) {
        emit8(0xF7);
        emit8(0xF8 | reg.index());
    }

    public void divl(CpuRegister reg) {
        emit8(0xF7);
        emit8(0xF0 | reg.index());
    }

    public void imull(CpuRegister dst, CpuRegister src) {
        emit8(0x0F);
        emit8(0xAF);
        Operand operand = new Operand(src);
        EmitOperand(dst.index(), operand);
    }

    public void imull(CpuRegister dst, CpuRegister src, Immediate imm) {
        // See whether imm can be represented as a sign-extended 8bit value.
        if (imm.isInt8()) {
            // Sign-extension works.
            emit8(0x6B);
            Operand operand = new Operand(src);
            EmitOperand(dst.index(), operand);
            emit8(imm.value() & 0xFF);
        } else {
            // Not representable, use full immediate.
            emit8(0x69);
            Operand operand = new Operand(src);
            EmitOperand(dst.index(), operand);
            EmitImmediate(imm);
        }
    }

    public void imull(CpuRegister reg, Immediate imm) {
        imull(reg, reg, imm);
    }

    public void imull(CpuRegister reg, Address address) {
        emit8(0x0F);
        emit8(0xAF);
        EmitOperand(reg.index(), address);
    }

    public void imull(CpuRegister reg) {
        emit8(0xF7);
        EmitOperand(5, new Operand(reg));
    }

    public void imull(Address address) {
        emit8(0xF7);
        EmitOperand(5, address);
    }

    public void mull(CpuRegister reg) {
        emit8(0xF7);
        EmitOperand(4, new Operand(reg));
    }

    public void mull(Address address) {
        emit8(0xF7);
        EmitOperand(4, address);
    }

    public void sbbl(CpuRegister dst, CpuRegister src) {
        emit8(0x1B);
        Operand operand = new Operand(src);
        EmitOperand(dst.index(), operand);
    }

    public void sbbl(CpuRegister reg, Immediate imm) {
        EmitComplex(3, new Operand(reg), imm);
    }

    public void sbbl(CpuRegister dst, Address address) {
        emit8(0x1B);
        EmitOperand(dst.index(), address);
    }

    public void sbbl(Address address, CpuRegister src) {
        emit8(0x19);
        EmitOperand(src.index(), address);
    }

    public void incl(CpuRegister reg) {
        emit8(0x40 + reg.index());
    }

    public void incl(Address address) {
        emit8(0xFF);
        EmitOperand(0, address);
    }

    public void decl(CpuRegister reg) {
        emit8(0x48 + reg.index());
    }

    public void decl(Address address) {
        emit8(0xFF);
        EmitOperand(1, address);
    }

    public void shll(CpuRegister reg, Immediate imm) {
        EmitGenericShift(4, new Operand(reg), imm);
    }

    public void shll(CpuRegister operand, CpuRegister shifter) {
        EmitGenericShift(4, new Operand(operand), shifter);
    }

    public void shll(Address address, Immediate imm) {
        EmitGenericShift(4, address, imm);
    }

    public void shll(Address address, CpuRegister shifter) {
        EmitGenericShift(4, address, shifter);
    }

    public void shrl(CpuRegister reg, Immediate imm) {
        EmitGenericShift(5, new Operand(reg), imm);
    }

    public void shrl(CpuRegister operand, CpuRegister shifter) {
        EmitGenericShift(5, new Operand(operand), shifter);
    }

    public void shrl(Address address, Immediate imm) {
        EmitGenericShift(5, address, imm);
    }

    public void shrl(Address address, CpuRegister shifter) {
        EmitGenericShift(5, address, shifter);
    }

    public void sarl(CpuRegister reg, Immediate imm) {
        EmitGenericShift(7, new Operand(reg), imm);
    }

    public void sarl(CpuRegister operand, CpuRegister shifter) {
        EmitGenericShift(7, new Operand(operand), shifter);
    }

    public void sarl(Address address, Immediate imm) {
        EmitGenericShift(7, address, imm);
    }

    public void sarl(Address address, CpuRegister shifter) {
        EmitGenericShift(7, address, shifter);
    }

    public void shld(CpuRegister dst, CpuRegister src, CpuRegister shifter) {
        CHECK_EQ(ECX.index(), shifter.index());
        emit8(0x0F);
        emit8(0xA5);
        EmitRegisterOperand(src.index(), dst.index());
    }

    public void shld(CpuRegister dst, CpuRegister src, Immediate imm) {
        emit8(0x0F);
        emit8(0xA4);
        EmitRegisterOperand(src.index(), dst.index());
        emit8(imm.value() & 0xFF);
    }

    public void shrd(CpuRegister dst, CpuRegister src, CpuRegister shifter) {
        CHECK_EQ(ECX.index(), shifter.index());
        emit8(0x0F);
        emit8(0xAD);
        EmitRegisterOperand(src.index(), dst.index());
    }

    public void shrd(CpuRegister dst, CpuRegister src, Immediate imm) {
        emit8(0x0F);
        emit8(0xAC);
        EmitRegisterOperand(src.index(), dst.index());
        emit8(imm.value() & 0xFF);
    }

    public void roll(CpuRegister reg, Immediate imm) {
        EmitGenericShift(0, new Operand(reg), imm);
    }

    public void roll(CpuRegister operand, CpuRegister shifter) {
        EmitGenericShift(0, new Operand(operand), shifter);
    }

    public void rorl(CpuRegister reg, Immediate imm) {
        EmitGenericShift(1, new Operand(reg), imm);
    }

    public void rorl(CpuRegister operand, CpuRegister shifter) {
        EmitGenericShift(1, new Operand(operand), shifter);
    }

    public void negl(CpuRegister reg) {
        emit8(0xF7);
        EmitOperand(3, new Operand(reg));
    }

    public void notl(CpuRegister reg) {
        emit8(0xF7);
        emit8(0xD0 | reg.index());
    }

    public void enter(Immediate imm) {
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

    public void ret(Immediate imm) {
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

    public void j(Condition condition, Label label) {
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

    public void j(Condition condition, NearLabel label) {
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

    public void jecxz(NearLabel label) {
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

    public void jmp(CpuRegister reg) {
        emit8(0xFF);
        EmitRegisterOperand(4, reg.index());
    }

    public void jmp(Address address) {
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

    public void jmp(NearLabel label) {
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

    public void cmpxchgb(Address address, ByteRegister reg) {
        emit8(0x0F);
        emit8(0xB0);
        EmitOperand(reg.index(), address);
    }

    public void cmpxchgw(Address address, CpuRegister reg) {
        EmitOperandSizeOverride();
        emit8(0x0F);
        emit8(0xB1);
        EmitOperand(reg.index(), address);
    }

    public void cmpxchgl(Address address, CpuRegister reg) {
        emit8(0x0F);
        emit8(0xB1);
        EmitOperand(reg.index(), address);
    }

    public void cmpxchg8b(Address address) {
        emit8(0x0F);
        emit8(0xC7);
        EmitOperand(1, address);
    }

    public void xaddb(Address address, ByteRegister reg) {
        emit8(0x0F);
        emit8(0xC0);
        EmitOperand(reg.index(), address);
    }

    public void xaddw(Address address, CpuRegister reg) {
        EmitOperandSizeOverride();
        emit8(0x0F);
        emit8(0xC1);
        EmitOperand(reg.index(), address);
    }

    public void xaddl(Address address, CpuRegister reg) {
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

    public void bind(NearLabel label) {
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