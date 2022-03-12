package nl.tudelft.trustchain.frost;

public final class FrostCpp {

    static {
        System.loadLibrary("hello_cmake");
    }
    public native static String stringFromJNI();

}
