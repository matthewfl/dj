package edu.berkeley.dj.rt

/**
 * Created by matthewfl
 */
case class Config (val uuid: String="ID"+java.util.UUID.randomUUID.toString.replace("-",""),
                   val debug_clazz_bytecode: String = null) {

  def fieldPrefix = "__dj_" //+ uuid

  final val internalPrefix = "edu.berkeley.dj.internal."

  final val proxyClassPrefix = internalPrefix + "proxyclazz."


}
