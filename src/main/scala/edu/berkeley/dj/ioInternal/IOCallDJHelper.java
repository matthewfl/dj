package edu.berkeley.dj.ioInternal;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Created by matthewfl
 */
public class IOCallDJHelper {

    private IOCallDJHelper() {}

    // take the parameters and proxy them to the DJ types and call into the DJ class
    public Object call(Object self, String method, String[] argTypes, Object[] args) {
        throw new NotImplementedException();
    }
}
