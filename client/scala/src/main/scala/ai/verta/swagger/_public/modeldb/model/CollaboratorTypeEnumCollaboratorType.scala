// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

object CollaboratorTypeEnumCollaboratorType {
  type CollaboratorTypeEnumCollaboratorType = String
  val READ_ONLY: CollaboratorTypeEnumCollaboratorType = "READ_ONLY"
  val READ_WRITE: CollaboratorTypeEnumCollaboratorType = "READ_WRITE"

  def toJson(obj: CollaboratorTypeEnumCollaboratorType): JString = JString(obj)

  def fromJson(v: JValue): CollaboratorTypeEnumCollaboratorType = v match {
    case JString(s) => s // TODO: check if the value is valid
    case _ => throw new IllegalArgumentException(s"unknown type ${v.getClass.toString}")
  }
}
