// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

object RepositoryVisibilityEnumRepositoryVisibility {
  type RepositoryVisibilityEnumRepositoryVisibility = String
  val PRIVATE: RepositoryVisibilityEnumRepositoryVisibility = "PRIVATE"
  val PUBLIC: RepositoryVisibilityEnumRepositoryVisibility = "PUBLIC"
  val ORG_SCOPED_PUBLIC: RepositoryVisibilityEnumRepositoryVisibility = "ORG_SCOPED_PUBLIC"

  def toJson(obj: RepositoryVisibilityEnumRepositoryVisibility): JString = JString(obj)

  def fromJson(v: JValue): RepositoryVisibilityEnumRepositoryVisibility = v match {
    case JString(s) => s // TODO: check if the value is valid
    case _ => throw new IllegalArgumentException(s"unknown type ${v.getClass.toString}")
  }
}
