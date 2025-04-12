package com.v7878.jnasm;

public class Label {
    private static final int BIAS = 1;

    // TODO: should be private, but raw value used in assemblers
    public int position;

    public Label() {
        this.position = 0;
    }

    // Returns the position for bound and linked labels. Cannot be used for unused labels.
    public int getPosition() {
        if (isUnused()) {
            throw new IllegalStateException("Label is unused");
        }
        return isBound() ? -(position + BIAS) : position - BIAS;
    }

    public int getLinkPosition() {
        if (!isLinked()) {
            throw new IllegalStateException("Label is not linked");
        }
        return position - BIAS;
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

    public void reset() {
        this.position = 0;
    }

    public void bindTo(int position) {
        if (position < 0) {
            throw new IllegalStateException("Negative position: " + position);
        }
        if (isBound()) {
            throw new IllegalStateException("Label is already bound");
        }
        // position should be strictly negative
        this.position = -(position + BIAS);
    }

    public void linkTo(int position) {
        if (position < 0) {
            throw new IllegalStateException("Negative position: " + position);
        }
        if (isBound()) {
            throw new IllegalStateException("Label is already bound");
        }
        // position should be strictly positive
        this.position = position + BIAS;
    }
}
