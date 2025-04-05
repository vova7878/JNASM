package com.v7878.jnasm.x86;

import com.v7878.jnasm.Label;

/*
  Emit Machine Instructions.
 */
// TODO: javadoc
public interface X86AssemblerI {
    void call(CpuRegister reg);

    void call(Address address);

    void call(Label label);

    void call(ExternalLabel label);

    void pushl(CpuRegister reg);

    void pushl(Address address);

    void pushl(Immediate imm);

    void popl(CpuRegister reg);

    void popl(Address address);

    void movl(CpuRegister dst, Immediate src);

    void movl(CpuRegister dst, CpuRegister src);

    void movl(CpuRegister dst, Address src);

    void movl(Address dst, CpuRegister src);

    void movl(Address dst, Immediate imm);

    void movl(Address dst, Label lbl);

    void movntl(Address dst, CpuRegister src);

    void blsi(CpuRegister dst, CpuRegister src);  // no addr variant (for now)

    void blsmsk(CpuRegister dst, CpuRegister src);  // no addr variant (for now)

    void blsr(CpuRegister dst, CpuRegister src);  // no addr varianr (for now)

    void bswapl(CpuRegister dst);

    void bsfl(CpuRegister dst, CpuRegister src);

    void bsfl(CpuRegister dst, Address src);

    void bsrl(CpuRegister dst, CpuRegister src);

    void bsrl(CpuRegister dst, Address src);

    void popcntl(CpuRegister dst, CpuRegister src);

    void popcntl(CpuRegister dst, Address src);

    void rdtsc();

    void rorl(CpuRegister reg, Immediate imm);

    void rorl(CpuRegister operand, CpuRegister shifter);

    void roll(CpuRegister reg, Immediate imm);

    void roll(CpuRegister operand, CpuRegister shifter);

    void movzxb(CpuRegister dst, ByteRegister src);

    void movzxb(CpuRegister dst, Address src);

    void movsxb(CpuRegister dst, ByteRegister src);

    void movsxb(CpuRegister dst, Address src);

    void movb(CpuRegister dst, Address src);

    void movb(Address dst, ByteRegister src);

    void movb(Address dst, Immediate imm);

    void movzxw(CpuRegister dst, CpuRegister src);

    void movzxw(CpuRegister dst, Address src);

    void movsxw(CpuRegister dst, CpuRegister src);

    void movsxw(CpuRegister dst, Address src);

    void movw(CpuRegister dst, Address src);

    void movw(Address dst, CpuRegister src);

    void movw(Address dst, Immediate imm);

    void leal(CpuRegister dst, Address src);

    void cmovl(Condition condition, CpuRegister dst, CpuRegister src);

    void cmovl(Condition condition, CpuRegister dst, Address src);

    void setb(Condition condition, CpuRegister dst);

    void movaps(XmmRegister dst, XmmRegister src);     // move

    void movaps(XmmRegister dst, Address src);  // load aligned

    void movups(XmmRegister dst, Address src);  // load unaligned

    void movaps(Address dst, XmmRegister src);  // store aligned

    void movups(Address dst, XmmRegister src);  // store unaligned

    void vmovaps(XmmRegister dst, XmmRegister src);     // move

    void vmovaps(XmmRegister dst, Address src);  // load aligned

    void vmovups(XmmRegister dst, Address src);  // load unaligned

    void vmovaps(Address dst, XmmRegister src);  // store aligned

    void vmovups(Address dst, XmmRegister src);  // store unaligned

    void movss(XmmRegister dst, Address src);

    void movss(Address dst, XmmRegister src);

    void movss(XmmRegister dst, XmmRegister src);

    void movd(XmmRegister dst, CpuRegister src);

    void movd(CpuRegister dst, XmmRegister src);

    void addss(XmmRegister dst, XmmRegister src);

    void addss(XmmRegister dst, Address src);

    void subss(XmmRegister dst, XmmRegister src);

    void subss(XmmRegister dst, Address src);

    void mulss(XmmRegister dst, XmmRegister src);

    void mulss(XmmRegister dst, Address src);

    void divss(XmmRegister dst, XmmRegister src);

    void divss(XmmRegister dst, Address src);

    void addps(XmmRegister dst, XmmRegister src);  // no addr variant (for now)

    void subps(XmmRegister dst, XmmRegister src);

    void mulps(XmmRegister dst, XmmRegister src);

    void divps(XmmRegister dst, XmmRegister src);

    void vmulps(XmmRegister dst, XmmRegister src1, XmmRegister src2);

    void vmulpd(XmmRegister dst, XmmRegister src1, XmmRegister src2);

