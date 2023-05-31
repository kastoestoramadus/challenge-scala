package eu.ww86

import sttp.tapir.*
import cats.effect.IO
import eu.ww86.domain.{TransformTaskDetails, TransformTaskId}
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

  val createTask: PublicEndpoint[URI, Unit, TransformTaskId, Any] = endpoint.post
    .in("task" )
    .in(jsonBody[URI])
    .out(jsonBody[TransformTaskId])

  val getTaskEndpoint: PublicEndpoint[UUID, Unit, TransformTaskDetails, Any] = endpoint.get
    .in("task" / path[UUID]("taskId"))
    .out(jsonBody[TransformTaskDetails])

  val listTaskEndpoint: PublicEndpoint[Unit, Unit, Seq[TransformTaskDetails], Any] = endpoint.get
    .in("task" )
    .out(jsonBody[Seq[TransformTaskDetails]])

  val cancelTaskEndpoint: PublicEndpoint[UUID, Unit, String, Any] = endpoint.delete
    .in("task" / path[UUID]("taskId"))
    .out(stringBody)

  val getFileEndpoint: PublicEndpoint[UUID, Unit, Json, Any] = endpoint.get
    .in("result" / path[UUID]("taskId"))
    .out(jsonBody[Json]) // TODO streaming
