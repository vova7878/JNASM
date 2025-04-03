package com.v7878.jnasm;

public interface CodeBuffer {
    // Get the size of the emitted code.
    int size();

    void move(int new_position, int old_position, int size);

    void emit8(int value);

    void emit16(int value);

    void emit32(int value);

    void emit64(long value);

    void store8(int position, int value);

    void store16(int position, int value);

    void store32(int position, int value);

    void store64(int position, long value);

    byte load8(int position);

    default int loadU8(int position) {
        return load8(position) & 0xff;
    }

    short load16(int position);

    default int loadU16(int position) {
        return load16(position) & 0xffff;
    }

    int load32(int position);

    long load64(int position);
}
