package com.v7878.jnasm.riscv64;

public interface RV64AssemblerI {
    // According to "The RISC-V Instruction Set Manual"

    // LUI/AUIPC (RV32I, with sign-extension on RV64I), opcode = 0x17, 0x37
    // Note: These take a 20-bit unsigned value to align with the clang assembler for testing,
    // but the value stored in the register shall actually be sign-extended to 64 bits.
    void Lui(RV64XRegister rd, /* unsigned */ int uimm20);

    void Auipc(RV64XRegister rd, /* unsigned */ int uimm20);

    // Jump instructions (RV32I), opcode = 0x67, 0x6f
    void Jal(RV64XRegister rd, int offset);

    void Jalr(RV64XRegister rd, RV64XRegister rs1, int offset);

    // Branch instructions (RV32I), opcode = 0x63, funct3 from 0x0 ~ 0x1 and 0x4 ~ 0x7
    void Beq(RV64XRegister rs1, RV64XRegister rs2, int offset);

    void Bne(RV64XRegister rs1, RV64XRegister rs2, int offset);

    void Blt(RV64XRegister rs1, RV64XRegister rs2, int offset);

    void Bge(RV64XRegister rs1, RV64XRegister rs2, int offset);

    void Bltu(RV64XRegister rs1, RV64XRegister rs2, int offset);

    void Bgeu(RV64XRegister rs1, RV64XRegister rs2, int offset);

    // Load instructions (RV32I+RV64I): opcode = 0x03, funct3 from 0x0 ~ 0x6
    void Lb(RV64XRegister rd, RV64XRegister rs1, int offset);

    void Lh(RV64XRegister rd, RV64XRegister rs1, int offset);

    void Lw(RV64XRegister rd, RV64XRegister rs1, int offset);

    void Ld(RV64XRegister rd, RV64XRegister rs1, int offset);

    void Lbu(RV64XRegister rd, RV64XRegister rs1, int offset);

    void Lhu(RV64XRegister rd, RV64XRegister rs1, int offset);

    void Lwu(RV64XRegister rd, RV64XRegister rs1, int offset);

    // Store instructions (RV32I+RV64I): opcode = 0x23, funct3 from 0x0 ~ 0x3
    void Sb(RV64XRegister rs2, RV64XRegister rs1, int offset);

    void Sh(RV64XRegister rs2, RV64XRegister rs1, int offset);

    void Sw(RV64XRegister rs2, RV64XRegister rs1, int offset);

    void Sd(RV64XRegister rs2, RV64XRegister rs1, int offset);

    // IMM ALU instructions (RV32I): opcode = 0x13, funct3 from 0x0 ~ 0x7
    void Addi(RV64XRegister rd, RV64XRegister rs1, int imm12);

    void Slti(RV64XRegister rd, RV64XRegister rs1, int imm12);

    void Sltiu(RV64XRegister rd, RV64XRegister rs1, int imm12);

    void Xori(RV64XRegister rd, RV64XRegister rs1, int imm12);

    void Ori(RV64XRegister rd, RV64XRegister rs1, int imm12);

    void Andi(RV64XRegister rd, RV64XRegister rs1, int imm12);

    void Slli(RV64XRegister rd, RV64XRegister rs1, int shamt);

    void Srli(RV64XRegister rd, RV64XRegister rs1, int shamt);

    void Srai(RV64XRegister rd, RV64XRegister rs1, int shamt);

