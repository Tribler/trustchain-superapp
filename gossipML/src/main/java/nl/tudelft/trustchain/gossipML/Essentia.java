package nl.tudelft.trustchain.gossipML;

/**
 * Bridge between Essentia music library and federated ml library
 */
public final class Essentia {
    static {
        System.loadLibrary("superappessentia");
    }
    public native static int extractData(String inputPath, String outputPath);
}
