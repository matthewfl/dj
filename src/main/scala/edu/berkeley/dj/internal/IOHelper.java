package edu.berkeley.dj.internal;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Created by matthewfl
 */
public class IOHelper {

    public static int constructLocalIO(int target, String clsname, String[] argsTyp, Object[] args) {
        // return a int id for this object on the target machine
        if(InternalInterface.getInternalInterface().getSelfId() == target) {
            // then we are going to perform this here
            return InternalInterface.getInternalInterface().createIOObject(clsname, argsTyp, args);
        }

        throw new NotImplementedException();
//        return -1;
    }


    public static Object callMethod(int target, int id, String methodName, String[] argsTyp, Object[] args) {
        if(InternalInterface.getInternalInterface().getSelfId() == target) {
            return InternalInterface.getInternalInterface().callIOMethod(id, methodName, argsTyp, args);
        }

        throw new NotImplementedException();

//        return null;
    }
}
