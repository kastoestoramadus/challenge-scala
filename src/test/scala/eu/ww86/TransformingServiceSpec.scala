package eu.ww86

import cats.effect.std.Supervisor
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.unsafe.implicits.global
import cats.effect.*
import cats.implicits.*
import eu.ww86.domain.TransformTaskStatus.*
import eu.ww86.domain.*
import eu.ww86.myio.InMemoryFiles
import eu.ww86.transforming_service.TransformingService
import io.circe.Json
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import java.net.URI
import scala.concurrent.duration.*

class TransformingServiceSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers {

  import TransformingServiceSpec.*

  "TransformingService " - {
    "pass whole usage trip" in {
      Supervisor[IO](await = false).use { supervisor =>
        implicit val s: Supervisor[IO] = supervisor
        val transformingService = new TransformingService(
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

          wait <- IO.sleep(Duration(1000, MILLISECONDS))

          listedLater <- transformingService.listTasks()
          detailsOfPickedSecondO <- transformingService.getTaskDetails(secondId) // on second

          serveFile <- transformingService.serveFile(secondId) // on second
        } yield {
          listedEmpty shouldBe List()
          listedAfterCreation should contain theSameElementsAs List( //InMemoryFiles has IO.sleep; so check is valid
            firstId -> SCHEDULED,
            secondId -> SCHEDULED,
            thirdId -> SCHEDULED,
            fourthId -> SCHEDULED
          )

          detailsThirdOnStart shouldBe Some(TransformTaskDetails(thirdUri, None, TransformTaskStatus.SCHEDULED, None))

          serveNotReady shouldBe Left(())
          cancelThird shouldBe true

          detailsCanceledThirdO shouldBe Some(TransformTaskDetails(thirdUri, None, TransformTaskStatus.CANCELED, None))

          listedLater should contain theSameElementsAs List(
            firstId -> DONE,
            secondId -> DONE,
            thirdId -> CANCELED,
            fourthId -> FAILED
          )

          detailsOfPickedSecondO match {
            case Some(detailsOfPickedSecond) =>
              detailsOfPickedSecond.requestedCsv shouldBe secondUri
              detailsOfPickedSecond.state shouldBe DONE
              detailsOfPickedSecond.resultsFileName shouldBe Some(secondId.value.toString + ".json")
              detailsOfPickedSecond.processingDetails.isDefined shouldBe true
              val unpackedProcessingDetails = detailsOfPickedSecond.processingDetails.get
              unpackedProcessingDetails.linesProcessed shouldBe 3
              unpackedProcessingDetails.startedAt > 0 shouldBe true
              unpackedProcessingDetails.linesPerMinute > 0 shouldBe true
            case _ =>
              "no details returned" shouldBe "nonempty details"
          }

          serveFile match {
            case Right(stream) =>
              for( str <- stream.collect { case byte => byte.toChar }.compile.fold("")(_ + _) )
              yield { str shouldBe """{"e":"4,E","f f":"5","g":"6"}
                  |{"e":"7","f f":"8","g":":.9/"}
                  |""".stripMargin }
            case Left(_) =>
              "Returned no stream" shouldBe "Returns stream"
          }

          ()
        }
      }
    }
  }
}

object TransformingServiceSpec {
  val firstUri = new URI("https://data.wa.gov/first")
  val secondUri = new URI("https://data.wa.gov/second")
  val thirdUri = new URI("https://data.wa.gov/third")
  val fourthUri = new URI("https://data.wa.gov/fourth")

  val filesRepo: InMemoryFiles = {
    val r = new InMemoryFiles()

    r.addInputFile(firstUri,
      """a,b,c
        |1,2,3
        |""".stripMargin)
    r.addInputFile(secondUri,
      """e,f f,g
        |"4,E",5,6
        |7,8,:.9/
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