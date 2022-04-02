package org.bitcoin;

public class FrostSigner {
  public byte[] pubkey;
  public byte[] pubnonce;
  public byte[] partial_sig;
  public byte[] vss_hash;
  public byte[][] pubcoeff;

  public FrostSigner(int THRESHOLD) {
	pubkey = new byte[64];
	pubnonce = new byte[132];
	partial_sig = new byte[36];
	vss_hash = new byte[32];
	pubcoeff = new byte[THRESHOLD][64];
  }

}
