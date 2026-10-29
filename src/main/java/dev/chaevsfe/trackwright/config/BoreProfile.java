package dev.chaevsfe.trackwright.config;

public record BoreProfile(int left, int right, int up) {
    public static final BoreProfile DEFAULT = new BoreProfile(1, 1, 2);

    public int width() {
        return left + right + 1;
    }

    public int height() {
        return up + 1;
    }

    public BoreProfile withLeft(int value) {
        return new BoreProfile(value, right, up);
    }

    public BoreProfile withRight(int value) {
        return new BoreProfile(left, value, up);
    }

    public BoreProfile withUp(int value) {
        return new BoreProfile(left, right, value);
    }

    public BoreProfile clamp(int widthMax, int headroomMax) {
        int sides = Math.max(0, widthMax - 1);
        int l = Math.max(0, Math.min(sides, left));
        int r = Math.max(0, Math.min(sides, right));
        while (l + r > sides) {
            if (l >= r) {
                l--;
            } else {
                r--;
            }
        }
        int u = Math.max(0, Math.min(Math.max(0, headroomMax), up));
        return new BoreProfile(l, r, u);
    }

    public BoreProfile scaleSides(double multiplier) {
        if (multiplier == 1) {
            return this;
        }
        return new BoreProfile(scale(left, multiplier), scale(right, multiplier), up);
    }

    private static int scale(int extent, double multiplier) {
        return (int) Math.ceil(extent * multiplier - 1.0E-9);
    }

    @Override
    public String toString() {
        return left + "/" + right + "/" + up;
    }
}
