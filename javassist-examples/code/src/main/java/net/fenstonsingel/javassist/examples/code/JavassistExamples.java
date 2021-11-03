package net.fenstonsingel.javassist.examples.code;

public class JavassistExamples {

    // static insert to the start of the method
    private static void example1() {
    }

    // static inserts to a method line and the end of the method
    private static void example2() {
        System.out.println("...");
    }

    // insert depending on a method parameter
    private static void example3(boolean b) {}

    // insert depending on a varargs method parameter
    private static void example4(boolean... bs) {}

    // insert depending on a local variable
    private static void example5() {
        int i = 21;
        System.out.printf("Hello from example5 w/ %d!\n", i);
    }

    // inserts depending on a class static field and method
    private static void example6() {
        System.out.printf("Hello from example6 w/ %d!\n", fooi);
    }

    // inserts depending on a class nested static class
    private static void example7() {}

    // insert depending on outside interfaces and classes
    private static void example8() {}

    // insert figuring out whether invokevirtual/invokeinterface calls work
    private static void example9() {}

    // insert depending on a generic class
    private static void example10() {}

    // insert depending on an enum class
    private static void example11() {}


    public static void main(String[] args) {
        example1();
        example2();

        example3(false);
        example3(true);
        example4();
        example4(true);
        example4(false, true);

        example5();

        example6();
        example7();

        example8();
        example9();

        example10();
        example11();
    }


    private static int fooi = 84;

    private static void foov() {
        System.out.println("Also hello from foov!");
    }

    public static class A {
        public static int fooi = 42;
        public static void foov() {
            System.out.println("Hello from foov from A!");
        }
    }

    private static void barv(net.fenstonsingel.javassist.examples.outside.OutsideClass oc) {
        System.out.printf("Hello from example9 w/ %d from OutsideClass object!\n", oc.foo());
    }

}
