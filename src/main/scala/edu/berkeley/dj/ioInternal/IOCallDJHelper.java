package edu.berkeley.dj.ioInternal;

/**
 * Created by matthewfl
 */
public class IOCallDJHelper {

    private IOCallDJHelper() {}

    // take the parameters and proxy them to the DJ types and call into the DJ class
    public static Object call(Object self, String method, String[] argTypes, Object[] args) {
        return IOInternalInterface.getIOInternalInterface().call(self, method, argTypes, args);
//        throw new NotImplementedException();
    }
}
