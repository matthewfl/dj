package edu.berkeley.dj.internal;

/**
 * Created by matthewfl
 */
public class ClientMain {

    static public void prestart(Object ii) {
        InternalInterface._setIsClient();
        InternalInterface.setInternalInterface(ii);
        InternalInterface.debug("Client main has started");
        InternalInterface.getInternalInterface().registerClient();
    }
}