    void vdivps(XmmRegister dst, XmmRegister src1, XmmRegister src2);

    void vdivpd(XmmRegister dst, XmmRegister src1, XmmRegister src2);

    void vaddps(XmmRegister dst, XmmRegister add_left, XmmRegister add_right);

    void vsubps(XmmRegister dst, XmmRegister add_left, XmmRegister add_right);

    void vsubpd(XmmRegister dst, XmmRegister add_left, XmmRegister add_right);

    void vaddpd(XmmRegister dst, XmmRegister add_left, XmmRegister add_right);

    void vfmadd213ss(XmmRegister acc, XmmRegister left, XmmRegister right);

    void vfmadd213sd(XmmRegister acc, XmmRegister left, XmmRegister right);

    void movapd(XmmRegister dst, XmmRegister src);     // move

    void movapd(XmmRegister dst, Address src);  // load aligned

    void movupd(XmmRegister dst, Address src);  // load unaligned

    void movapd(Address dst, XmmRegister src);  // store aligned

    void movupd(Address dst, XmmRegister src);  // store unaligned

    void vmovapd(XmmRegister dst, XmmRegister src);     // move

    void vmovapd(XmmRegister dst, Address src);  // load aligned

    void vmovupd(XmmRegister dst, Address src);  // load unaligned

    void vmovapd(Address dst, XmmRegister src);  // store aligned

    void vmovupd(Address dst, XmmRegister src);  // store unaligned

    void movsd(XmmRegister dst, Address src);

    void movsd(Address dst, XmmRegister src);

    void movsd(XmmRegister dst, XmmRegister src);

    void movhpd(XmmRegister dst, Address src);

    void movhpd(Address dst, XmmRegister src);

    void addsd(XmmRegister dst, XmmRegister src);

    void addsd(XmmRegister dst, Address src);

    void subsd(XmmRegister dst, XmmRegister src);

    void subsd(XmmRegister dst, Address src);

    void mulsd(XmmRegister dst, XmmRegister src);

    void mulsd(XmmRegister dst, Address src);

    void divsd(XmmRegister dst, XmmRegister src);

    void divsd(XmmRegister dst, Address src);

    void addpd(XmmRegister dst, XmmRegister src);  // no addr variant (for now)

    void subpd(XmmRegister dst, XmmRegister src);

    void mulpd(XmmRegister dst, XmmRegister src);

    void divpd(XmmRegister dst, XmmRegister src);

    void movdqa(XmmRegister dst, XmmRegister src);     // move

    void movdqa(XmmRegister dst, Address src);  // load aligned

    void movdqu(XmmRegister dst, Address src);  // load unaligned

    void movdqa(Address dst, XmmRegister src);  // store aligned

    void movdqu(Address dst, XmmRegister src);  // store unaligned

    void vmovdqa(XmmRegister dst, XmmRegister src);     // move

    void vmovdqa(XmmRegister dst, Address src);  // load aligned

    void vmovdqu(XmmRegister dst, Address src);  // load unaligned

    void vmovdqa(Address dst, XmmRegister src);  // store aligned

    void vmovdqu(Address dst, XmmRegister src);  // store unaligned

    void paddb(XmmRegister dst, XmmRegister src);  // no addr variant (for now)

    void psubb(XmmRegister dst, XmmRegister src);

    void vpaddb(XmmRegister dst, XmmRegister add_left, XmmRegister add_right);

    void vpaddw(XmmRegister dst, XmmRegister add_left, XmmRegister add_right);

    void paddw(XmmRegister dst, XmmRegister src);

    void psubw(XmmRegister dst, XmmRegister src);

    void pmullw(XmmRegister dst, XmmRegister src);

    void vpmullw(XmmRegister dst, XmmRegister src1, XmmRegister src2);

    void vpsubb(XmmRegister dst, XmmRegister src1, XmmRegister src2);

    void vpsubw(XmmRegister dst, XmmRegister src1, XmmRegister src2);

    void vpsubd(XmmRegister dst, XmmRegister src1, XmmRegister src2);

    void paddd(XmmRegister dst, XmmRegister src);

    void psubd(XmmRegister dst, XmmRegister src);

    void pmulld(XmmRegister dst, XmmRegister src);

    void vpmulld(XmmRegister dst, XmmRegister src1, XmmRegister src2);

    void vpaddd(XmmRegister dst, XmmRegister src1, XmmRegister src2);

    void paddq(XmmRegister dst, XmmRegister src);

    void psubq(XmmRegister dst, XmmRegister src);

    void vpaddq(XmmRegister dst, XmmRegister add_left, XmmRegister add_right);

