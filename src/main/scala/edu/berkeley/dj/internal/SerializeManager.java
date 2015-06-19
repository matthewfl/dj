package edu.berkeley.dj.internal;

/**
 * Created by matthewfl
 *
 * This class represent that an object or a set of objects
 * are getting serialize or deserialized
 *
 *
 */
public class SerializeManager {

    public int depth_left;

    public void enter() { depth_left--; }

    public void leave() { depth_left++; }

    public boolean shouldStub() {
        return depth_left <= 0;
    }


    // TODO: putter and setter values for primitves
    // maybe make these final so that the jvm can know to inline
    public void put_value_Z(boolean v) {}

    public void put_value_C(char v) {}

    public void put_value_B(byte v) {}

    public void put_value_S(short v) {}

    public void put_value_I(int v) {}

    public void put_value_J(long v) {}

    public void put_value_F(float v) {}

    public void put_value_D(double v) {}

    public boolean get_value_Z() {return false;}

    public char get_value_C() { return 'C'; }

    public byte get_value_B() { return 0; }

    public short get_value_S() { return 0; }

    public int get_value_I() { return 0; }

    public long get_value_J() { return 0; }

    public float get_value_F() { return 0; }

    public double get_value_D() { return 0; }
}
