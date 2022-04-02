package org.bitcoin;

public class FrostSecret {
  public byte[] keypair;
  public byte[] agg_share;
  public byte[] secnonce;

  public FrostSecret() {
	keypair = new byte[96];
	agg_share = new byte[32];
	secnonce = new byte[68];
  }

}
