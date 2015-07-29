package edu.berkeley.dj.internal.arrayclazz;

/**
 * Created by matthewfl
 */
public class Byte_2 extends Base {

    private Byte_1 ir[];

    public Byte_2(int a, int b) {
        ir = new Byte_1[a];
        for(int i = 0; i < ir.length; i++) {
            ir[i] = new Byte_1(b);
        }
    }

    public void store(int i, Byte_1 v) {
        ir[i] = v;
    }

    public Byte_1 load(int i) {
        return ir[i];
    }

    public int length() {
        return ir.length;    }

}
