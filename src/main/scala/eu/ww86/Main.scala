package eu.ww86

import cats.effect.std.Supervisor
import cats.effect.{ExitCode, IO, IOApp}
import com.comcast.ip4s.{Host, Port, port}
import eu.ww86.domain.InMemoryTransformationsState
import eu.ww86.myio.{InMemoryFiles, SolidMemoryFiles}
import eu.ww86.transforming_service.TransformingService
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}

object Main extends IOApp:

  override def run(args: List[String]): IO[ExitCode] =

    Supervisor[IO](await = false).use { supervisor =>
      implicit val s: Supervisor[IO] = supervisor
      val serverEndpoints = new ServerEndpoints(new TransformingService(
        new SolidMemoryFiles,
        new InMemoryTransformationsState
      ))

      val serverOptions: Http4sServerOptions[IO] =
        Http4sServerOptions
          .customiseInterceptors[IO]
          .metricsInterceptor(serverEndpoints.prometheusMetrics.metricsInterceptor())
          .options
      val routes = Http4sServerInterpreter[IO](serverOptions).toRoutes(serverEndpoints.all)

      val port = sys.env
        .get("HTTP_PORT")
        .flatMap(_.toIntOption)
        .flatMap(Port.fromInt)
        .getOrElse(port"8080")

      EmberServerBuilder
        .default[IO]
        .withHost(Host.fromString("localhost").get)
        .withPort(port)
        .withHttpApp(Router("/" -> routes).orNotFound)
        .build
        .use { server =>
          for {
            _ <- IO.println(s"Go to http://localhost:${server.address.getPort}/docs to open SwaggerUI. Press ENTER key to exit.")
            _ <- IO.readLine
          } yield ()
        }
        .as(ExitCode.Success)
    }
