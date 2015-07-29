package edu.berkeley.dj.internal.arrayclazz;

/**
 * Created by matthewfl
 */
public class Byte_1 extends Base {

    private byte ir[];

    public byte load_B(int i) { return ir[i]; }

    public void set(int i, byte b) { ir[i] = b; }

    public int length() { return ir.length; }

    public Byte_1(int l) { ir = new byte[l]; }


}
