package eu.ww86.myio

import cats.effect.IO
import cats.effect.kernel.Async
import eu.ww86.myio.FilesService.TransformingHooks
import fs2.Stream
import fs2.io.file.{Files, Path}

import java.io.{BufferedReader, File, IOException, InputStream, InputStreamReader, PrintWriter}
import java.net.URI
import java.net.URLConnection
import scala.collection.mutable
import scala.io.Source
import scala.util.Try
import scala.jdk.StreamConverters.*

class SolidMemoryFiles extends FilesService:

  protected val outputFiles = mutable.Map[String, String]()
  private val applicative = IO
  private val async = IO

  def transformFileFromURL(uri: URI, outputFileName: String)
                          (transformingFunction: TransformingHooks => Either[String, Unit]): IO[Either[String, Unit]] =
    IO.blocking { // FIXME java style code;
      // current solution fits working mechanism of limiting jobs; consequence of testing first without knowledge of all final libs
      var inputStream: InputStream = null
      var outputFile: PrintWriter = null
      def finalize() = {
        if (inputStream != null) inputStream.close()
        if (outputFile != null) outputFile.close()
      }

      val file = new File(outputFileName)
      if (file.exists()) file.delete()
      file.createNewFile()

      Try {
        inputStream = uri.toURL.openStream
        val reader = new BufferedReader(new InputStreamReader(inputStream))
        outputFile = new PrintWriter(outputFileName)
        val linesIterator = reader.lines().toScala(Iterator) // using indirectly java stream
        transformingFunction(TransformingHooks(linesIterator, line => {
          outputFile.write(line + "\n")
          ()
        }))
        finalize()
        Right(())
      }.recover {
        case e: IOException =>
          finalize()
          Left("IOException: "+e.getMessage)
        case t: Throwable =>
          finalize()
          Left("Unknown throwable: "+ t.getMessage)
        case u =>
          finalize()
          Left("Unknown: "+ u.toString)
      }.getOrElse(Left("Unknown."))
    }

  def getOutputFile(outputFileName: String): IO[Either[Unit, Stream[IO, Byte]]] = {
    val fileStream: Stream[IO, Byte] = Files[IO].readAll(Path(outputFileName))

    Async[IO].pure(Right(fileStream))
  }