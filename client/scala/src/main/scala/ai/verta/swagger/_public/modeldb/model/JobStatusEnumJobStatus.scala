// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

object JobStatusEnumJobStatus {
  type JobStatusEnumJobStatus = String
  val NOT_STARTED: JobStatusEnumJobStatus = "NOT_STARTED"
  val IN_PROGRESS: JobStatusEnumJobStatus = "IN_PROGRESS"
  val COMPLETED: JobStatusEnumJobStatus = "COMPLETED"

  def toJson(obj: JobStatusEnumJobStatus): JString = JString(obj)

  def fromJson(v: JValue): JobStatusEnumJobStatus = v match {
    case JString(s) => s // TODO: check if the value is valid
    case _ => throw new IllegalArgumentException(s"unknown type ${v.getClass.toString}")
  }
}
