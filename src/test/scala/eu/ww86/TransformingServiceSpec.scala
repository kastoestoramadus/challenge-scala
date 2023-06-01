package eu.ww86

import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.unsafe.implicits.global
import cats.effect.{Clock, IO}
import eu.ww86.domain.{InMemoryTransformationsState, TransformTaskDetails, TransformTaskStatus}
import eu.ww86.myio.InMemoryFiles
import io.circe.Json
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import cats.implicits._
import cats.effect._
import eu.ww86.domain._
import eu.ww86.domain.TransformTaskStatus._
import java.net.URI
import scala.concurrent.duration.*

class TransformingServiceSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers {
  import TransformingServiceSpec._
  "TransformingService " - {
    "pass whole usage trip" in {
      val transformingService = new TransformingService[IO](
        filesRepo,
        new InMemoryTransformationsState
      )

      for {
        listedEmpty <- transformingService.listTasks()
        firstId <- transformingService.createTask(firstUri)
        secondId <- transformingService.createTask(secondUri)
        thirdId <- transformingService.createTask(thirdUri)
        fourthId <- transformingService.createTask(fourthUri)
        listedAfterCreation <- transformingService.listTasks()
        detailsThirdOnStart <- transformingService.getTaskDetails(thirdId) // on third
        serveNotReady <- transformingService.serveFile(thirdId) // on third
        cancelThird <- transformingService.cancelTask(thirdId) // on third
        detailsCanceledThirdO <- transformingService.getTaskDetails(thirdId) // on third

        wait <- IO.sleep(Duration(100, MILLISECONDS)) // TODO: improvement to time manipulation

        listedLater <- transformingService.listTasks()
        detailsOfPickedSecondO <- transformingService.getTaskDetails(secondId) // on second

        serveFile <- transformingService.serveFile(secondId) // on second
      } yield {
        listedEmpty shouldBe List()
        listedAfterCreation should contain theSameElementsAs List(
          firstId -> RUNNING,
          secondId -> RUNNING,
          thirdId -> SCHEDULED,
          fourthId -> FAILED
        )

        detailsThirdOnStart shouldBe Some(TransformTaskDetails(thirdUri, None, TransformTaskStatus.SCHEDULED, None))

        serveNotReady shouldBe None
        cancelThird shouldBe true

        detailsCanceledThirdO shouldBe Some(TransformTaskDetails(thirdUri, None, TransformTaskStatus.CANCELED, None))

        listedLater should contain theSameElementsAs List(
          firstId -> DONE,
          secondId -> DONE,
          thirdId -> CANCELED,
          fourthId -> FAILED
        )
        detailsOfPickedSecondO.isDefined shouldBe true

        detailsOfPickedSecondO match {
          case Some(detailsOfPickedSecond) =>
            detailsOfPickedSecond.requestedCsv shouldBe secondUri
            detailsOfPickedSecond.state shouldBe DONE
            detailsOfPickedSecond.processingDetails shouldBe None // FIXME
            detailsOfPickedSecond.resultsFileName shouldBe Some("")
        }

        serveFile shouldBe Some("")
      }
    }
  }
}

object TransformingServiceSpec {
  val firstUri = new URI("https://data.wa.gov/first")
  val secondUri = new URI("https://data.wa.gov/second")
  val thirdUri = new URI("https://data.wa.gov/third")
  val fourthUri = new URI("https://data.wa.gov/fourth")

  val filesRepo = {
    val r = new InMemoryFiles[IO]()

    r.addInputFile(firstUri,
      """a,b,c
        |1,2,3
        |""".stripMargin)
    r.addInputFile(secondUri,
      """e,f,g
        |4,5,6
        |7,8,9
        |""".stripMargin)
    r.addInputFile(thirdUri,
      """h,i,j
        |1,2,3
        |4,5,6
        |7,8,9
        |""".stripMargin)
    r
  }
}