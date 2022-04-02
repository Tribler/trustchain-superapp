package nl.tudelft.trustchain.frost;

import org.bitcoin.*;

import java.nio.charset.StandardCharsets;

public class FrostCpp {

    public static void a(){
        final int numberOfKeys = 5;
        int threshold = 3;
        byte[][] publicKeys = new byte[numberOfKeys][];

        FrostSigner[] signers = new FrostSigner[numberOfKeys];
        FrostSecret[] secrets = new FrostSecret[numberOfKeys];
        for (int i = 0; i < numberOfKeys; i++) {
            signers[i] = new FrostSigner(3);
            secrets[i] = new FrostSecret();
        }

        byte[][][] shares = new byte[numberOfKeys][numberOfKeys][];
        for (int i = 0; i < numberOfKeys; i++) {
            FrostSigner sig = signers[i];
            FrostSecret sec = secrets[i];
            NativeSecp256k1.generateKey(sec, sig);
            System.out.println("-------Private Key " + i);
            prnt(sec.keypair);
            System.out.println("Public Key " + i);
            prnt(sig.pubkey);
            publicKeys[i] = sig.pubkey;
            System.out.println();
        }
        System.out.println("ok");

        System.out.println("COMMITMENTS-----");
        for (int i = 0; i < numberOfKeys; i++) {
            byte[][] res = NativeSecp256k1.sendShares(publicKeys, secrets[i], signers[i]);
            shares[0][i] = res[0];
            shares[1][i] = res[1];
            shares[2][i] = res[2];
            shares[3][i] = res[3];
            shares[4][i] = res[4];
            System.out.println(i + "-----------");
            for (int j = 0; j < 5; j++) {
                prnt(res[j]);
            }
        }

        System.out.println("AGGREGATE COMMITMENTS");
        FrostCache cache = new FrostCache();
        FrostSession session = new FrostSession();
        System.out.println("Send vss shares....");
        for (int i = 0; i < numberOfKeys; i++) {
            // todo combine these 2
            NativeSecp256k1.receiveFrost(shares[i], secrets[i], signers, i);
            NativeSecp256k1.send_vss_signatures(secrets[i], signers, session, cache, i);
        }

        System.out.println("Receive vss shares....");
        for (int i = 0; i < numberOfKeys; i++) {
            NativeSecp256k1.receive_vss_signatures(signers[i], session, cache);
        }
        System.out.println("Aggregate vss shares:");

        // todo combine these 3
        byte[] aggregate_vss_signatures = NativeSecp256k1.aggregate_vss_signatures(signers, session);
        byte[] aggregated_public_key = NativeSecp256k1.getAggregatedPublicKey(publicKeys, numberOfKeys);
        boolean ok = NativeSecp256k1.verifyVSS(aggregate_vss_signatures, signers[0], aggregated_public_key);

        System.out.println("VERIFICATION IS " + ok);

        System.out.println("Signing with Frost....");
        cache = new FrostCache();
        session = new FrostSession();
        String mess = "this_could_be_the_hash_of_a_msg!";

        System.out.println("Doing partial sign....");
        byte[] msg = mess.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < numberOfKeys; i++) {
            NativeSecp256k1.sign1j(secrets[i], signers[i], msg, aggregate_vss_signatures, session, cache);
        }

        System.out.println("Broadcasting partial signs...");
        for (int i = 0; i < threshold; i++) {
            NativeSecp256k1.sign2j(new int[]{1, 2, 3}, secrets[i], signers, msg, session, cache, i);
        }

        System.out.println("SA aggregating signatures....");
        byte[] frostSig = NativeSecp256k1.sign3j(aggregate_vss_signatures, signers, session);
        prnt(frostSig);

        prnt(frostSig);

        boolean good = NativeSecp256k1.frostVerify(frostSig, msg, aggregated_public_key);
        System.out.println("####### VERIFY: " + good);
        System.out.println("JAVA DONE");
        System.out.println(good);

    }

    static void prnt(byte[] arr) {
        for (byte b : arr){
            System.out.print(String.format("%02x", b) + ", ");
        }
        System.out.println();
    }

//    static {
//        System.loadLibrary("hello_cmake");
//    }
//    public native static String stringFromJNI();
//
//    static{
//        System.loadLibrary("secp256k1");
//    }
//    public native static byte[][] generateKeyPair();
//    public native static byte[][] generateKeyPair();

//    static {
//        System.loadLibrary("hello_cmake");
//    }
//    public native static String stringFromJNI();

}