    void vpsubq(XmmRegister dst, XmmRegister add_left, XmmRegister add_right);

    void paddusb(XmmRegister dst, XmmRegister src);

    void paddsb(XmmRegister dst, XmmRegister src);

    void paddusw(XmmRegister dst, XmmRegister src);

    void paddsw(XmmRegister dst, XmmRegister src);

    void psubusb(XmmRegister dst, XmmRegister src);

    void psubsb(XmmRegister dst, XmmRegister src);

    void psubusw(XmmRegister dst, XmmRegister src);

    void psubsw(XmmRegister dst, XmmRegister src);

    void cvtsi2ss(XmmRegister dst, CpuRegister src);

    void cvtsi2sd(XmmRegister dst, CpuRegister src);

    void cvtss2si(CpuRegister dst, XmmRegister src);

    void cvtss2sd(XmmRegister dst, XmmRegister src);

    void cvtsd2si(CpuRegister dst, XmmRegister src);

    void cvtsd2ss(XmmRegister dst, XmmRegister src);

    void cvttss2si(CpuRegister dst, XmmRegister src);

    void cvttsd2si(CpuRegister dst, XmmRegister src);

    void cvtdq2ps(XmmRegister dst, XmmRegister src);

    void cvtdq2pd(XmmRegister dst, XmmRegister src);

    void comiss(XmmRegister a, XmmRegister b);

    void comiss(XmmRegister a, Address b);

    void comisd(XmmRegister a, XmmRegister b);

    void comisd(XmmRegister a, Address b);

    void ucomiss(XmmRegister a, XmmRegister b);

    void ucomiss(XmmRegister a, Address b);

    void ucomisd(XmmRegister a, XmmRegister b);

    void ucomisd(XmmRegister a, Address b);

    void roundsd(XmmRegister dst, XmmRegister src, Immediate imm);

    void roundss(XmmRegister dst, XmmRegister src, Immediate imm);

    void sqrtsd(XmmRegister dst, XmmRegister src);

    void sqrtss(XmmRegister dst, XmmRegister src);

    void xorpd(XmmRegister dst, Address src);

    void xorpd(XmmRegister dst, XmmRegister src);

    void xorps(XmmRegister dst, Address src);

    void xorps(XmmRegister dst, XmmRegister src);

    void pxor(XmmRegister dst, XmmRegister src);  // no addr variant (for now)

    void vpxor(XmmRegister dst, XmmRegister src1, XmmRegister src2);

    void vxorps(XmmRegister dst, XmmRegister src1, XmmRegister src2);

    void vxorpd(XmmRegister dst, XmmRegister src1, XmmRegister src2);

    void andpd(XmmRegister dst, XmmRegister src);

    void andpd(XmmRegister dst, Address src);

    void andps(XmmRegister dst, XmmRegister src);

    void andps(XmmRegister dst, Address src);

    void pand(XmmRegister dst, XmmRegister src);  // no addr variant (for now)

    void vpand(XmmRegister dst, XmmRegister src1, XmmRegister src2);

    void vandps(XmmRegister dst, XmmRegister src1, XmmRegister src2);

    void vandpd(XmmRegister dst, XmmRegister src1, XmmRegister src2);

    void andn(CpuRegister dst, CpuRegister src1, CpuRegister src2);  // no addr variant (for now)

    void andnpd(XmmRegister dst, XmmRegister src);  // no addr variant (for now)

    void andnps(XmmRegister dst, XmmRegister src);

    void pandn(XmmRegister dst, XmmRegister src);

    void vpandn(XmmRegister dst, XmmRegister src1, XmmRegister src2);

    void vandnps(XmmRegister dst, XmmRegister src1, XmmRegister src2);

    void vandnpd(XmmRegister dst, XmmRegister src1, XmmRegister src2);

    void orpd(XmmRegister dst, XmmRegister src);  // no addr variant (for now)

    void orps(XmmRegister dst, XmmRegister src);

    void por(XmmRegister dst, XmmRegister src);

    void vpor(XmmRegister dst, XmmRegister src1, XmmRegister src2);

    void vorps(XmmRegister dst, XmmRegister src1, XmmRegister src2);

    void vorpd(XmmRegister dst, XmmRegister src1, XmmRegister src2);

    void pavgb(XmmRegister dst, XmmRegister src);  // no addr variant (for now)

    void pavgw(XmmRegister dst, XmmRegister src);

    void psadbw(XmmRegister dst, XmmRegister src);

    void pmaddwd(XmmRegister dst, XmmRegister src);

    void vpmaddwd(XmmRegister dst, XmmRegister src1, XmmRegister src2);

    void phaddw(XmmRegister dst, XmmRegister src);

