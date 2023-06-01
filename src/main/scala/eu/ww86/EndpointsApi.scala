package eu.ww86

import sttp.tapir.*
import cats.effect.IO
import eu.ww86.domain.{TransformTaskDetails, TransformTaskId, TransformTaskStatus}
import io.circe.Json
import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import java.io.File
import java.net.URI
import java.util.UUID


object EndpointsApi:

  val uriCodec: Codec[String, URI, CodecFormat.TextPlain] =
    Codec.string.mapDecode(s => DecodeResult.Value(new URI(s)))(uri => uri.toString)
  implicit val uriSchema: Schema[URI] = uriCodec.schema

  val pingEndpoint: PublicEndpoint[Unit, Unit, String, Any] = endpoint.get
    .in("ping")
    .out(stringBody)

  val createTaskEndpoint: PublicEndpoint[URI, Unit, TransformTaskId, Any] = endpoint.post
    .in("task" )
    .in(jsonBody[URI])
    .out(jsonBody[TransformTaskId])

  val getTaskEndpoint: PublicEndpoint[UUID, Unit, Option[TransformTaskDetails], Any] = endpoint.get
    .in("task" / path[UUID]("taskId"))
    .out(jsonBody[Option[TransformTaskDetails]])

  val listTaskEndpoint: PublicEndpoint[Unit, Unit, List[(TransformTaskId, TransformTaskStatus)], Any] = endpoint.get
    .in("task" )
    .out(jsonBody[List[(TransformTaskId, TransformTaskStatus)]])

  val cancelTaskEndpoint: PublicEndpoint[UUID, Unit, Boolean, Any] = endpoint.delete
    .in("task" / path[UUID]("taskId"))
    .out(jsonBody[Boolean])

  val getFileEndpoint: PublicEndpoint[UUID, Unit, Option[String], Any] = endpoint.get
    .in("result" / path[UUID]("taskId"))
    .out(jsonBody[Option[String]]) // TODO streaming
