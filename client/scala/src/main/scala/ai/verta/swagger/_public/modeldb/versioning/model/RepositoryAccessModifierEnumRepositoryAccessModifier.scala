// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

object RepositoryAccessModifierEnumRepositoryAccessModifier {
  type RepositoryAccessModifierEnumRepositoryAccessModifier = String
  val UNKNOWN: RepositoryAccessModifierEnumRepositoryAccessModifier = "UNKNOWN"
  val REGULAR: RepositoryAccessModifierEnumRepositoryAccessModifier = "REGULAR"
  val PROTECTED: RepositoryAccessModifierEnumRepositoryAccessModifier = "PROTECTED"

  def toJson(obj: RepositoryAccessModifierEnumRepositoryAccessModifier): JString = JString(obj)

  def fromJson(v: JValue): RepositoryAccessModifierEnumRepositoryAccessModifier = v match {
    case JString(s) => s // TODO: check if the value is valid
    case _ => throw new IllegalArgumentException(s"unknown type ${v.getClass.toString}")
  }
}
