// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

object JobTypeEnumJobType {
  type JobTypeEnumJobType = String
  val KUBERNETES_JOB: JobTypeEnumJobType = "KUBERNETES_JOB"

  def toJson(obj: JobTypeEnumJobType): JString = JString(obj)

  def fromJson(v: JValue): JobTypeEnumJobType = v match {
    case JString(s) => s // TODO: check if the value is valid
    case _ => throw new IllegalArgumentException(s"unknown type ${v.getClass.toString}")
  }
}
