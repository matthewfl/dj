package edu.berkeley.dj.rt

/**
 * Created by matthewfl
 */
class RunningInterface (private val config : Config) {

  override def toString = "RunningInterface (" + config.uuid + ")"

  def getUUID = config.uuid

}
