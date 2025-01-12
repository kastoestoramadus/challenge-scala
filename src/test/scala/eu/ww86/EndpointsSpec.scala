package eu.ww86

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import eu.ww86.ServerEndpoints
import eu.ww86.domain.InMemoryTransformationsState
import eu.ww86.myio.InMemoryFiles
import io.circe.generic.auto.*
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.client3.circe.*
import sttp.client3.testing.SttpBackendStub
import sttp.client3.{UriContext, basicRequest}
import sttp.tapir.integ.cats.effect.CatsMonadError
import sttp.tapir.server.stub.TapirStubInterpreter

// some properties are covered by compile and strong typing of Tapir
class EndpointsSpec extends AnyFlatSpec with Matchers with EitherValues:

  it should "return hello message" in {
    // given
    val backendStub = TapirStubInterpreter(SttpBackendStub(new CatsMonadError[IO]()))
      .whenServerEndpointRunLogic(ServerEndpoints.pingServerEndpoint)
      .backend()

    // when
    val response = basicRequest
      .get(uri"http://test.com/ping")
      .send(backendStub)

    // then
    response.map(_.body.value shouldBe "pong").unwrap
  }


  extension [T](t: IO[T]) def unwrap: T = t.unsafeRunSync()
