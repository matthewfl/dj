package edu.berkeley.dj.rt

/**
 * Created by matthewfl
 */
case class Config (val uuid: String="DJ"+java.util.UUID.randomUUID.toString.replace("-",""),
                   val debug_clazz_bytecode: String = null,
                   val cluster_code: String = "dj-default",
                   val cluster_conn_mode: String = "dummy") {

  def fieldPrefix = "__dj_" //+ uuid

  final val internalPrefix = "edu.berkeley.dj.internal."

  // we can not load classes in the java.* javax.* namespaces
  // but we still have to rewrite thei members for our new system
  // so we change all references to those classes
  // and then reload our modified classes in this new namespace
  final val coreprefix = internalPrefix + "coreclazz."

  final val proxysubclasses = internalPrefix + "proxysubclazz."

}