package net.fenstonsingel.javassist.examples.outside;

public enum OutsideEnumClass {
    A(1), B(2), C(3);

    private final int fooi;
    OutsideEnumClass(int init) { fooi = init; }

    public int fooi() { return fooi; }
}
