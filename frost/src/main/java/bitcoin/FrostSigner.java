package bitcoin;
import java.util.Arrays;

public class FrostSigner implements Comparable<FrostSigner>{
  public byte[] pubkey;
  public byte[] pubnonce;
  public byte[] partial_sig;
  public byte[] vss_hash;
  public byte[][] pubcoeff;
  public String ip;

  public FrostSigner(int THRESHOLD) {
	pubkey = new byte[64];
	pubnonce = new byte[132];
	partial_sig = new byte[36];
	vss_hash = new byte[32];
	pubcoeff = new byte[THRESHOLD][64];
  }

    public FrostSigner(byte[] pubkey, byte[] pubnonce, byte[] partial_sig, byte[] vss_hash, byte[][] pubcoeff) {
        this.pubkey = pubkey;
        this.pubnonce = pubnonce;
        this.partial_sig = partial_sig;
        this.vss_hash = vss_hash;
        this.pubcoeff = pubcoeff;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FrostSigner that = (FrostSigner) o;

        if (!Arrays.equals(pubkey, that.pubkey)) return false;
        if (!Arrays.equals(pubnonce, that.pubnonce)) return false;
        if (!Arrays.equals(partial_sig, that.partial_sig)) return false;
        if (!Arrays.equals(vss_hash, that.vss_hash)) return false;
        return Arrays.deepEquals(pubcoeff, that.pubcoeff);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(pubkey);
        result = 31 * result + Arrays.hashCode(pubnonce);
        result = 31 * result + Arrays.hashCode(partial_sig);
        result = 31 * result + Arrays.hashCode(vss_hash);
        result = 31 * result + Arrays.deepHashCode(pubcoeff);
        return result;
    }

    @Override
    public int compareTo(FrostSigner o) {
        return ip.compareTo(o.ip);
    }
}