    void phaddd(XmmRegister dst, XmmRegister src);

    void haddps(XmmRegister dst, XmmRegister src);

    void haddpd(XmmRegister dst, XmmRegister src);

    void phsubw(XmmRegister dst, XmmRegister src);

    void phsubd(XmmRegister dst, XmmRegister src);

    void hsubps(XmmRegister dst, XmmRegister src);

    void hsubpd(XmmRegister dst, XmmRegister src);

    void pminsb(XmmRegister dst, XmmRegister src);  // no addr variant (for now)

    void pmaxsb(XmmRegister dst, XmmRegister src);

    void pminsw(XmmRegister dst, XmmRegister src);

    void pmaxsw(XmmRegister dst, XmmRegister src);

    void pminsd(XmmRegister dst, XmmRegister src);

    void pmaxsd(XmmRegister dst, XmmRegister src);

    void pminub(XmmRegister dst, XmmRegister src);  // no addr variant (for now)

    void pmaxub(XmmRegister dst, XmmRegister src);

    void pminuw(XmmRegister dst, XmmRegister src);

    void pmaxuw(XmmRegister dst, XmmRegister src);

    void pminud(XmmRegister dst, XmmRegister src);

    void pmaxud(XmmRegister dst, XmmRegister src);

    void minps(XmmRegister dst, XmmRegister src);  // no addr variant (for now)

    void maxps(XmmRegister dst, XmmRegister src);

    void minpd(XmmRegister dst, XmmRegister src);

    void maxpd(XmmRegister dst, XmmRegister src);

    void pcmpeqb(XmmRegister dst, XmmRegister src);

    void pcmpeqw(XmmRegister dst, XmmRegister src);

    void pcmpeqd(XmmRegister dst, XmmRegister src);

    void pcmpeqq(XmmRegister dst, XmmRegister src);

    void pcmpgtb(XmmRegister dst, XmmRegister src);

    void pcmpgtw(XmmRegister dst, XmmRegister src);

    void pcmpgtd(XmmRegister dst, XmmRegister src);

    void pcmpgtq(XmmRegister dst, XmmRegister src);  // SSE4.2

    void shufpd(XmmRegister dst, XmmRegister src, Immediate imm);

    void shufps(XmmRegister dst, XmmRegister src, Immediate imm);

    void pshufd(XmmRegister dst, XmmRegister src, Immediate imm);

    void punpcklbw(XmmRegister dst, XmmRegister src);

    void punpcklwd(XmmRegister dst, XmmRegister src);

    void punpckldq(XmmRegister dst, XmmRegister src);

    void punpcklqdq(XmmRegister dst, XmmRegister src);

    void punpckhbw(XmmRegister dst, XmmRegister src);

    void punpckhwd(XmmRegister dst, XmmRegister src);

    void punpckhdq(XmmRegister dst, XmmRegister src);

    void punpckhqdq(XmmRegister dst, XmmRegister src);

    void psllw(XmmRegister reg, Immediate shift_count);

    void pslld(XmmRegister reg, Immediate shift_count);

    void psllq(XmmRegister reg, Immediate shift_count);

    void psraw(XmmRegister reg, Immediate shift_count);

    void psrad(XmmRegister reg, Immediate shift_count);
    // no psraq

    void psrlw(XmmRegister reg, Immediate shift_count);

    void psrld(XmmRegister reg, Immediate shift_count);

    void psrlq(XmmRegister reg, Immediate shift_count);

    void psrldq(XmmRegister reg, Immediate shift_count);

    void flds(Address src);

    void fstps(Address dst);

    void fsts(Address dst);

    void fldl(Address src);

    void fstpl(Address dst);

    void fstl(Address dst);

    void fstsw();

    void fucompp();

    void fnstcw(Address dst);

    void fldcw(Address src);

    void fistpl(Address dst);

    void fistps(Address dst);

    void fildl(Address src);

    void filds(Address src);

    void fincstp();

    void ffree(Immediate index);

    void fsin();

    void fcos();

    void fptan();

    void fprem();

    void xchgb(ByteRegister dst, ByteRegister src);

    void xchgb(ByteRegister reg, Address address);

    void xchgw(CpuRegister dst, CpuRegister src);

    void xchgw(CpuRegister reg, Address address);

    void xchgl(CpuRegister dst, CpuRegister src);

    void xchgl(CpuRegister reg, Address address);

    void cmpb(Address address, Immediate imm);

    void cmpw(Address address, Immediate imm);

    void cmpl(CpuRegister reg, Immediate imm);

    void cmpl(CpuRegister reg0, CpuRegister reg1);

