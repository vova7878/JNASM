package com.v7878.jnasm.common_x86;

public class X86VEXConstants {
    public static final int GET_REX_R = 0x04;
    public static final int GET_REX_X = 0x02;
    public static final int GET_REX_B = 0x01;
    public static final int SET_VEX_R = 0x80;
    public static final int SET_VEX_X = 0x40;
    public static final int SET_VEX_B = 0x20;
    public static final int SET_VEX_M_0F = 0x01;
    public static final int SET_VEX_M_0F_38 = 0x02;
    public static final int SET_VEX_M_0F_3A = 0x03;
    public static final int SET_VEX_W = 0x80;
    public static final int SET_VEX_L_128 = 0x00;
    public static final int SET_VEL_L_256 = 0x04;
    public static final int SET_VEX_PP_NONE = 0x00;
    public static final int SET_VEX_PP_66 = 0x01;
    public static final int SET_VEX_PP_F3 = 0x02;
    public static final int SET_VEX_PP_F2 = 0x03;
    public static final int TWO_BYTE_VEX = 0xC5;
    public static final int THREE_BYTE_VEX = 0xC4;
    public static final int VEX_INIT = 0x00;
}
