package com.eotsplus.anim;

public final class EotsAnim {
    public static final int IDLE = 0;
    public static final int REAR = 1;
    public static final int DASH = 2;
    public static final int BITE_WINDUP = 3;
    public static final int BITE_SNAP = 4;
    public static final int CHARGE = 5;
    public static final int FIRE = 6;
    public static final int SCALE_RAISE = 7;
    public static final int INHALE = 8;
    public static final int EXHALE = 9;
    public static final int SHED = 10;
    public static final int ULT_SPIRAL = 11;
    public static final int ULT_DIVE = 12;
    public static final int ROAR = 13;
    public static final int COIL = 14;
    public static final int CONSTRICT = 15;
    public static final int BLADE_WINDUP = 16;
    public static final int BLADE_THROW = 17;
    public static final int SERPENT = 18;

    private EotsAnim() {
    }

    public static int id(int raw) {
        return raw & 0xFF;
    }
}
