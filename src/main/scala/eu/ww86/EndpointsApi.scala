package eu.ww86

import cats.effect.IO
import eu.ww86.domain.{TransformTaskDetails, TransformTaskId, TransformTaskStatus}
import fs2.Stream
import io.circe.Json
import io.circe.generic.auto.*
import sttp.capabilities.fs2.Fs2Streams
import sttp.model.{Header, MediaType}
import sttp.tapir.*
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
    .in("task")
    .in(jsonBody[URI].example(new URI(
      "https://media.githubusercontent.com/media/datablist/sample-csv-files/main/files/organizations/organizations-100.csv"
    )))
    .out(jsonBody[TransformTaskId])

  val getTaskEndpoint: PublicEndpoint[UUID, Unit, Option[TransformTaskDetails], Any] = endpoint.get
    .in("task" / path[UUID]("taskId"))
    .out(jsonBody[Option[TransformTaskDetails]])

  val listTaskEndpoint: PublicEndpoint[Unit, Unit, List[(TransformTaskId, TransformTaskStatus)], Any] = endpoint.get
    .in("task")
    .out(jsonBody[List[(TransformTaskId, TransformTaskStatus)]])

  val cancelTaskEndpoint: PublicEndpoint[UUID, Unit, Boolean, Any] = endpoint.delete
    .in("task" / path[UUID]("taskId"))
    .out(jsonBody[Boolean])

  val getFileEndpoint: PublicEndpoint[UUID, Unit, Stream[IO, Byte], Any with Fs2Streams[IO]] = endpoint.get
    .in("result" / path[UUID]("taskId"))
    .out(streamBody(Fs2Streams[IO])(Schema.schemaForFile, CodecFormat.TextPlain()))
