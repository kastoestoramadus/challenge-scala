package eu.ww86.transforming_service

import cats.Applicative
import cats.effect.kernel.{Async, Concurrent}
import cats.effect.std.Supervisor
import cats.effect.{Clock, IO}
import eu.ww86.domain.*
import eu.ww86.myio.FilesService
import eu.ww86.myio.FilesService.TransformingHooks
import io.circe.Json
import org.slf4j.LoggerFactory

import java.net.URI
import scala.annotation.tailrec
import scala.collection.mutable
import scala.concurrent.duration.{Duration, MILLISECONDS}

// could be an inner class, moved out for cleaner style
private[transforming_service] class BackgroundRoutines(files: FilesService, quickStates: TransformingService.PerformantMetrics, state: TransformationsState) {
  import TransformingService.*
  private val logger = LoggerFactory.getLogger(classOf[BackgroundRoutines])

  val ioThreadsLimit = 2

  private val createParallelLock = new Object
  private var ioThreadsCount = 0 // defective solution? is releasing guarantied?

  def consumeUntilEmptyWithLimit(): IO[Boolean] =
    createParallelLock.synchronized{
      if(ioThreadsCount >= ioThreadsLimit) IO.pure(true)
      else {
        ioThreadsCount += 1
        consumeUntilEmpty()
      }
    }

  def consumeUntilEmpty(): IO[Boolean] = {
    def processIfNotEmpty() = state.listTasks().find(_._2 == TransformTaskStatus.SCHEDULED).flatMap(p =>
      state.getTask(p._1).map(history => p._1 -> history.requestedCsv)) match {
      case Some(id, uri) =>
        handleOneTask(uri, id) >> IO.sleep(Duration(1, MILLISECONDS)) >> consumeUntilEmpty()
      case None =>
        createParallelLock.synchronized{ ioThreadsCount -= 1 }
        IO.pure(true)
    }

    processIfNotEmpty()
  }

  def handleOneTask(uri: URI, id: TransformTaskId): IO[Boolean] =
    files.transformFileFromURL(uri, makeFileName(id)) {
      transformRoutine(uri, id)
    }.map {
        case Right(()) =>
          state.reportTaskDone(id)
          logger.info(s"Job completed successfully for uri/id $uri/$id")
          true
        case Left(str) =>
          state.reportTaskError(id, str)
          logger.warn(s"Job has failed with message: $str for uri/id $uri/$id")
          false
    }
  
  protected def transformRoutine(uri: URI, id: TransformTaskId): TransformingHooks => Either[String, Unit] = {
    pair =>
      val inputLinesIterator = pair.reader
      val lineWriter = pair.writer

      if (inputLinesIterator.hasNext) {
        state.reportTaskProcessing(id)
        val headersLine = inputLinesIterator.next()
        CsvToJsonUtil.createFromFirstLineHeaders(headersLine) match {
          case Some(toJson) =>
            val reportProgress = quickStates.reportTaskProcessingProgress(id)
            reportProgress(1)

            @tailrec
            def consumeNextValuesLine(linesProcessed: Int, linesSkipped: Int): Unit = {
              if (inputLinesIterator.hasNext && !quickStates.wasRecentlyCanceled(id)) {
                val csvRow = inputLinesIterator.next()
                if (linesProcessed % reportEachNLine == 0)
                  reportProgress(linesProcessed)
                toJson.process(csvRow) match {
                  case Some(json) =>
                    lineWriter(json.noSpaces) // line added to output
                    consumeNextValuesLine(linesProcessed + 1, linesSkipped)
                  case _ =>
                    consumeNextValuesLine(linesProcessed + 1, linesSkipped + 1)
                  // ignore, logging?
                }
              } else {
                // After processing of all
                reportProgress(linesProcessed)
                logger.warn(s"Skipped $linesSkipped lines for uri/id $uri/$id")
              }
            }
            Right(consumeNextValuesLine(1, 0))
          case _ =>
            Left("Headers not in csv format.") // break
        }
      } else
        Left("File empty?") // break
  }
}