    void cmpl(CpuRegister reg, Address address);

    void cmpl(Address address, CpuRegister reg);

    void cmpl(Address address, Immediate imm);

    void testl(CpuRegister reg1, CpuRegister reg2);

    void testl(CpuRegister reg, Immediate imm);

    void testl(CpuRegister reg1, Address address);

    void testb(Address dst, Immediate imm);

    void testl(Address dst, Immediate imm);

    void andl(CpuRegister dst, Immediate imm);

    void andl(CpuRegister dst, CpuRegister src);

    void andl(CpuRegister dst, Address address);

    void andw(Address address, Immediate imm);

    void orl(CpuRegister dst, Immediate imm);

    void orl(CpuRegister dst, CpuRegister src);

    void orl(CpuRegister dst, Address address);

    void xorl(CpuRegister dst, CpuRegister src);

    void xorl(CpuRegister dst, Immediate imm);

    void xorl(CpuRegister dst, Address address);

    void addl(CpuRegister dst, CpuRegister src);

    void addl(CpuRegister reg, Immediate imm);

    void addl(CpuRegister reg, Address address);

    void addl(Address address, CpuRegister reg);

    void addl(Address address, Immediate imm);

    void addw(Address address, Immediate imm);

    void addw(CpuRegister reg, Immediate imm);

    void adcl(CpuRegister dst, CpuRegister src);

    void adcl(CpuRegister reg, Immediate imm);

    void adcl(CpuRegister dst, Address address);

    void subl(CpuRegister dst, CpuRegister src);

    void subl(CpuRegister reg, Immediate imm);

    void subl(CpuRegister reg, Address address);

    void subl(Address address, CpuRegister src);

    void cdq();

    void idivl(CpuRegister reg);

    void divl(CpuRegister reg);

    void imull(CpuRegister dst, CpuRegister src);

    void imull(CpuRegister reg, Immediate imm);

    void imull(CpuRegister dst, CpuRegister src, Immediate imm);

    void imull(CpuRegister reg, Address address);

    void imull(CpuRegister reg);

    void imull(Address address);

    void mull(CpuRegister reg);

    void mull(Address address);

    void sbbl(CpuRegister dst, CpuRegister src);

    void sbbl(CpuRegister reg, Immediate imm);

    void sbbl(CpuRegister reg, Address address);

    void sbbl(Address address, CpuRegister src);

    void incl(CpuRegister reg);

    void incl(Address address);

    void decl(CpuRegister reg);

    void decl(Address address);

    void shll(CpuRegister reg, Immediate imm);

    void shll(CpuRegister operand, CpuRegister shifter);

    void shll(Address address, Immediate imm);

    void shll(Address address, CpuRegister shifter);

    void shrl(CpuRegister reg, Immediate imm);

    void shrl(CpuRegister operand, CpuRegister shifter);

    void shrl(Address address, Immediate imm);

    void shrl(Address address, CpuRegister shifter);

    void sarl(CpuRegister reg, Immediate imm);

    void sarl(CpuRegister operand, CpuRegister shifter);

    void sarl(Address address, Immediate imm);

    void sarl(Address address, CpuRegister shifter);

    void shld(CpuRegister dst, CpuRegister src, CpuRegister shifter);

    void shld(CpuRegister dst, CpuRegister src, Immediate imm);

    void shrd(CpuRegister dst, CpuRegister src, CpuRegister shifter);

    void shrd(CpuRegister dst, CpuRegister src, Immediate imm);

    void negl(CpuRegister reg);

    void notl(CpuRegister reg);

    void enter(Immediate imm);

    void leave();

    void ret();

    void ret(Immediate imm);

    void nop();

    void int3();

    void hlt();

    void j(Condition condition, Label label);

    void j(Condition condition, NearLabel label);

    void jecxz(NearLabel label);

    void jmp(CpuRegister reg);

    void jmp(Address address);

    void jmp(Label label);

    void jmp(NearLabel label);

    void repne_scasb();

    void repne_scasw();

    void repe_cmpsb();

    void repe_cmpsw();

    void repe_cmpsl();

    void rep_movsb();

    void rep_movsl();

    void rep_movsw();

    X86AssemblerI lock();

    void cmpxchgb(Address address, ByteRegister reg);

    void cmpxchgw(Address address, CpuRegister reg);

    void cmpxchgl(Address address, CpuRegister reg);

    void cmpxchg8b(Address address);

    void xaddb(Address address, ByteRegister reg);

    void xaddw(Address address, CpuRegister reg);

    void xaddl(Address address, CpuRegister reg);

    void mfence();

    X86AssemblerI fs();

    X86AssemblerI gs();
}
