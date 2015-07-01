package edu.berkeley.dj.jit;

/**
 * Created by matthewfl
 */
public interface JITInterface {

    // a new client has been created
    // we can then use messages to request additional information from it
    void newClient(int id);


}
