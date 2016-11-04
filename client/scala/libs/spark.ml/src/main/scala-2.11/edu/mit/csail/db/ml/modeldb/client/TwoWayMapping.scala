package edu.mit.csail.db.ml.modeldb.client

import scala.collection.mutable

/**
  * This data structure combines two HashMaps to maintain an association
  * between pairs of objects. We use this in the ModelDbSyncer.
  *
  * The mapping goes from objects of type K to objects of type V,
  * and vice versa.
  */
class TwoWayMapping[K, V] {

  // We store the mappings in two hashmaps.
  private val kToV = mutable.HashMap[K, V]()
  private val vToK = mutable.HashMap[V, K]()

  // Unfortunately, we need to name these functions differently.
  // If we don't, the Scala compiler complains that the functions
  // are ambiguous because of type erasure.

  // Put a mapping into the two maps.
  def putKV(k: K, v: V): Unit = {
    kToV.put(k, v)
    vToK.put(v, k)
  }
  def putVK(v: V, k: K): Unit = putKV(k, v)

  // Retrieve the corresponding value for a given key.
  def getK(k: K): Option[V] = kToV.get(k)
  def getV(v: V): Option[K] = vToK.get(v)

  // Check if the key is contained in the mapping.
  def hasK(k: K): Boolean = kToV.contains(k)
  def hasV(v: V): Boolean = vToK.contains(v)
  def clear(): Unit = {
    vToK.clear()
    kToV.clear()
  }
}
