package com.igormaznitsa.soundtime.bpc;

public final class BcpBitString {

  private final long bitString0;
  private final long bitString1;

  public BcpBitString(final String bitString) {
    long mask = 1L;

    long s0 = 0L;
    long s1 = 0L;

    int bitIndex = 0;

    for (int i = 0; i < bitString.length(); i++) {
      final char c = bitString.charAt(i);
      if (c == '0') {
        if (bitIndex != 0) {
          mask <<= 1;
        }
        bitIndex ^= 1;
      } else if (c == '1') {
        if (bitIndex == 0) {
          s0 |= mask;
        } else {
          s1 |= mask;
          mask <<= 1;
        }
        bitIndex ^= 1;
      }
    }

    this.bitString0 = s1;
    this.bitString1 = s0;
  }

  public boolean isEvenDiapason(final int fromPair, final int toPairInclude) {
    int notEven = 0;
    for (int i = fromPair; i <= toPairInclude; i++) {
      final int bitPair = getBitPair(i);
      if (bitPair == 1 || bitPair == 2) {
        notEven ^= 1;
      }
    }
    return notEven != 0;
  }

  public int readDiapason(final int fromPair, final int toPairInclusive) {
    int accum = 0;
    for (int i = fromPair; i <= toPairInclusive; i++) {
      accum <<= 2;
      accum |= this.getBitPair(i);
    }
    return accum;
  }

  public int getBitPair(final int index) {
    return (int) ((this.bitString0 >>> index) & 1)
        | ((int) ((this.bitString1 >>> index) & 1) << 1);
  }

}
