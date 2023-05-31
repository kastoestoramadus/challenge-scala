package eu.ww86

import cats.effect.{Clock, IO}
import cats.effect.testing.scalatest.AsyncIOSpec
import eu.ww86.domain.{InMemoryTransformationsState, TransformTaskDetails, TransformTaskStatus}
import eu.ww86.myio.InMemoryFiles
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import cats.effect.unsafe.implicits.global
import io.circe.Json

import java.net.URI

class TransformingServiceSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers {
  "TransformingService " - {
    "pass whole usage trip" in {
      val timer: Clock[IO] = ???
      val transformingService = new TransformingService(
        new InMemoryFiles,
        new InMemoryTransformationsState(timer),
        timer
      )
      val thirdUri = new URI("https://data.wa.gov/third")
      val secondUri = new URI("https://data.wa.gov/second")

      for {
        listedEmpty <- transformingService.listTasks()
        firstId <- transformingService.createTask(new URI("https://data.wa.gov/first"))
        secondId <- transformingService.createTask(secondUri)
        thirdId <- transformingService.createTask(thirdUri)
        listedAfterCreation <- transformingService.listTasks()
        detailsThirdOnStart <- transformingService.getTaskDetails(thirdId) // on third
        serveNotReady <- transformingService.serveFile(thirdId) // on third
        cancelThird <- transformingService.cancelTask(thirdId) // on third
        detailsCanceledThird <- transformingService.getTaskDetails(thirdId) // on third

        // somehow change clock

        listedLater <- transformingService.listTasks()
        detailsOfPickedSecond <- transformingService.getTaskDetails(secondId) // on second

        serveFile <- transformingService.serveFile(secondId) // on second
      } yield {
        listedEmpty shouldBe List()
        listedAfterCreation shouldBe List(???)

        detailsThirdOnStart shouldBe Some(TransformTaskDetails(thirdUri, None, TransformTaskStatus.SCHEDULED, None))

        serveNotReady shouldBe None
        cancelThird shouldBe true

        detailsCanceledThird shouldBe Some(TransformTaskDetails(thirdUri, None, TransformTaskStatus.CANCELED, None))

        listedLater shouldBe List(???)
        detailsOfPickedSecond shouldBe Some(TransformTaskDetails(secondUri, Some(???), TransformTaskStatus.DONE, Some(???)))

        serveFile shouldBe Json.False
      }
    }
  }
}
