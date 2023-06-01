package eu.ww86.myio

import cats.Applicative
import eu.ww86.domain.TransformTaskId
import eu.ww86.myio.FilesService.TransformingHooks
import io.circe.Json

import scala.collection.mutable
import java.net.URI


trait FilesService[F[_]] {
  def transformFileFromURL(uri: URI, outputFileName: String)
                          (transformingFunction: TransformingHooks => Unit): F[Boolean]
  def getOutputFile(outputFileName: String): F[Option[String]]
}

object FilesService {
  type Line = String
  class TransformingHooks(val reader: Iterator[Line], val writer: Line => Unit)
}

// not thread safe, use only for testing
class InMemoryFiles[F[_]: Applicative] extends FilesService[F]:
  protected val inputFiles = mutable.Map[URI, String]()
  protected val outputFiles = mutable.Map[String, String]()
  private val applicative = implicitly[Applicative[F]]

  def transformFileFromURL(uri: URI, outputFileName: String)
                          (transformingFunction: TransformingHooks => Unit): F[Boolean] = applicative.pure{
    inputFiles.get(uri) match
      case Some(inputFile) =>
        val source = inputFile.split("\n").iterator
        val buffer = StringBuilder()
        transformingFunction(TransformingHooks(source, line => buffer.addAll(line)))
        val r = buffer.mkString
        if(r.nonEmpty){
          outputFiles.addOne(outputFileName -> buffer.mkString)
          true
        } else
          false
      case None => false
  }

  def getOutputFile(outputFileName: String): F[Option[String]] = applicative.pure(outputFiles.get(outputFileName))

  def addInputFile(uri: URI, inputFile: String): F[Unit] = applicative.pure(inputFiles.addOne(uri -> inputFile))
