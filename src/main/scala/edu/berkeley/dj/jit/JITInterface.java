package edu.berkeley.dj.jit;

import edu.berkeley.dj.internal.StackRepresentation;

/**
 * Created by matthewfl
 */
public interface JITInterface {

    // Constructor is call on the main node before the main method is called


    // a new client has been created
    // this is called on the main node
    // the id can be used with the distributed running to run some code on a targeted machine
    void newClient(int id);

    // TODO: need to have some stack representation
    // TODO: would be nice to determine what object invoke some operation, eg capture the this variable from the scope before something happened

    // These are called on the machine that invoked the remote operation
    // the self object can be inspected to see what state this object is now in
    // eg where it is located, or attempt to find out what else it references
    void recordRemoteRead(Object self, int from_machine, int to_machine, int field_id, StackRepresentation stack);

    void recordRemoteWrite(Object self, int from_machine, int to_machine, int field_id, StackRepresentation stack);

    void recordRemoteRPC(Object self, int from_machine, int to_machine, StackRepresentation stack);


    // these are recorded on the machine that currently owns the object self,
    // this will some policies easier to implement such as always move an object to the
    // machine that performed the last read
    void recordReceiveRemoteRead(Object self, int from_machine, int to_machine, int field_id);

    void recordReceiveRemoteWrite(Object self, int from_machine, int to_machine, int field_id);



    // called during Thread.start from the machine that is starting the thread
    // self is the runnable object that was passed to the thread to start
    // a trivial implementation would just return from_machine
    int placeThread(Object self, int from_machine, StackRepresentation stack);

    // when something submits work into a work queue like system
    // eg using .par on a scala collection
    void scheduleQueuedWork(Object self, int from_machine, StackRepresentation stack);
}

