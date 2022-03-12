package nl.tudelft.trustchain.frost;

public final class HelloCpp {
    static {
        System.loadLibrary("hello_cmake");
    }
    public native static String stringFromJNI();
}
