package eu.ww86

import cats.effect.testing.scalatest.AsyncIOSpec
import eu.ww86.myio.FilesService.Line
import eu.ww86.myio.SolidMemoryFiles
import org.scalatest.Assertion
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import java.net.URI

// dependant on internet access
class FileServiceItTest extends AsyncFreeSpec with AsyncIOSpec with Matchers {

  "File service" - {
    " opens a network file" in {
      val csvFile = new URI("""https://media.githubusercontent.com/media/datablist/sample-csv-files/main/files/organizations/organizations-100.csv""")
      val service = new SolidMemoryFiles
      val outputTestFile = "testTmp.txt"
      for {
        fileCreation <- primitiveTransforLogic(csvFile, service)
        fileCheck <- service.getOutputFile(outputTestFile)
      } yield {
        fileCreation shouldBe Right(())
        val r: Assertion = fileCheck match {
          case Right(stream) =>
            val str = stream.collect { case byte => byte.toChar }.compile.fold("")(_ + _).unsafeRunSync()
            str shouldBe
              """Index,Organization Id,Name,Website,Country,Description,Founded,Industry,Number of employeesIndex,Organization Id,Name,Website,Country,Description,Founded,Industry,Number of employees
                |""".stripMargin

          case Left(_) =>
            "File not exists" shouldBe "File created"
        }
        r
      }
    }
    " fail on fake a network file" in {
      val fakePath = new URI("""https://media.githubusercontent.com/media/organizations-100.csv""")
      val service = new SolidMemoryFiles

      for {
        fileCreation <- primitiveTransforLogic(fakePath, service)
      } yield {
        fileCreation.isLeft shouldBe true
      }
    }
  }

  private def primitiveTransforLogic(uri: URI, service: SolidMemoryFiles) = {

    service.transformFileFromURL(uri, "testTmp.txt") { pair =>
      val inputLinesIterator: Iterator[Line] = pair.reader
      val lineWriter: Line => Unit = pair.writer
      val oneLine = inputLinesIterator.next()
      lineWriter(oneLine + oneLine)
      Right(())
    }

  }
}
