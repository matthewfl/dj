package edu.berkeley.dj.internal;

/**
 * Created by matthewfl
 */
public class RPCHelpers {

    private RPCHelpers() {}

    public static boolean checkPerformRPC(String id) {
        return false;
    }

    public static Object call_A(Object self, String name, String[] params, Object[] args) { return null; }

    public static boolean call_Z(Object self, String name, String[] params, Object[] args) { return false;}

    public static char call_C(Object self, String name, String[] params, Object[] args) { return ' ';}

    public static byte call_B(Object self, String name, String[] params, Object[] args) { return (byte)0; }

    public static short call_S(Object self, String name, String[] params, Object[] args) { return 0;}

    public static int call_I(Object self, String name, String[] params, Object[] args) { return 0; }

    public static long call_J(Object self, String name, String[] params, Object[] args) { return 0l; }

    public static float call_F(Object self, String name, String[] params, Object[] args) { return 0.0f;}

    public static double call_D(Object self, String name, String[] params, Object[] args) {return 0.0;}

    public static void call_V(Object self, String name, String[] params, Object[] args) { }



}
