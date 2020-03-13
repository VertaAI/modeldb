// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

object PathLocationTypeEnumPathLocationType {
  type PathLocationTypeEnumPathLocationType = String
  val LOCAL_FILE_SYSTEM: PathLocationTypeEnumPathLocationType = "LOCAL_FILE_SYSTEM"
  val NETWORK_FILE_SYSTEM: PathLocationTypeEnumPathLocationType = "NETWORK_FILE_SYSTEM"
  val HADOOP_FILE_SYSTEM: PathLocationTypeEnumPathLocationType = "HADOOP_FILE_SYSTEM"
  val S3_FILE_SYSTEM: PathLocationTypeEnumPathLocationType = "S3_FILE_SYSTEM"

  def toJson(obj: PathLocationTypeEnumPathLocationType): JString = JString(obj)

  def fromJson(v: JValue): PathLocationTypeEnumPathLocationType = v match {
    case JString(s) => s // TODO: check if the value is valid
    case _ => throw new IllegalArgumentException(s"unknown type ${v.getClass.toString}")
  }
}
