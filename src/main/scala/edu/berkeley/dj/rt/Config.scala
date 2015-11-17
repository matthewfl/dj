package edu.berkeley.dj.rt

/**
 * Created by matthewfl
 */
case class Config (val uuid: String="DJ"+java.util.UUID.randomUUID.toString.replace("-",""),
                   val debug_clazz_bytecode: String = null,
                   val cluster_code: String = "dj-default",
                   val cluster_conn_mode: String = "dummy",
                   val distributed_jit: String = "edu.berkeley.dj.jit.SimpleJIT") {

  def fieldPrefix = "__dj_" //+ uuid

  final val internalPrefix = "edu.berkeley.dj.internal."

  final val ioInternalPrefix = "edu.berkeley.dj.ioInternal."

  // we can not load classes in the java.* javax.* namespaces
  // but we still have to rewrite thei members for our new system
  // so we change all references to those classes
  // and then reload our modified classes in this new namespace
  final val coreprefix = internalPrefix + "coreclazz."

  final val arrayprefix = internalPrefix + "arrayclazz."

  final val proxysubclasses = internalPrefix + "proxysubclazz."

  final val staticInitMethodName = fieldPrefix + "static_clinit"

}
