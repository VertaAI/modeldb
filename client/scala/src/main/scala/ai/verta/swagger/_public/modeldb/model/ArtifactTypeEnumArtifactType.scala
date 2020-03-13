// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

object ArtifactTypeEnumArtifactType {
  type ArtifactTypeEnumArtifactType = String
  val IMAGE: ArtifactTypeEnumArtifactType = "IMAGE"
  val MODEL: ArtifactTypeEnumArtifactType = "MODEL"
  val TENSORBOARD: ArtifactTypeEnumArtifactType = "TENSORBOARD"
  val DATA: ArtifactTypeEnumArtifactType = "DATA"
  val BLOB: ArtifactTypeEnumArtifactType = "BLOB"
  val STRING: ArtifactTypeEnumArtifactType = "STRING"
  val CODE: ArtifactTypeEnumArtifactType = "CODE"

  def toJson(obj: ArtifactTypeEnumArtifactType): JString = JString(obj)

  def fromJson(v: JValue): ArtifactTypeEnumArtifactType = v match {
    case JString(s) => s // TODO: check if the value is valid
    case _ => throw new IllegalArgumentException(s"unknown type ${v.getClass.toString}")
  }
}
