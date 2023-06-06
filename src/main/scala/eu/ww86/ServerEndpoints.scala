package eu.ww86

import sttp.tapir.*
import cats.effect.IO
import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import domain.*
import eu.ww86.EndpointsApi.pingEndpoint
import eu.ww86.service.TransformingService
import sttp.capabilities.fs2.Fs2Streams

class ServerEndpoints(service: TransformingService):

  import eu.ww86.EndpointsApi._

  val createTaskServerEndpoint: ServerEndpoint[Any, IO] = createTaskEndpoint.serverLogicSuccess(service.createTask)
  val getTaskServerEndpoint: ServerEndpoint[Any, IO] = getTaskEndpoint.serverLogicSuccess(uuid =>
    service.getTaskDetails(TransformTaskId(uuid)))
  val listTaskServerEndpoint: ServerEndpoint[Any, IO] = listTaskEndpoint.serverLogicSuccess(_ => service.listTasks())
  val cancelTaskServerEndpoint: ServerEndpoint[Any, IO] = cancelTaskEndpoint.serverLogicSuccess(uuid =>
    service.cancelTask(TransformTaskId(uuid)))
  val getFileServerEndpoint: ServerEndpoint[Any with Fs2Streams[IO], IO] = getFileEndpoint.serverLogic(uuid =>
    service.serveFile(TransformTaskId(uuid)))

  val apiEndpoints: List[ServerEndpoint[Any with Fs2Streams[IO], IO]] = List(
    createTaskServerEndpoint,
    getTaskServerEndpoint,
    listTaskServerEndpoint,
    cancelTaskServerEndpoint,
    getFileServerEndpoint,
    ServerEndpoints.pingServerEndpoint)

  val docEndpoints: List[ServerEndpoint[Any, IO]] = SwaggerInterpreter()
    .fromServerEndpoints[IO](apiEndpoints, "wmi-csv-converter", "1.0.0")

  val prometheusMetrics: PrometheusMetrics[IO] = PrometheusMetrics.default[IO]()
  val metricsEndpoint: ServerEndpoint[Any, IO] = prometheusMetrics.metricsEndpoint

  val all: List[ServerEndpoint[Any with Fs2Streams[IO], IO]] = apiEndpoints ++ docEndpoints ++ List(metricsEndpoint)

object ServerEndpoints {
  val pingServerEndpoint: ServerEndpoint[Any, IO] = pingEndpoint.serverLogicSuccess(user => IO.pure("pong"))

}