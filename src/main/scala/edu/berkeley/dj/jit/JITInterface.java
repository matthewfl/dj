package edu.berkeley.dj.jit;

/**
 * Created by matthewfl
 */
public interface JITInterface {

    // Constructor is call on the main node before the main method is called


    // a new client has been created
    // this is called on the main node
    // the id can be used with the distributed running to run some code on a targeted machine
    void newClient(int id);


}
