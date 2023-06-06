package eu.ww86.myio

import cats.Applicative
import cats.effect.IO
import cats.effect.kernel.Async
import eu.ww86.domain.TransformTaskId
import eu.ww86.myio.FilesService.TransformingHooks
import fs2.Stream
import io.circe.Json

import scala.collection.mutable
import java.net.URI
import scala.concurrent.duration.{Duration, MILLISECONDS}


trait FilesService {
  def transformFileFromURL(uri: URI, outputFileName: String)
                          (transformingFunction: TransformingHooks => Either[String, Unit]): IO[Either[String, Unit]]
  // use only for small files
  def getOutputFile(outputFileName: String): IO[Either[Unit, Stream[IO, Byte]]]
}

object FilesService {
  type Line = String

  class TransformingHooks(val reader: Iterator[Line], val writer: Line => Unit)
}

// not thread safe, use only for testing
class InMemoryFiles extends FilesService:
  protected val inputFiles = mutable.Map[URI, String]()
  protected val outputFiles = mutable.Map[String, String]()
  private val applicative = IO
  private val async = IO

  def transformFileFromURL(uri: URI, outputFileName: String)
                          (transformingFunction: TransformingHooks => Either[String, Unit]): IO[Either[String, Unit]] =
    // sleep to make concurrency testable in memory. Loading files from disk and network is slower.
    IO.sleep(Duration(100, MILLISECONDS)) >> applicative.pure {
      inputFiles.get(uri) match
        case Some(inputFile) =>
          val source = inputFile.split("\n").iterator
          val buffer = StringBuilder()
          transformingFunction(TransformingHooks(source, line => {
            //Thread.sleep(100) // still tests are single threaded :(
            buffer.addAll(line + "\n")
          }))
          val r = buffer.mkString
          if (r.nonEmpty) {
            outputFiles.addOne(outputFileName -> buffer.mkString)
            Right(())
          } else
            Left("Empty File?")
        case None =>
          Left("Couldn't access the file")
    }

  def getOutputFile(outputFileName: String): IO[Either[Unit, Stream[IO, Byte]]] = applicative.pure{
    outputFiles.get(outputFileName).map(str => Stream.emits(str.getBytes)) match {
      case Some(something) =>
        Right(something)
      case None =>
        Left(())
    }
  }

  def addInputFile(uri: URI, inputFile: String): IO[Unit] = applicative.pure(inputFiles.addOne(uri -> inputFile))
