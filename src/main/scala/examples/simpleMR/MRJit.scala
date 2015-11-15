package examples.simpleMR

import edu.berkeley.dj.internal.{JITCommands, StackRepresentation}
import edu.berkeley.dj.jit.JITInterface

/**
 * Created by matthewfl
 */
class MRJit extends JITInterface{

  override def newClient(id: Int) {}

  // the MR JIT doesn't care about remotes and reads and writes atm
  override def recordRemoteRead(self: AnyRef, from_machine: Int, to_machine: Int, field_id: Int, stack: StackRepresentation): Unit = {
    //JITCommands.moveObject(self, from_machine)
  }

  override def recordRemoteWrite(self: AnyRef, from_machine: Int, to_machine: Int, field_id: Int, stack: StackRepresentation): Unit = {
    //JITCommands.moveObject(self, from_machine)
  }

  override def recordRemoteRPC(self: AnyRef, from_machine: Int, to_machine: Int, stack: StackRepresentation): Unit = {}

  override def placeThread(self: AnyRef, from_machine: Int, stack: StackRepresentation): Int = {
    from_machine
  }


  override def scheduleQueuedWork(self: AnyRef, from_machine: Int, stack: StackRepresentation) = {
//    if(self.isInstanceOf[MapTask]) {
//      JITCommands.runQueuedWork(self, self.asInstanceOf[MapTask].getTargetMachine)
//    } else {
//      JITCommands.runQueuedWork(self, from_machine)
//    }
    JITCommands.runQueuedWork(self, from_machine)
  }

  override def recordReceiveRemoteRead(self: AnyRef, from_machine: Int, to_machine: Int, field_id: Int) = {}

  override def recordReceiveRemoteWrite(self: AnyRef, from_machine: Int, to_machine: Int, field_id: Int) = {}
}
