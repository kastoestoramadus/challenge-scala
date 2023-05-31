package eu.ww86

import sttp.tapir.*

import cats.effect.IO
import io.circe.generic.auto.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.tapir.swagger.bundle.SwaggerInterpreter

object ServerEndpoints:
  import eu.ww86.EndpointsApi._
  val pipngServerEndpoint: ServerEndpoint[Any, IO] = pingEndpoint.serverLogicSuccess(user => IO.pure("pong"))

  val apiEndpoints: List[ServerEndpoint[Any, IO]] = List(pipngServerEndpoint)

  val docEndpoints: List[ServerEndpoint[Any, IO]] = SwaggerInterpreter()
    .fromServerEndpoints[IO](apiEndpoints, "wmi-csv-converter", "1.0.0")

  val prometheusMetrics: PrometheusMetrics[IO] = PrometheusMetrics.default[IO]()
  val metricsEndpoint: ServerEndpoint[Any, IO] = prometheusMetrics.metricsEndpoint

  val all: List[ServerEndpoint[Any, IO]] = apiEndpoints ++ docEndpoints ++ List(metricsEndpoint)
