package eu.ww86.myio

import cats.effect.IO
import cats.effect.kernel.Async
import eu.ww86.myio.FilesService.TransformingHooks
import fs2.Stream
import fs2.io.file.{Files, Path}

import java.net.URI
import scala.collection.mutable

// TODO
class SolidMemoryFiles extends FilesService:

  protected val outputFiles = mutable.Map[String, String]()
  private val applicative = IO
  private val async = IO

  def transformFileFromURL(uri: URI, outputFileName: String)
                          (transformingFunction: TransformingHooks => Either[String, Unit]): IO[Either[String, Unit]] =
    ???

//    applicative.pure {
//      inputFiles.get(uri) match
//        case Some(inputFile) =>
//          val source = inputFile.split("\n").iterator
//          val buffer = StringBuilder()
//          transformingFunction(TransformingHooks(source, line => {
//            //Thread.sleep(100) // still tests are single threaded :(
//            buffer.addAll(line + "\n")
//          }))
//          val r = buffer.mkString
//          if (r.nonEmpty) {
//            outputFiles.addOne(outputFileName -> buffer.mkString)
//            Right(())
//          } else
//            Left("Empty File?")
//        case None =>
//          Left("Couldn't access the file")
//  }

  def getOutputFile(outputFileName: String): IO[Either[Unit, Stream[IO, Byte]]] = {
    val fileStream: Stream[IO, Byte] = Files[IO].readAll(Path(outputFileName))

    Async[IO].pure(Right(fileStream))
    }