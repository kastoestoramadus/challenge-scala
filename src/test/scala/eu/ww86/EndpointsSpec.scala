package eu.ww86

import eu.ww86.ServerEndpoints.{*, given}
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.client3.testing.SttpBackendStub
import sttp.client3.{UriContext, basicRequest}
import sttp.tapir.server.stub.TapirStubInterpreter

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.circe.generic.auto.*
import sttp.client3.circe.*
import sttp.tapir.integ.cats.effect.CatsMonadError

class EndpointsSpec extends AnyFlatSpec with Matchers with EitherValues:

  it should "return hello message" in {
    // given
    val backendStub = TapirStubInterpreter(SttpBackendStub(new CatsMonadError[IO]()))
      .whenServerEndpointRunLogic(pipngServerEndpoint)
      .backend()

    // when
    val response = basicRequest
      .get(uri"http://test.com/ping")
      .send(backendStub)

    // then
    response.map(_.body.value shouldBe "pong").unwrap
  }


  extension [T](t: IO[T]) def unwrap: T = t.unsafeRunSync()
