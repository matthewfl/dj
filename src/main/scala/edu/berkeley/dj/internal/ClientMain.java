package edu.berkeley.dj.internal;

/**
 * Created by matthewfl
 */
public class ClientMain {

    static public void prestart(Object ii) {
        try {
            InternalInterface._setIsClient();
            InternalInterface.setInternalInterface(ii);
            InternalInterface.debug("Client main has started");
            InternalInterface.getInternalInterface().registerClient();
        } catch(Exception e) {
            System.err.println("Failed to properly start client");
            e.printStackTrace();
            throw e;
        }
    }
}
