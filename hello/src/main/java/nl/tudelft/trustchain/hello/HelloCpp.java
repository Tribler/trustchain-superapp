package nl.tudelft.trustchain.hello;

public final class HelloCpp {
    static {
        System.loadLibrary("hello_cmake");
    }
    public native static String stringFromJNI();
}