    // ALU instructions (RV32I): opcode = 0x33, funct3 from 0x0 ~ 0x7
    void Add(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Sub(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Slt(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Sltu(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Xor(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Or(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void And(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Sll(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Srl(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Sra(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    // 32bit Imm ALU instructions (RV64I): opcode = 0x1b, funct3 from 0x0, 0x1, 0x5
    void Addiw(RV64XRegister rd, RV64XRegister rs1, int imm12);

    void Slliw(RV64XRegister rd, RV64XRegister rs1, int shamt);

    void Srliw(RV64XRegister rd, RV64XRegister rs1, int shamt);

    void Sraiw(RV64XRegister rd, RV64XRegister rs1, int shamt);

    // 32bit ALU instructions (RV64I): opcode = 0x3b, funct3 from 0x0 ~ 0x7
    void Addw(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Subw(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Sllw(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Srlw(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Sraw(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    // Environment call and breakpoint (RV32I), opcode = 0x73
    void Ecall();

    void Ebreak();

    // Fence instruction (RV32I): opcode = 0xf, funct3 = 0
    void Fence(RV64FenceType pred, RV64FenceType succ);

    void FenceTso();

    // "Zifencei" Standard Extension, opcode = 0xf, funct3 = 1
    void FenceI();

    // RV32M Standard Extension: opcode = 0x33, funct3 from 0x0 ~ 0x7
    void Mul(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Mulh(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Mulhsu(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Mulhu(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Div(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Divu(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Rem(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Remu(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    // RV64M Standard Extension: opcode = 0x3b, funct3 0x0 and from 0x4 ~ 0x7
    void Mulw(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Divw(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Divuw(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Remw(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Remuw(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    // RV32A/RV64A Standard Extension
    void LrW(RV64XRegister rd, RV64XRegister rs1, RV64AqRl aqrl);

    void LrD(RV64XRegister rd, RV64XRegister rs1, RV64AqRl aqrl);

    void ScW(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl);

    void ScD(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl);

    void AmoSwapW(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl);

    void AmoSwapD(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl);

    void AmoAddW(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl);

    void AmoAddD(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl);

    void AmoXorW(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl);

    void AmoXorD(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl);

    void AmoAndW(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl);

    void AmoAndD(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl);

    void AmoOrW(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl);

    void AmoOrD(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl);

    void AmoMinW(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl);

    void AmoMinD(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl);

    void AmoMaxW(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl);

    void AmoMaxD(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl);

    void AmoMinuW(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl);

    void AmoMinuD(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl);

    void AmoMaxuW(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl);

    void AmoMaxuD(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl);

    // "Zicsr" Standard Extension, opcode = 0x73, funct3 from 0x1 ~ 0x3 and 0x5 ~ 0x7
    void Csrrw(RV64XRegister rd, int /* 12-bit */ csr, RV64XRegister rs1);

    void Csrrs(RV64XRegister rd, int /* 12-bit */ csr, RV64XRegister rs1);

    void Csrrc(RV64XRegister rd, int /* 12-bit */ csr, RV64XRegister rs1);

    void Csrrwi(RV64XRegister rd, int /* 12-bit */ csr, /* unsigned */ int uimm5);

    void Csrrsi(RV64XRegister rd, int /* 12-bit */ csr, /* unsigned */ int uimm5);

    void Csrrci(RV64XRegister rd, int /* 12-bit */ csr, /* unsigned */ int uimm5);

    // FP load/store instructions (RV32F+RV32D): opcode = 0x07, 0x27
    void FLw(RV64FRegister rd, RV64XRegister rs1, int offset);

    void FLd(RV64FRegister rd, RV64XRegister rs1, int offset);

    void FSw(RV64FRegister rs2, RV64XRegister rs1, int offset);

    void FSd(RV64FRegister rs2, RV64XRegister rs1, int offset);

    // FP FMA instructions (RV32F+RV32D): opcode = 0x43, 0x47, 0x4b, 0x4f
    void FMAddS(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FRegister rs3, RV64FPRoundingMode frm);

    void FMAddD(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FRegister rs3, RV64FPRoundingMode frm);

    void FMSubS(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FRegister rs3, RV64FPRoundingMode frm);

    void FMSubD(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FRegister rs3, RV64FPRoundingMode frm);

    void FNMSubS(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FRegister rs3, RV64FPRoundingMode frm);

    void FNMSubD(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FRegister rs3, RV64FPRoundingMode frm);

    void FNMAddS(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FRegister rs3, RV64FPRoundingMode frm);

    void FNMAddD(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FRegister rs3, RV64FPRoundingMode frm);

    // FP FMA instruction helpers passing the default rounding mode.
    default void FMAddS(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FRegister rs3) {
        FMAddS(rd, rs1, rs2, rs3, RV64FPRoundingMode.kDefault);
    }

    default void FMAddD(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FRegister rs3) {
        FMAddD(rd, rs1, rs2, rs3, RV64FPRoundingMode.kDefault);
    }

    default void FMSubS(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FRegister rs3) {
        FMSubS(rd, rs1, rs2, rs3, RV64FPRoundingMode.kDefault);
    }

    default void FMSubD(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FRegister rs3) {
        FMSubD(rd, rs1, rs2, rs3, RV64FPRoundingMode.kDefault);
    }

    default void FNMSubS(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FRegister rs3) {
        FNMSubS(rd, rs1, rs2, rs3, RV64FPRoundingMode.kDefault);
    }

    default void FNMSubD(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FRegister rs3) {
        FNMSubD(rd, rs1, rs2, rs3, RV64FPRoundingMode.kDefault);
    }

    default void FNMAddS(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FRegister rs3) {
        FNMAddS(rd, rs1, rs2, rs3, RV64FPRoundingMode.kDefault);
    }

    default void FNMAddD(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FRegister rs3) {
        FNMAddD(rd, rs1, rs2, rs3, RV64FPRoundingMode.kDefault);
    }

    // Simple FP instructions (RV32F+RV32D): opcode = 0x53, funct7 = 0b0XXXX0D
    void FAddS(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FPRoundingMode frm);

    void FAddD(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FPRoundingMode frm);

    void FSubS(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FPRoundingMode frm);

    void FSubD(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FPRoundingMode frm);

    void FMulS(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FPRoundingMode frm);

    void FMulD(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FPRoundingMode frm);

    void FDivS(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FPRoundingMode frm);

    void FDivD(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FPRoundingMode frm);

    void FSqrtS(RV64FRegister rd, RV64FRegister rs1, RV64FPRoundingMode frm);

    void FSqrtD(RV64FRegister rd, RV64FRegister rs1, RV64FPRoundingMode frm);

    void FSgnjS(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2);

    void FSgnjD(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2);

    void FSgnjnS(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2);

    void FSgnjnD(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2);

    void FSgnjxS(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2);

    void FSgnjxD(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2);

    void FMinS(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2);

    void FMinD(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2);

    void FMaxS(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2);

    void FMaxD(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2);

    void FCvtSD(RV64FRegister rd, RV64FRegister rs1, RV64FPRoundingMode frm);

    void FCvtDS(RV64FRegister rd, RV64FRegister rs1, RV64FPRoundingMode frm);

    // Simple FP instruction helpers passing the default rounding mode.
    default void FAddS(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2) {
        FAddS(rd, rs1, rs2, RV64FPRoundingMode.kDefault);
    }

    default void FAddD(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2) {
        FAddD(rd, rs1, rs2, RV64FPRoundingMode.kDefault);
    }

    default void FSubS(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2) {
        FSubS(rd, rs1, rs2, RV64FPRoundingMode.kDefault);
    }

    default void FSubD(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2) {
        FSubD(rd, rs1, rs2, RV64FPRoundingMode.kDefault);
    }

    default void FMulS(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2) {
        FMulS(rd, rs1, rs2, RV64FPRoundingMode.kDefault);
    }

    default void FMulD(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2) {
        FMulD(rd, rs1, rs2, RV64FPRoundingMode.kDefault);
    }

    default void FDivS(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2) {
        FDivS(rd, rs1, rs2, RV64FPRoundingMode.kDefault);
    }

    default void FDivD(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2) {
        FDivD(rd, rs1, rs2, RV64FPRoundingMode.kDefault);
    }

    default void FSqrtS(RV64FRegister rd, RV64FRegister rs1) {
        FSqrtS(rd, rs1, RV64FPRoundingMode.kDefault);
    }

    default void FSqrtD(RV64FRegister rd, RV64FRegister rs1) {
        FSqrtD(rd, rs1, RV64FPRoundingMode.kDefault);
    }

    default void FCvtSD(RV64FRegister rd, RV64FRegister rs1) {
        FCvtSD(rd, rs1, RV64FPRoundingMode.kDefault);
    }

    default void FCvtDS(RV64FRegister rd, RV64FRegister rs1) {
        FCvtDS(rd, rs1, RV64FPRoundingMode.kIgnored);
    }

    // FP compare instructions (RV32F+RV32D): opcode = 0x53, funct7 = 0b101000D
    void FEqS(RV64XRegister rd, RV64FRegister rs1, RV64FRegister rs2);

    void FEqD(RV64XRegister rd, RV64FRegister rs1, RV64FRegister rs2);

    void FLtS(RV64XRegister rd, RV64FRegister rs1, RV64FRegister rs2);

    void FLtD(RV64XRegister rd, RV64FRegister rs1, RV64FRegister rs2);

    void FLeS(RV64XRegister rd, RV64FRegister rs1, RV64FRegister rs2);

    void FLeD(RV64XRegister rd, RV64FRegister rs1, RV64FRegister rs2);

    // FP conversion instructions (RV32F+RV32D+RV64F+RV64D): opcode = 0x53, funct7 = 0b110X00D
    void FCvtWS(RV64XRegister rd, RV64FRegister rs1, RV64FPRoundingMode frm);

    void FCvtWD(RV64XRegister rd, RV64FRegister rs1, RV64FPRoundingMode frm);

    void FCvtWuS(RV64XRegister rd, RV64FRegister rs1, RV64FPRoundingMode frm);

    void FCvtWuD(RV64XRegister rd, RV64FRegister rs1, RV64FPRoundingMode frm);

    void FCvtLS(RV64XRegister rd, RV64FRegister rs1, RV64FPRoundingMode frm);

    void FCvtLD(RV64XRegister rd, RV64FRegister rs1, RV64FPRoundingMode frm);

    void FCvtLuS(RV64XRegister rd, RV64FRegister rs1, RV64FPRoundingMode frm);

    void FCvtLuD(RV64XRegister rd, RV64FRegister rs1, RV64FPRoundingMode frm);

    void FCvtSW(RV64FRegister rd, RV64XRegister rs1, RV64FPRoundingMode frm);

    void FCvtDW(RV64FRegister rd, RV64XRegister rs1, RV64FPRoundingMode frm);

    void FCvtSWu(RV64FRegister rd, RV64XRegister rs1, RV64FPRoundingMode frm);

    void FCvtDWu(RV64FRegister rd, RV64XRegister rs1, RV64FPRoundingMode frm);

    void FCvtSL(RV64FRegister rd, RV64XRegister rs1, RV64FPRoundingMode frm);

    void FCvtDL(RV64FRegister rd, RV64XRegister rs1, RV64FPRoundingMode frm);

    void FCvtSLu(RV64FRegister rd, RV64XRegister rs1, RV64FPRoundingMode frm);

    void FCvtDLu(RV64FRegister rd, RV64XRegister rs1, RV64FPRoundingMode frm);

    // FP conversion instruction helpers passing the default rounding mode.
    default void FCvtWS(RV64XRegister rd, RV64FRegister rs1) {
        FCvtWS(rd, rs1, RV64FPRoundingMode.kDefault);
    }

    default void FCvtWD(RV64XRegister rd, RV64FRegister rs1) {
        FCvtWD(rd, rs1, RV64FPRoundingMode.kDefault);
    }

    default void FCvtWuS(RV64XRegister rd, RV64FRegister rs1) {
        FCvtWuS(rd, rs1, RV64FPRoundingMode.kDefault);
    }

    default void FCvtWuD(RV64XRegister rd, RV64FRegister rs1) {
        FCvtWuD(rd, rs1, RV64FPRoundingMode.kDefault);
    }

    default void FCvtLS(RV64XRegister rd, RV64FRegister rs1) {
        FCvtLS(rd, rs1, RV64FPRoundingMode.kDefault);
    }

    default void FCvtLD(RV64XRegister rd, RV64FRegister rs1) {
        FCvtLD(rd, rs1, RV64FPRoundingMode.kDefault);
    }

    default void FCvtLuS(RV64XRegister rd, RV64FRegister rs1) {
        FCvtLuS(rd, rs1, RV64FPRoundingMode.kDefault);
    }

    default void FCvtLuD(RV64XRegister rd, RV64FRegister rs1) {
        FCvtLuD(rd, rs1, RV64FPRoundingMode.kDefault);
    }

    default void FCvtSW(RV64FRegister rd, RV64XRegister rs1) {
        FCvtSW(rd, rs1, RV64FPRoundingMode.kDefault);
    }

    default void FCvtDW(RV64FRegister rd, RV64XRegister rs1) {
        FCvtDW(rd, rs1, RV64FPRoundingMode.kIgnored);
    }

    default void FCvtSWu(RV64FRegister rd, RV64XRegister rs1) {
        FCvtSWu(rd, rs1, RV64FPRoundingMode.kDefault);
    }

    default void FCvtDWu(RV64FRegister rd, RV64XRegister rs1) {
        FCvtDWu(rd, rs1, RV64FPRoundingMode.kIgnored);
    }

    default void FCvtSL(RV64FRegister rd, RV64XRegister rs1) {
        FCvtSL(rd, rs1, RV64FPRoundingMode.kDefault);
    }

    default void FCvtDL(RV64FRegister rd, RV64XRegister rs1) {
        FCvtDL(rd, rs1, RV64FPRoundingMode.kDefault);
    }

    default void FCvtSLu(RV64FRegister rd, RV64XRegister rs1) {
        FCvtSLu(rd, rs1, RV64FPRoundingMode.kDefault);
    }

    default void FCvtDLu(RV64FRegister rd, RV64XRegister rs1) {
        FCvtDLu(rd, rs1, RV64FPRoundingMode.kDefault);
    }

    // FP move instructions (RV32F+RV32D): opcode = 0x53, funct3 = 0x0, funct7 = 0b111X00D
    void FMvXW(RV64XRegister rd, RV64FRegister rs1);

    void FMvXD(RV64XRegister rd, RV64FRegister rs1);

    void FMvWX(RV64FRegister rd, RV64XRegister rs1);

    void FMvDX(RV64FRegister rd, RV64XRegister rs1);

    // TODO: FPClassMaskType enum
    // FP classify instructions (RV32F+RV32D): opcode = 0x53, funct3 = 0x1, funct7 = 0b111X00D
    void FClassS(RV64XRegister rd, RV64FRegister rs1);

    void FClassD(RV64XRegister rd, RV64FRegister rs1);

    // "C" Standard Extension, Compresseed Instructions
    void CLwsp(RV64XRegister rd, int offset);

    void CLdsp(RV64XRegister rd, int offset);

    void CFLdsp(RV64FRegister rd, int offset);

    void CSwsp(RV64XRegister rs2, int offset);

    void CSdsp(RV64XRegister rs2, int offset);

    void CFSdsp(RV64FRegister rs2, int offset);

    void CLw(RV64XRegister rd_s, RV64XRegister rs1_s, int offset);

    void CLd(RV64XRegister rd_s, RV64XRegister rs1_s, int offset);

    void CFLd(RV64FRegister rd_s, RV64XRegister rs1_s, int offset);

    void CSw(RV64XRegister rs2_s, RV64XRegister rs1_s, int offset);

    void CSd(RV64XRegister rs2_s, RV64XRegister rs1_s, int offset);

    void CFSd(RV64FRegister rs2_s, RV64XRegister rs1_s, int offset);

    void CLi(RV64XRegister rd, int imm);

    void CLui(RV64XRegister rd, /* special */ int nzimm6);

    void CAddi(RV64XRegister rd, /* special */ int nzimm);

    void CAddiw(RV64XRegister rd, int imm);

    void CAddi16Sp(/* special */ int nzimm);

    void CAddi4Spn(RV64XRegister rd_s, /* special */ int nzuimm);

    void CSlli(RV64XRegister rd, int shamt);

    void CSrli(RV64XRegister rd_s, int shamt);

    void CSrai(RV64XRegister rd_s, int shamt);

    void CAndi(RV64XRegister rd_s, int imm);

    void CMv(RV64XRegister rd, RV64XRegister rs2);

    void CAdd(RV64XRegister rd, RV64XRegister rs2);

    void CAnd(RV64XRegister rd_s, RV64XRegister rs2_s);

    void COr(RV64XRegister rd_s, RV64XRegister rs2_s);

    void CXor(RV64XRegister rd_s, RV64XRegister rs2_s);

    void CSub(RV64XRegister rd_s, RV64XRegister rs2_s);

    void CAddw(RV64XRegister rd_s, RV64XRegister rs2_s);

    void CSubw(RV64XRegister rd_s, RV64XRegister rs2_s);

    // "Zcb" Standard Extension, part of "C", opcode = 0b00, 0b01, funct3 = 0b100.
    void CLbu(RV64XRegister rd_s, RV64XRegister rs1_s, int offset);

    void CLhu(RV64XRegister rd_s, RV64XRegister rs1_s, int offset);

    void CLh(RV64XRegister rd_s, RV64XRegister rs1_s, int offset);

    void CSb(RV64XRegister rd_s, RV64XRegister rs1_s, int offset);

    void CSh(RV64XRegister rd_s, RV64XRegister rs1_s, int offset);

    void CZextB(RV64XRegister rd_rs1_s);

    void CSextB(RV64XRegister rd_rs1_s);

    void CZextH(RV64XRegister rd_rs1_s);

    void CSextH(RV64XRegister rd_rs1_s);

    void CZextW(RV64XRegister rd_rs1_s);

    void CNot(RV64XRegister rd_rs1_s);

    void CMul(RV64XRegister rd_s, RV64XRegister rs2_s);
    // "Zcb" Standard Extension End; resume "C" Standard Extension.
    // TODO(riscv64): Reorder "Zcb" after remaining "C" instructions.

    void CJ(int offset);

    void CJr(RV64XRegister rs1);

    void CJalr(RV64XRegister rs1);

    void CBeqz(RV64XRegister rs1_s, int offset);

    void CBnez(RV64XRegister rs1_s, int offset);

    void CEbreak();

    void CNop();

    void CUnimp();

    // "Zba" Standard Extension, opcode = 0x1b, 0x33 or 0x3b, funct3 and funct7 varies.
    void AddUw(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Sh1Add(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Sh1AddUw(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Sh2Add(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Sh2AddUw(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Sh3Add(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Sh3AddUw(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void SlliUw(RV64XRegister rd, RV64XRegister rs1, int shamt);

    // "Zbb" Standard Extension, opcode = 0x13, 0x1b, 0x33 or 0x3b, funct3 and funct7 varies.
    // Note: 32-bit sext.b, sext.h and zext.h from the Zbb extension are explicitly
    // prefixed with "Zbb" to differentiate them from the utility macros.
    void Andn(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Orn(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Xnor(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Clz(RV64XRegister rd, RV64XRegister rs1);

    void Clzw(RV64XRegister rd, RV64XRegister rs1);

    void Ctz(RV64XRegister rd, RV64XRegister rs1);

    void Ctzw(RV64XRegister rd, RV64XRegister rs1);

    void Cpop(RV64XRegister rd, RV64XRegister rs1);

    void Cpopw(RV64XRegister rd, RV64XRegister rs1);

    void Min(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Minu(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Max(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Maxu(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Rol(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Rolw(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Ror(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Rorw(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Rori(RV64XRegister rd, RV64XRegister rs1, int shamt);

    void Roriw(RV64XRegister rd, RV64XRegister rs1, int shamt);

    void OrcB(RV64XRegister rd, RV64XRegister rs1);

    void Rev8(RV64XRegister rd, RV64XRegister rs1);

    void ZbbSextB(RV64XRegister rd, RV64XRegister rs1);

    void ZbbSextH(RV64XRegister rd, RV64XRegister rs1);

    void ZbbZextH(RV64XRegister rd, RV64XRegister rs1);

    // "Zbs" Standard Extension, opcode = 0x13, or 0x33, funct3 and funct7 varies.
    void Bclr(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Bclri(RV64XRegister rd, RV64XRegister rs1, int shamt);

    void Bext(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Bexti(RV64XRegister rd, RV64XRegister rs1, int shamt);

    void Binv(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Binvi(RV64XRegister rd, RV64XRegister rs1, int shamt);

    void Bset(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    void Bseti(RV64XRegister rd, RV64XRegister rs1, int shamt);

    //____________________________ RISC-V Vector Instructions  START _____________________________//

    // Vector Conguration-Setting Instructions, opcode = 0x57, funct3 = 0x3
    void VSetvli(RV64XRegister rd, RV64XRegister rs1, int vtypei);

    void VSetivli(RV64XRegister rd, /* unsigned */ int uimm, int vtypei);

    void VSetvl(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2);

    static int VTypeiValue(RV64VectorMaskAgnostic vma,
                           RV64VectorTailAgnostic vta,
                           RV64SelectedElementWidth sew,
                           RV64LengthMultiplier lmul) {
        return (vma.value() << 7) | (vta.value() << 6)
                | (sew.value() << 3) | lmul.value();
    }

    // Vector Unit-Stride Load/Store Instructions
    void VLe8(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLe16(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLe32(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLe64(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLm(RV64VRegister vd, RV64XRegister rs1);

    void VSe8(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm);

    void VSe16(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm);

    void VSe32(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm);

    void VSe64(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm);

    void VSm(RV64VRegister vs3, RV64XRegister rs1);

    // Vector unit-stride fault-only-first Instructions
    void VLe8ff(RV64VRegister vd, RV64XRegister rs1);

    void VLe16ff(RV64VRegister vd, RV64XRegister rs1);

    void VLe32ff(RV64VRegister vd, RV64XRegister rs1);

    void VLe64ff(RV64VRegister vd, RV64XRegister rs1);

    // Vector Strided Load/Store Instructions
    void VLse8(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VLse16(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VLse32(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VLse64(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VSse8(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VSse16(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VSse32(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VSse64(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    // Vector Indexed Load/Store Instructions
    void VLoxei8(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLoxei16(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLoxei32(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLoxei64(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLuxei8(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLuxei16(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLuxei32(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLuxei64(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSoxei8(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSoxei16(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSoxei32(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSoxei64(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSuxei8(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSuxei16(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSuxei32(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSuxei64(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    // Vector Segment Load/Store

    // Vector Unit-Stride Segment Loads/Stores

    void VLseg2e8(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg2e16(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg2e32(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg2e64(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg3e8(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg3e16(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg3e32(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg3e64(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg4e8(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg4e16(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg4e32(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg4e64(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg5e8(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg5e16(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg5e32(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg5e64(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg6e8(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg6e16(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg6e32(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg6e64(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg7e8(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg7e16(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg7e32(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg7e64(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg8e8(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg8e16(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg8e32(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg8e64(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VSseg2e8(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm);

    void VSseg2e16(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm);

    void VSseg2e32(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm);

    void VSseg2e64(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm);

    void VSseg3e8(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm);

    void VSseg3e16(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm);

    void VSseg3e32(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm);

    void VSseg3e64(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm);

    void VSseg4e8(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm);

    void VSseg4e16(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm);

    void VSseg4e32(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm);

    void VSseg4e64(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm);

    void VSseg5e8(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm);

    void VSseg5e16(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm);

    void VSseg5e32(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm);

    void VSseg5e64(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm);

    void VSseg6e8(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm);

    void VSseg6e16(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm);

    void VSseg6e32(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm);

    void VSseg6e64(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm);

    void VSseg7e8(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm);

    void VSseg7e16(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm);

    void VSseg7e32(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm);

    void VSseg7e64(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm);

    void VSseg8e8(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm);

    void VSseg8e16(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm);

    void VSseg8e32(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm);

    void VSseg8e64(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm);

    // Vector Unit-Stride Fault-only-First Segment Loads

    void VLseg2e8ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg2e16ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg2e32ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg2e64ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg3e8ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg3e16ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg3e32ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg3e64ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg4e8ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg4e16ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg4e32ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg4e64ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg5e8ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg5e16ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg5e32ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg5e64ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg6e8ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg6e16ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg6e32ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg6e64ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg7e8ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg7e16ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg7e32ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg7e64ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg8e8ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg8e16ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg8e32ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    void VLseg8e64ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm);

    // Vector Strided Segment Loads/Stores

    void VLsseg2e8(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VLsseg2e16(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VLsseg2e32(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VLsseg2e64(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VLsseg3e8(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VLsseg3e16(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VLsseg3e32(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VLsseg3e64(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VLsseg4e8(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VLsseg4e16(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VLsseg4e32(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VLsseg4e64(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VLsseg5e8(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VLsseg5e16(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VLsseg5e32(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VLsseg5e64(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VLsseg6e8(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VLsseg6e16(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VLsseg6e32(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VLsseg6e64(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VLsseg7e8(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VLsseg7e16(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VLsseg7e32(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VLsseg7e64(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VLsseg8e8(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VLsseg8e16(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VLsseg8e32(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VLsseg8e64(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VSsseg2e8(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VSsseg2e16(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VSsseg2e32(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VSsseg2e64(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VSsseg3e8(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VSsseg3e16(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VSsseg3e32(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VSsseg3e64(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VSsseg4e8(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VSsseg4e16(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VSsseg4e32(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VSsseg4e64(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VSsseg5e8(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VSsseg5e16(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VSsseg5e32(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VSsseg5e64(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VSsseg6e8(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VSsseg6e16(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VSsseg6e32(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VSsseg6e64(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VSsseg7e8(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VSsseg7e16(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VSsseg7e32(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VSsseg7e64(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VSsseg8e8(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VSsseg8e16(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VSsseg8e32(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    void VSsseg8e64(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm);

    // Vector Indexed-unordered Segment Loads/Stores

    void VLuxseg2ei8(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLuxseg2ei16(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLuxseg2ei32(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLuxseg2ei64(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLuxseg3ei8(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLuxseg3ei16(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLuxseg3ei32(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLuxseg3ei64(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLuxseg4ei8(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLuxseg4ei16(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLuxseg4ei32(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLuxseg4ei64(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLuxseg5ei8(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLuxseg5ei16(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLuxseg5ei32(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLuxseg5ei64(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLuxseg6ei8(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLuxseg6ei16(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLuxseg6ei32(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLuxseg6ei64(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLuxseg7ei8(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLuxseg7ei16(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLuxseg7ei32(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLuxseg7ei64(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLuxseg8ei8(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLuxseg8ei16(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLuxseg8ei32(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLuxseg8ei64(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSuxseg2ei8(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSuxseg2ei16(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSuxseg2ei32(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSuxseg2ei64(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSuxseg3ei8(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSuxseg3ei16(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSuxseg3ei32(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSuxseg3ei64(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSuxseg4ei8(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSuxseg4ei16(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSuxseg4ei32(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSuxseg4ei64(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSuxseg5ei8(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSuxseg5ei16(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSuxseg5ei32(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSuxseg5ei64(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSuxseg6ei8(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSuxseg6ei16(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSuxseg6ei32(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSuxseg6ei64(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSuxseg7ei8(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSuxseg7ei16(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSuxseg7ei32(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSuxseg7ei64(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSuxseg8ei8(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSuxseg8ei16(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSuxseg8ei32(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSuxseg8ei64(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    // Vector Indexed-ordered Segment Loads/Stores

    void VLoxseg2ei8(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLoxseg2ei16(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLoxseg2ei32(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLoxseg2ei64(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLoxseg3ei8(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLoxseg3ei16(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLoxseg3ei32(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLoxseg3ei64(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLoxseg4ei8(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLoxseg4ei16(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLoxseg4ei32(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLoxseg4ei64(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLoxseg5ei8(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLoxseg5ei16(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLoxseg5ei32(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLoxseg5ei64(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLoxseg6ei8(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLoxseg6ei16(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLoxseg6ei32(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLoxseg6ei64(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLoxseg7ei8(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLoxseg7ei16(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLoxseg7ei32(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLoxseg7ei64(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLoxseg8ei8(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLoxseg8ei16(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLoxseg8ei32(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VLoxseg8ei64(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSoxseg2ei8(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSoxseg2ei16(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSoxseg2ei32(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSoxseg2ei64(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSoxseg3ei8(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSoxseg3ei16(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSoxseg3ei32(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSoxseg3ei64(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSoxseg4ei8(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSoxseg4ei16(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSoxseg4ei32(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSoxseg4ei64(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSoxseg5ei8(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSoxseg5ei16(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSoxseg5ei32(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSoxseg5ei64(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSoxseg6ei8(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSoxseg6ei16(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSoxseg6ei32(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSoxseg6ei64(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSoxseg7ei8(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSoxseg7ei16(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSoxseg7ei32(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSoxseg7ei64(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSoxseg8ei8(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSoxseg8ei16(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSoxseg8ei32(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    void VSoxseg8ei64(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    // Vector Whole Register Load/Store Instructions

    void VL1re8(RV64VRegister vd, RV64XRegister rs1);

    void VL1re16(RV64VRegister vd, RV64XRegister rs1);

    void VL1re32(RV64VRegister vd, RV64XRegister rs1);

    void VL1re64(RV64VRegister vd, RV64XRegister rs1);

    void VL2re8(RV64VRegister vd, RV64XRegister rs1);

    void VL2re16(RV64VRegister vd, RV64XRegister rs1);

    void VL2re32(RV64VRegister vd, RV64XRegister rs1);

    void VL2re64(RV64VRegister vd, RV64XRegister rs1);

    void VL4re8(RV64VRegister vd, RV64XRegister rs1);

    void VL4re16(RV64VRegister vd, RV64XRegister rs1);

    void VL4re32(RV64VRegister vd, RV64XRegister rs1);

    void VL4re64(RV64VRegister vd, RV64XRegister rs1);

    void VL8re8(RV64VRegister vd, RV64XRegister rs1);

    void VL8re16(RV64VRegister vd, RV64XRegister rs1);

    void VL8re32(RV64VRegister vd, RV64XRegister rs1);

    void VL8re64(RV64VRegister vd, RV64XRegister rs1);

    void VL1r(RV64VRegister vd, RV64XRegister rs1);  // Pseudoinstruction equal to VL1re8

    void VL2r(RV64VRegister vd, RV64XRegister rs1);  // Pseudoinstruction equal to VL2re8

    void VL4r(RV64VRegister vd, RV64XRegister rs1);  // Pseudoinstruction equal to VL4re8

    void VL8r(RV64VRegister vd, RV64XRegister rs1);  // Pseudoinstruction equal to VL8re8

    void VS1r(RV64VRegister vs3, RV64XRegister rs1);  // Store {vs3} to address in a1

    void VS2r(RV64VRegister vs3, RV64XRegister rs1);  // Store {vs3}-{vs3 + 1} to address in a1

    void VS4r(RV64VRegister vs3, RV64XRegister rs1);  // Store {vs3}-{vs3 + 3} to address in a1

    void VS8r(RV64VRegister vs3, RV64XRegister rs1);  // Store {vs3}-{vs3 + 7} to address in a1

    // Vector Arithmetic Instruction

    // Vector vadd instructions, funct6 = 0b000000
    void VAdd_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VAdd_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    void VAdd_vi(RV64VRegister vd, RV64VRegister vs2, int imm5, RV64VM vm);

    // Vector vsub instructions, funct6 = 0b000010
    void VSub_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VSub_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    // Vector vrsub instructions, funct6 = 0b000011
    void VRsub_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    void VRsub_vi(RV64VRegister vd, RV64VRegister vs2, int imm5, RV64VM vm);

    // Pseudo-instruction over VRsub_vi
    void VNeg_v(RV64VRegister vd, RV64VRegister vs2);

    // Vector vminu instructions, funct6 = 0b000100
    void VMinu_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VMinu_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    // Vector vmin instructions, funct6 = 0b000101
    void VMin_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VMin_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    // Vector vmaxu instructions, funct6 = 0b000110
    void VMaxu_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VMaxu_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    // Vector vmax instructions, funct6 = 0b000111
    void VMax_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VMax_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    // Vector vand instructions, funct6 = 0b001001
    void VAnd_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VAnd_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    void VAnd_vi(RV64VRegister vd, RV64VRegister vs2, int imm5, RV64VM vm);

    // Vector vor instructions, funct6 = 0b001010
    void VOr_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VOr_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    void VOr_vi(RV64VRegister vd, RV64VRegister vs2, int imm5, RV64VM vm);

    // Vector vxor instructions, funct6 = 0b001011
    void VXor_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VXor_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    void VXor_vi(RV64VRegister vd, RV64VRegister vs2, int imm5, RV64VM vm);

    // Pseudo-instruction over VXor_vi
    void VNot_v(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    // Vector vrgather instructions, funct6 = 0b001100
    void VRgather_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VRgather_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    void VRgather_vi(RV64VRegister vd, RV64VRegister vs2, /* unsigned */ int uimm5, RV64VM vm);

    // Vector vslideup instructions, funct6 = 0b001110
    void VSlideup_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    void VSlideup_vi(RV64VRegister vd, RV64VRegister vs2, /* unsigned */ int uimm5, RV64VM vm);

    // Vector vrgatherei16 instructions, funct6 = 0b001110
    void VRgatherei16_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    // Vector vslidedown instructions, funct6 = 0b001111
    void VSlidedown_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    void VSlidedown_vi(RV64VRegister vd, RV64VRegister vs2, /* unsigned */ int uimm5, RV64VM vm);

    // Vector vadc instructions, funct6 = 0b010000
    void VAdc_vvm(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1);

    void VAdc_vxm(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1);

    void VAdc_vim(RV64VRegister vd, RV64VRegister vs2, int imm5);

    // Vector vmadc instructions, funct6 = 0b010001
    void VMadc_vvm(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1);

    void VMadc_vxm(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1);

    void VMadc_vim(RV64VRegister vd, RV64VRegister vs2, int imm5);

    // Vector vmadc instructions, funct6 = 0b010001
    void VMadc_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1);

    void VMadc_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1);

    void VMadc_vi(RV64VRegister vd, RV64VRegister vs2, int imm5);

    // Vector vsbc instructions, funct6 = 0b010010
    void VSbc_vvm(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1);

    void VSbc_vxm(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1);

    // Vector vmsbc instructions, funct6 = 0b010011
    void VMsbc_vvm(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1);

    void VMsbc_vxm(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1);

    void VMsbc_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1);

    void VMsbc_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1);

    // Vector vmerge instructions, funct6 = 0b010111, vm = 0
    void VMerge_vvm(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1);

    void VMerge_vxm(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1);

    void VMerge_vim(RV64VRegister vd, RV64VRegister vs2, int imm5);

    // Vector vmv instructions, funct6 = 0b010111, vm = 1, vs2 = v0
    void VMv_vv(RV64VRegister vd, RV64VRegister vs1);

    void VMv_vx(RV64VRegister vd, RV64XRegister rs1);

    void VMv_vi(RV64VRegister vd, int imm5);

    // Vector vmseq instructions, funct6 = 0b011000
    void VMseq_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VMseq_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    void VMseq_vi(RV64VRegister vd, RV64VRegister vs2, int imm5, RV64VM vm);

    // Vector vmsne instructions, funct6 = 0b011001
    void VMsne_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VMsne_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    void VMsne_vi(RV64VRegister vd, RV64VRegister vs2, int imm5, RV64VM vm);

    // Vector vmsltu instructions, funct6 = 0b011010
    void VMsltu_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VMsltu_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    // Pseudo-instruction over VMsltu_vv
    void VMsgtu_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    // Vector vmslt instructions, funct6 = 0b011011
    void VMslt_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VMslt_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    // Pseudo-instruction over VMslt_vv
    void VMsgt_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    // Vector vmsleu instructions, funct6 = 0b011100
    void VMsleu_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VMsleu_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    void VMsleu_vi(RV64VRegister vd, RV64VRegister vs2, int imm5, RV64VM vm);

    // Pseudo-instructions over VMsleu_*
    void VMsgeu_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VMsltu_vi(RV64VRegister vd, RV64VRegister vs2, int aimm5, RV64VM vm);

    // Vector vmsle instructions, funct6 = 0b011101
    void VMsle_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VMsle_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    void VMsle_vi(RV64VRegister vd, RV64VRegister vs2, int imm5, RV64VM vm);

    // Pseudo-instructions over VMsle_*
    void VMsge_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VMslt_vi(RV64VRegister vd, RV64VRegister vs2, int aimm5, RV64VM vm);

    // Vector vmsgtu instructions, funct6 = 0b011110
    void VMsgtu_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    void VMsgtu_vi(RV64VRegister vd, RV64VRegister vs2, int imm5, RV64VM vm);

    // Pseudo-instruction over VMsgtu_vi
    void VMsgeu_vi(RV64VRegister vd, RV64VRegister vs2, int aimm5, RV64VM vm);

    // Vector vmsgt instructions, funct6 = 0b011111
    void VMsgt_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    void VMsgt_vi(RV64VRegister vd, RV64VRegister vs2, int imm5, RV64VM vm);

    // Pseudo-instruction over VMsgt_vi
    void VMsge_vi(RV64VRegister vd, RV64VRegister vs2, int aimm5, RV64VM vm);

    // Vector vsaddu instructions, funct6 = 0b100000
    void VSaddu_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VSaddu_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    void VSaddu_vi(RV64VRegister vd, RV64VRegister vs2, int imm5, RV64VM vm);

    // Vector vsadd instructions, funct6 = 0b100001
    void VSadd_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VSadd_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    void VSadd_vi(RV64VRegister vd, RV64VRegister vs2, int imm5, RV64VM vm);

    // Vector vssubu instructions, funct6 = 0b100010
    void VSsubu_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VSsubu_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    // Vector vssub instructions, funct6 = 0b100011
    void VSsub_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VSsub_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    // Vector vsll instructions, funct6 = 0b100101
    void VSll_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VSll_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    void VSll_vi(RV64VRegister vd, RV64VRegister vs2, /* unsigned */ int uimm5, RV64VM vm);

    // Vector vsmul instructions, funct6 = 0b100111
    void VSmul_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VSmul_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    // Vector vmv<nr>r.v instructions, funct6 = 0b100111
    void Vmv1r_v(RV64VRegister vd, RV64VRegister vs2);

    void Vmv2r_v(RV64VRegister vd, RV64VRegister vs2);

    void Vmv4r_v(RV64VRegister vd, RV64VRegister vs2);

    void Vmv8r_v(RV64VRegister vd, RV64VRegister vs2);

    // Vector vsrl instructions, funct6 = 0b101000
    void VSrl_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VSrl_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    void VSrl_vi(RV64VRegister vd, RV64VRegister vs2, /* unsigned */ int uimm5, RV64VM vm);

    // Vector vsra instructions, funct6 = 0b101001
    void VSra_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VSra_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    void VSra_vi(RV64VRegister vd, RV64VRegister vs2, /* unsigned */ int uimm5, RV64VM vm);

    // Vector vssrl instructions, funct6 = 0b101010
    void VSsrl_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VSsrl_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    void VSsrl_vi(RV64VRegister vd, RV64VRegister vs2, /* unsigned */ int uimm5, RV64VM vm);

    // Vector vssra instructions, funct6 = 0b101011
    void VSsra_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VSsra_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    void VSsra_vi(RV64VRegister vd, RV64VRegister vs2, /* unsigned */ int uimm5, RV64VM vm);

    // Vector vnsrl instructions, funct6 = 0b101100
    void VNsrl_wv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VNsrl_wx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    void VNsrl_wi(RV64VRegister vd, RV64VRegister vs2, /* unsigned */ int uimm5, RV64VM vm);

    // Pseudo-instruction over VNsrl_wx
    void VNcvt_x_x_w(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    // Vector vnsra instructions, funct6 = 0b101101
    void VNsra_wv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VNsra_wx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    void VNsra_wi(RV64VRegister vd, RV64VRegister vs2, /* unsigned */ int uimm5, RV64VM vm);

    // Vector vnclipu instructions, funct6 = 0b101110
    void VNclipu_wv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VNclipu_wx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    void VNclipu_wi(RV64VRegister vd, RV64VRegister vs2, /* unsigned */ int uimm5, RV64VM vm);

    // Vector vnclip instructions, funct6 = 0b101111
    void VNclip_wv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VNclip_wx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    void VNclip_wi(RV64VRegister vd, RV64VRegister vs2, /* unsigned */ int uimm5, RV64VM vm);

    // Vector vwredsumu instructions, funct6 = 0b110000
    void VWredsumu_vs(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    // Vector vwredsum instructions, funct6 = 0b110001
    void VWredsum_vs(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    // Vector vredsum instructions, funct6 = 0b000000
    void VRedsum_vs(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    // Vector vredand instructions, funct6 = 0b000001
    void VRedand_vs(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    // Vector vredor instructions, funct6 = 0b000010
    void VRedor_vs(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    // Vector vredxor instructions, funct6 = 0b000011
    void VRedxor_vs(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    // Vector vredminu instructions, funct6 = 0b000100
    void VRedminu_vs(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    // Vector vredmin instructions, funct6 = 0b000101
    void VRedmin_vs(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    // Vector vredmaxu instructions, funct6 = 0b000110
    void VRedmaxu_vs(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    // Vector vredmax instructions, funct6 = 0b000111
    void VRedmax_vs(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    // Vector vaaddu instructions, funct6 = 0b001000
    void VAaddu_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VAaddu_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    // Vector vaadd instructions, funct6 = 0b001001
    void VAadd_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VAadd_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    // Vector vasubu instructions, funct6 = 0b001010
    void VAsubu_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VAsubu_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    // Vector vasub instructions, funct6 = 0b001011
    void VAsub_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VAsub_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    // Vector vslide1up instructions, funct6 = 0b001110
    void VSlide1up_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    // Vector vslide1down instructions, funct6 = 0b001111
    void VSlide1down_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    // Vector vcompress instructions, funct6 = 0b010111
    void VCompress_vm(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1);

    // Vector vmandn instructions, funct6 = 0b011000
    void VMandn_mm(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1);

    // Vector vmand instructions, funct6 = 0b011001
    void VMand_mm(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1);

    // Pseudo-instruction over VMand_mm
    void VMmv_m(RV64VRegister vd, RV64VRegister vs2);

    // Vector vmor instructions, funct6 = 0b011010
    void VMor_mm(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1);

    // Vector vmxor instructions, funct6 = 0b011011
    void VMxor_mm(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1);

    // Pseudo-instruction over VMxor_mm
    void VMclr_m(RV64VRegister vd);

    // Vector vmorn instructions, funct6 = 0b011100
    void VMorn_mm(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1);

    // Vector vmnand instructions, funct6 = 0b011101
    void VMnand_mm(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1);

    // Pseudo-instruction over VMnand_mm
    void VMnot_m(RV64VRegister vd, RV64VRegister vs2);

    // Vector vmnor instructions, funct6 = 0b011110
    void VMnor_mm(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1);

    // Vector vmxnor instructions, funct6 = 0b011111
    void VMxnor_mm(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1);

    // Pseudo-instruction over VMxnor_mm
    void VMset_m(RV64VRegister vd);

    // Vector vdivu instructions, funct6 = 0b100000
    void VDivu_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VDivu_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    // Vector vdiv instructions, funct6 = 0b100001
    void VDiv_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VDiv_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    // Vector vremu instructions, funct6 = 0b100010
    void VRemu_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VRemu_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    // Vector vrem instructions, funct6 = 0b100011
    void VRem_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VRem_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    // Vector vmulhu instructions, funct6 = 0b100100
    void VMulhu_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VMulhu_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    // Vector vmul instructions, funct6 = 0b100101
    void VMul_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VMul_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    // Vector vmulhsu instructions, funct6 = 0b100110
    void VMulhsu_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VMulhsu_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    // Vector vmulh instructions, funct6 = 0b100111
    void VMulh_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VMulh_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    // Vector vmadd instructions, funct6 = 0b101001
    void VMadd_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm);

    void VMadd_vx(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    // Vector vnmsub instructions, funct6 = 0b101011
    void VNmsub_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm);

    void VNmsub_vx(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    // Vector vmacc instructions, funct6 = 0b101101
    void VMacc_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm);

    void VMacc_vx(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    // Vector vnmsac instructions, funct6 = 0b101111
    void VNmsac_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm);

    void VNmsac_vx(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    // Vector vwaddu instructions, funct6 = 0b110000
    void VWaddu_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VWaddu_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    // Pseudo-instruction over VWaddu_vx
    void VWcvtu_x_x_v(RV64VRegister vd, RV64VRegister vs, RV64VM vm);

    // Vector vwadd instructions, funct6 = 0b110001
    void VWadd_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VWadd_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    // Pseudo-instruction over VWadd_vx
    void VWcvt_x_x_v(RV64VRegister vd, RV64VRegister vs, RV64VM vm);

    // Vector vwsubu instructions, funct6 = 0b110010
    void VWsubu_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VWsubu_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    // Vector vwsub instructions, funct6 = 0b110011
    void VWsub_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VWsub_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    // Vector vwaddu.w instructions, funct6 = 0b110100
    void VWaddu_wv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VWaddu_wx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    // Vector vwadd.w instructions, funct6 = 0b110101
    void VWadd_wv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VWadd_wx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    // Vector vwsubu.w instructions, funct6 = 0b110110
    void VWsubu_wv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VWsubu_wx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    // Vector vwsub.w instructions, funct6 = 0b110111
    void VWsub_wv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VWsub_wx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    // Vector vwmulu instructions, funct6 = 0b111000
    void VWmulu_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VWmulu_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    // Vector vwmulsu instructions, funct6 = 0b111010
    void VWmulsu_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VWmulsu_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    // Vector vwmul instructions, funct6 = 0b111011
    void VWmul_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VWmul_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm);

    // Vector vwmaccu instructions, funct6 = 0b111100
    void VWmaccu_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm);

    void VWmaccu_vx(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    // Vector vwmacc instructions, funct6 = 0b111101
    void VWmacc_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm);

    void VWmacc_vx(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    // Vector vwmaccus instructions, funct6 = 0b111110
    void VWmaccus_vx(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    // Vector vwmaccsu instructions, funct6 = 0b111111
    void VWmaccsu_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm);

    void VWmaccsu_vx(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm);

    // Vector vfadd instructions, funct6 = 0b000000
    void VFadd_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VFadd_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm);

    // Vector vfredusum instructions, funct6 = 0b000001
    void VFredusum_vs(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    // Vector vfsub instructions, funct6 = 0b000010
    void VFsub_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VFsub_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm);

    // Vector vfredosum instructions, funct6 = 0b000011
    void VFredosum_vs(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    // Vector vfmin instructions, funct6 = 0b000100
    void VFmin_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VFmin_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm);

    // Vector vfredmin instructions, funct6 = 0b000101
    void VFredmin_vs(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    // Vector vfmax instructions, funct6 = 0b000110
    void VFmax_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VFmax_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm);

    // Vector vfredmax instructions, funct6 = 0b000111
    void VFredmax_vs(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    // Vector vfsgnj instructions, funct6 = 0b001000
    void VFsgnj_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VFsgnj_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm);

    // Vector vfsgnjn instructions, funct6 = 0b001001
    void VFsgnjn_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VFsgnjn_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm);

    // Pseudo-instruction over VFsgnjn_vv
    void VFneg_v(RV64VRegister vd, RV64VRegister vs);

    // Vector vfsgnjx instructions, funct6 = 0b001010
    void VFsgnjx_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VFsgnjx_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm);

    // Pseudo-instruction over VFsgnjx_vv
    void VFabs_v(RV64VRegister vd, RV64VRegister vs);

    // Vector vfslide1up instructions, funct6 = 0b001110
    void VFslide1up_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm);

    // Vector vfslide1down instructions, funct6 = 0b001111
    void VFslide1down_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm);

    // Vector vfmerge/vfmv instructions, funct6 = 0b010111
    void VFmerge_vfm(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1);

    void VFmv_v_f(RV64VRegister vd, RV64FRegister fs1);

    // Vector vmfeq instructions, funct6 = 0b011000
    void VMfeq_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VMfeq_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm);

    // Vector vmfle instructions, funct6 = 0b011001
    void VMfle_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VMfle_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm);

    // Pseudo-instruction over VMfle_vv
    void VMfge_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    // Vector vmflt instructions, funct6 = 0b011011
    void VMflt_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VMflt_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm);

    // Pseudo-instruction over VMflt_vv
    void VMfgt_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    // Vector vmfne instructions, funct6 = 0b011100
    void VMfne_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VMfne_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm);

    // Vector vmfgt instructions, funct6 = 0b011101
    void VMfgt_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm);

    // Vector vmfge instructions, funct6 = 0b011111
    void VMfge_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm);

    // Vector vfdiv instructions, funct6 = 0b100000
    void VFdiv_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VFdiv_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm);

    // Vector vfrdiv instructions, funct6 = 0b100001
    void VFrdiv_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm);

    // Vector vfmul instructions, funct6 = 0b100100
    void VFmul_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VFmul_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm);

    // Vector vfrsub instructions, funct6 = 0b100111
    void VFrsub_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm);

    // Vector vfmadd instructions, funct6 = 0b101000
    void VFmadd_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm);

    void VFmadd_vf(RV64VRegister vd, RV64FRegister fs1, RV64VRegister vs2, RV64VM vm);

    // Vector vfnmadd instructions, funct6 = 0b101001
    void VFnmadd_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm);

    void VFnmadd_vf(RV64VRegister vd, RV64FRegister fs1, RV64VRegister vs2, RV64VM vm);

    // Vector vfmsub instructions, funct6 = 0b101010
    void VFmsub_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm);

    void VFmsub_vf(RV64VRegister vd, RV64FRegister fs1, RV64VRegister vs2, RV64VM vm);

    // Vector vfnmsub instructions, funct6 = 0b101011
    void VFnmsub_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm);

    void VFnmsub_vf(RV64VRegister vd, RV64FRegister fs1, RV64VRegister vs2, RV64VM vm);

    // Vector vfmacc instructions, funct6 = 0b101100
    void VFmacc_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm);

    void VFmacc_vf(RV64VRegister vd, RV64FRegister fs1, RV64VRegister vs2, RV64VM vm);

    // Vector vfnmacc instructions, funct6 = 0b101101
    void VFnmacc_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm);

    void VFnmacc_vf(RV64VRegister vd, RV64FRegister fs1, RV64VRegister vs2, RV64VM vm);

    // Vector vfmsac instructions, funct6 = 0b101110
    void VFmsac_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm);

    void VFmsac_vf(RV64VRegister vd, RV64FRegister fs1, RV64VRegister vs2, RV64VM vm);

    // Vector vfnmsac instructions, funct6 = 0b101111
    void VFnmsac_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm);

    void VFnmsac_vf(RV64VRegister vd, RV64FRegister fs1, RV64VRegister vs2, RV64VM vm);

    // Vector vfwadd instructions, funct6 = 0b110000
    void VFwadd_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VFwadd_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm);

    // Vector vfwredusum instructions, funct6 = 0b110001
    void VFwredusum_vs(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    // Vector vfwsub instructions, funct6 = 0b110010
    void VFwsub_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VFwsub_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm);

    // Vector vfwredosum instructions, funct6 = 0b110011
    void VFwredosum_vs(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    // Vector vfwadd.w instructions, funct6 = 0b110100
    void VFwadd_wv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VFwadd_wf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm);

    // Vector vfwsub.w instructions, funct6 = 0b110110
    void VFwsub_wv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VFwsub_wf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm);

    // Vector vfwmul instructions, funct6 = 0b111000
    void VFwmul_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm);

    void VFwmul_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm);

    // Vector vfwmacc instructions, funct6 = 0b111100
    void VFwmacc_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm);

    void VFwmacc_vf(RV64VRegister vd, RV64FRegister fs1, RV64VRegister vs2, RV64VM vm);

    // Vector vfwnmacc instructions, funct6 = 0b111101
    void VFwnmacc_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm);

    void VFwnmacc_vf(RV64VRegister vd, RV64FRegister fs1, RV64VRegister vs2, RV64VM vm);

    // Vector vfwmsac instructions, funct6 = 0b111110
    void VFwmsac_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm);

    void VFwmsac_vf(RV64VRegister vd, RV64FRegister fs1, RV64VRegister vs2, RV64VM vm);

    // Vector vfwnmsac instructions, funct6 = 0b111111
    void VFwnmsac_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm);

    void VFwnmsac_vf(RV64VRegister vd, RV64FRegister fs1, RV64VRegister vs2, RV64VM vm);

    // Vector VRXUNARY0 kind instructions, funct6 = 0b010000
    void VMv_s_x(RV64VRegister vd, RV64XRegister rs1);

    // Vector VWXUNARY0 kind instructions, funct6 = 0b010000
    void VMv_x_s(RV64XRegister rd, RV64VRegister vs2);

    void VCpop_m(RV64XRegister rd, RV64VRegister vs2, RV64VM vm);

    void VFirst_m(RV64XRegister rd, RV64VRegister vs2, RV64VM vm);

    // Vector VXUNARY0 kind instructions, funct6 = 0b010010
    void VZext_vf8(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    void VSext_vf8(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    void VZext_vf4(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    void VSext_vf4(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    void VZext_vf2(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    void VSext_vf2(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    // Vector VRFUNARY0 kind instructions, funct6 = 0b010000
    void VFmv_s_f(RV64VRegister vd, RV64FRegister fs1);

    // Vector VWFUNARY0 kind instructions, funct6 = 0b010000
    void VFmv_f_s(RV64FRegister fd, RV64VRegister vs2);

    // Vector VFUNARY0 kind instructions, funct6 = 0b010010
    void VFcvt_xu_f_v(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    void VFcvt_x_f_v(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    void VFcvt_f_xu_v(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    void VFcvt_f_x_v(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    void VFcvt_rtz_xu_f_v(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    void VFcvt_rtz_x_f_v(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    void VFwcvt_xu_f_v(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    void VFwcvt_x_f_v(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    void VFwcvt_f_xu_v(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    void VFwcvt_f_x_v(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    void VFwcvt_f_f_v(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    void VFwcvt_rtz_xu_f_v(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    void VFwcvt_rtz_x_f_v(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    void VFncvt_xu_f_w(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    void VFncvt_x_f_w(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    void VFncvt_f_xu_w(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    void VFncvt_f_x_w(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    void VFncvt_f_f_w(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    void VFncvt_rod_f_f_w(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    void VFncvt_rtz_xu_f_w(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    void VFncvt_rtz_x_f_w(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    // Vector VFUNARY1 kind instructions, funct6 = 0b010011
    void VFsqrt_v(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    void VFrsqrt7_v(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    void VFrec7_v(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    void VFclass_v(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    // Vector VMUNARY0 kind instructions, funct6 = 0b010100
    void VMsbf_m(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    void VMsof_m(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    void VMsif_m(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    void VIota_m(RV64VRegister vd, RV64VRegister vs2, RV64VM vm);

    void VId_v(RV64VRegister vd, RV64VM vm);

    //____________________________ RISC-V Vector Instructions  END ____________________________//

    //____________________________ RV64 MACRO Instructions  START _____________________________//

    // These pseudo instructions are from "RISC-V Assembly Programmer's Manual".

    void Nop();

    void Li(RV64XRegister rd, long imm);

    void Mv(RV64XRegister rd, RV64XRegister rs);

    void Not(RV64XRegister rd, RV64XRegister rs);

    void Neg(RV64XRegister rd, RV64XRegister rs);

    void NegW(RV64XRegister rd, RV64XRegister rs);

    void SextB(RV64XRegister rd, RV64XRegister rs);

    void SextH(RV64XRegister rd, RV64XRegister rs);

    void SextW(RV64XRegister rd, RV64XRegister rs);

    void ZextB(RV64XRegister rd, RV64XRegister rs);

    void ZextH(RV64XRegister rd, RV64XRegister rs);

    void ZextW(RV64XRegister rd, RV64XRegister rs);

    void Seqz(RV64XRegister rd, RV64XRegister rs);

    void Snez(RV64XRegister rd, RV64XRegister rs);

    void Sltz(RV64XRegister rd, RV64XRegister rs);

    void Sgtz(RV64XRegister rd, RV64XRegister rs);

    void FMvS(RV64FRegister rd, RV64FRegister rs);

    void FAbsS(RV64FRegister rd, RV64FRegister rs);

    void FNegS(RV64FRegister rd, RV64FRegister rs);

    void FMvD(RV64FRegister rd, RV64FRegister rs);

    void FAbsD(RV64FRegister rd, RV64FRegister rs);

    void FNegD(RV64FRegister rd, RV64FRegister rs);

    // Branch pseudo instructions
    void Beqz(RV64XRegister rs, int offset);

    void Bnez(RV64XRegister rs, int offset);

    void Blez(RV64XRegister rs, int offset);

    void Bgez(RV64XRegister rs, int offset);

    void Bltz(RV64XRegister rs, int offset);

    void Bgtz(RV64XRegister rs, int offset);

    void Bgt(RV64XRegister rs, RV64XRegister rt, int offset);

    void Ble(RV64XRegister rs, RV64XRegister rt, int offset);

    void Bgtu(RV64XRegister rs, RV64XRegister rt, int offset);

    void Bleu(RV64XRegister rs, RV64XRegister rt, int offset);

    // Jump pseudo instructions
    void J(int offset);

    void Jal(int offset);

    void Jr(RV64XRegister rs);

    void Jalr(RV64XRegister rs);

    void Jalr(RV64XRegister rd, RV64XRegister rs);

    void Ret();

    // Pseudo instructions for accessing control and status registers
    void RdCycle(RV64XRegister rd);

    void RdTime(RV64XRegister rd);

    void RdInstret(RV64XRegister rd);

    // TODO: CSRAddress enum
    void Csrr(RV64XRegister rd, int /* 12-bit */ csr);

    void Csrw(int /* 12-bit */ csr, RV64XRegister rs);

    void Csrs(int /* 12-bit */ csr, RV64XRegister rs);

    void Csrc(int /* 12-bit */ csr, RV64XRegister rs);

    void Csrwi(int /* 12-bit */ csr, /* unsigned */ int uimm5);

    void Csrsi(int /* 12-bit */ csr, /* unsigned */ int uimm5);

    void Csrci(int /* 12-bit */ csr, /* unsigned */ int uimm5);

    // Load/store macros for arbitrary 32-bit offsets.
    void Loadb(RV64XRegister tmp, RV64XRegister rd, RV64XRegister rs1, int offset);

    void Loadh(RV64XRegister tmp, RV64XRegister rd, RV64XRegister rs1, int offset);

    void Loadw(RV64XRegister tmp, RV64XRegister rd, RV64XRegister rs1, int offset);

    void Loadd(RV64XRegister tmp, RV64XRegister rd, RV64XRegister rs1, int offset);

    void Loadbu(RV64XRegister tmp, RV64XRegister rd, RV64XRegister rs1, int offset);

    void Loadhu(RV64XRegister tmp, RV64XRegister rd, RV64XRegister rs1, int offset);

    void Loadwu(RV64XRegister tmp, RV64XRegister rd, RV64XRegister rs1, int offset);

    void Storeb(RV64XRegister tmp, RV64XRegister rs2, RV64XRegister rs1, int offset);

    void Storeh(RV64XRegister tmp, RV64XRegister rs2, RV64XRegister rs1, int offset);

    void Storew(RV64XRegister tmp, RV64XRegister rs2, RV64XRegister rs1, int offset);

    void Stored(RV64XRegister tmp, RV64XRegister rs2, RV64XRegister rs1, int offset);

    void FLoadw(RV64XRegister tmp, RV64FRegister rd, RV64XRegister rs1, int offset);

    void FLoadd(RV64XRegister tmp, RV64FRegister rd, RV64XRegister rs1, int offset);

    void FStorew(RV64XRegister tmp, RV64FRegister rs2, RV64XRegister rs1, int offset);

    void FStored(RV64XRegister tmp, RV64FRegister rs2, RV64XRegister rs1, int offset);

    // Macros for loading constants.
    void LoadConst32(RV64XRegister rd, int value);

    void LoadConst64(RV64XRegister rd, long value);

    // Macros for adding constants.
    void AddConst32(RV64XRegister tmp, RV64XRegister rd, RV64XRegister rs1, int value);

    void AddConst64(RV64XRegister tmp, RV64XRegister rd, RV64XRegister rs1, long value);

    // Jumps and branches to a label.
    void Beqz(RV64XRegister rs, RV64Label label, boolean is_bare);

    void Bnez(RV64XRegister rs, RV64Label label, boolean is_bare);

    void Blez(RV64XRegister rs, RV64Label label, boolean is_bare);

    void Bgez(RV64XRegister rs, RV64Label label, boolean is_bare);

    void Bltz(RV64XRegister rs, RV64Label label, boolean is_bare);

    void Bgtz(RV64XRegister rs, RV64Label label, boolean is_bare);

    void Beq(RV64XRegister rs, RV64XRegister rt, RV64Label label, boolean is_bare);

    void Bne(RV64XRegister rs, RV64XRegister rt, RV64Label label, boolean is_bare);

    void Ble(RV64XRegister rs, RV64XRegister rt, RV64Label label, boolean is_bare);

    void Bge(RV64XRegister rs, RV64XRegister rt, RV64Label label, boolean is_bare);

    void Blt(RV64XRegister rs, RV64XRegister rt, RV64Label label, boolean is_bare);

    void Bgt(RV64XRegister rs, RV64XRegister rt, RV64Label label, boolean is_bare);

    void Bleu(RV64XRegister rs, RV64XRegister rt, RV64Label label, boolean is_bare);

    void Bgeu(RV64XRegister rs, RV64XRegister rt, RV64Label label, boolean is_bare);

    void Bltu(RV64XRegister rs, RV64XRegister rt, RV64Label label, boolean is_bare);

    void Bgtu(RV64XRegister rs, RV64XRegister rt, RV64Label label, boolean is_bare);

    void Jal(RV64XRegister rd, RV64Label label, boolean is_bare);

    void J(RV64Label label, boolean is_bare);

    void Jal(RV64Label label, boolean is_bare);

    // RV64Literal load.
    void Loadw(RV64XRegister rd, RV64Literal literal);

    void Loadwu(RV64XRegister rd, RV64Literal literal);

    void Loadd(RV64XRegister rd, RV64Literal literal);

    void FLoadw(RV64XRegister tmp, RV64FRegister rd, RV64Literal literal);

    void FLoadd(RV64XRegister tmp, RV64FRegister rd, RV64Literal literal);

    void LoadLabelAddress(RV64XRegister rd, RV64Label label);

    // Illegal instruction that triggers SIGILL.
    void Unimp();

    //_____________________________ RV64 MACRO Instructions END _____________________________//
}
