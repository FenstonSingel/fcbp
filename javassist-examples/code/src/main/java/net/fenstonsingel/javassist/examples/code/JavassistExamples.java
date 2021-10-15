package net.fenstonsingel.javassist.examples.code;

public class JavassistExamples {

    private static void example1() {}

    private static void example2() {
        System.out.println("...");
    }

    private static void example3(boolean b) {}

    private static void example4() {}

    public static void main(String[] args) {
        example1();
        example2();
        example3(false);
        example3(true);
        example4();
    }

}
