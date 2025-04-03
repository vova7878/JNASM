package com.v7878.jnasm;

public class Label {
    public int position;

    public Label() {
        this.position = 0;
    }

    // TODO: replace Integer.BYTES with address size

    // Returns the position for bound and linked labels. Cannot be used for unused labels.
    public int getPosition() {
        assert !isUnused() : "Label is unused";
        return isBound() ? -(position + Integer.BYTES) : position - Integer.BYTES;
    }

    public int getLinkPosition() {
        assert isLinked() : "Label is not linked";
        return position - Integer.BYTES;
    }

    public boolean isBound() {
        return position < 0;
    }

    public boolean isUnused() {
        return position == 0;
    }

    public boolean isLinked() {
        return position > 0;
    }

    public void reinitialize() {
        this.position = 0;
    }

    public void bindTo(int position) {
        assert !isBound() : "Label is already bound";
        this.position = -(position + Integer.BYTES);
        assert isBound() : "Binding failed";
    }

    public void linkTo(int position) {
        assert !isBound() : "Label is already bound";
        this.position = position + Integer.BYTES;
        assert isLinked() : "Linking failed";
    }
}
