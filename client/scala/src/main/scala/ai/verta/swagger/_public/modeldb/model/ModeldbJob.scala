// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.JobStatusEnumJobStatus._
import ai.verta.swagger._public.modeldb.model.JobTypeEnumJobType._
import ai.verta.swagger._public.modeldb.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.modeldb.model.ProtobufNullValue._
import ai.verta.swagger.client.objects._

case class ModeldbJob (
  id: Option[String] = None,
  description: Option[String] = None,
  start_time: Option[String] = None,
  end_time: Option[String] = None,
  metadata: Option[List[CommonKeyValue]] = None,
  job_status: Option[JobStatusEnumJobStatus] = None,
  job_type: Option[JobTypeEnumJobType] = None,
  owner: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbJob.toJson(this)
}

object ModeldbJob {
  def toJson(obj: ModeldbJob): JObject = {
    new JObject(
      List[Option[JField]](
        obj.id.map(x => JField("id", JString(x))),
        obj.description.map(x => JField("description", JString(x))),
        obj.start_time.map(x => JField("start_time", JString(x))),
        obj.end_time.map(x => JField("end_time", JString(x))),
        obj.metadata.map(x => JField("metadata", ((x: List[CommonKeyValue]) => JArray(x.map(((x: CommonKeyValue) => CommonKeyValue.toJson(x)))))(x))),
        obj.job_status.map(x => JField("job_status", ((x: JobStatusEnumJobStatus) => JobStatusEnumJobStatus.toJson(x))(x))),
        obj.job_type.map(x => JField("job_type", ((x: JobTypeEnumJobType) => JobTypeEnumJobType.toJson(x))(x))),
        obj.owner.map(x => JField("owner", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbJob =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbJob(
          // TODO: handle required
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString),
          description = fieldsMap.get("description").map(JsonConverter.fromJsonString),
          start_time = fieldsMap.get("start_time").map(JsonConverter.fromJsonString),
          end_time = fieldsMap.get("end_time").map(JsonConverter.fromJsonString),
          metadata = fieldsMap.get("metadata").map((x: JValue) => x match {case JArray(elements) => elements.map(CommonKeyValue.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          job_status = fieldsMap.get("job_status").map(JobStatusEnumJobStatus.fromJson),
          job_type = fieldsMap.get("job_type").map(JobTypeEnumJobType.fromJson),
          owner = fieldsMap.get("owner").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
