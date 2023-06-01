package eu.ww86

import cats.Applicative
import cats.effect.IO
import eu.ww86.domain.*
import eu.ww86.myio.FilesService
import cats.effect.Clock
import cats.effect.kernel.Concurrent
import io.circe.Json
import cats.effect.std.Supervisor

import java.net.URI
import scala.collection.mutable

class TransformingService[F[_]: Concurrent: Applicative](val files: FilesService[F], val state: TransformationsState) {
  private val applicative = implicitly[Applicative[F]]
  import TransformingService.makeFileName
  protected val reportEachNLine = 300

  val quickStates = new TransformingService.PerformantMetrics

  // line by line processing; TODO too long
  protected def transformRoutine(uri: URI, id: TransformTaskId): F[Boolean] = {
    files.transformFileFromURL(uri, makeFileName(id)) { pair =>
      val inputLinesIterator = pair.reader
      val lineWriter = pair.writer

      if(inputLinesIterator.hasNext && !quickStates.wasRecentlyCanceled(id)) {
        state.reportTaskProcessing(id)
        val headersLine = inputLinesIterator.next()
        CsvToJsonUtil.createFromFirstLineHeaders(headersLine) match {
          case Some(toJson) =>
            var linesProcessed = 1
            val reportProgress = quickStates.reportTaskProcessingProgress(id)
            while inputLinesIterator.hasNext
            do {
              val csvRow = inputLinesIterator.next()
              linesProcessed += 1
              if(linesProcessed % reportEachNLine == 0)
                reportProgress(linesProcessed)
              toJson.process(csvRow) match {
                case Some(json) =>
                  lineWriter(json.noSpaces) // line added to output
                case _ => // ignore, logging?
              }
            }
          case _ =>
            state.reportTaskError(id, "Headers not in csv format.") // break
        }
      } else
        state.reportTaskError(id, "File empty?") // break
    }
  }
  // TODO limiting to two + make them work until there are Scheduled events!
  def createTask(uri: URI): F[TransformTaskId] =
    Supervisor[F](await = true).use { supervisor =>
      val r = state.scheduleRequest(uri)
      supervisor.supervise( transformRoutine(uri, r))

      applicative.pure(r)
    }


  def listTasks(): F[List[(TransformTaskId, TransformTaskStatus)]] = applicative.pure(state.listTasks())

  def getTaskDetails(id: TransformTaskId): F[Option[TransformTaskDetails]] =
    applicative.pure {
      state.getTask(id).map{ withHistory =>
        TransformTaskDetails(
          withHistory.requestedCsv,
          for {
            startedAt <- withHistory.processingStartedAt
            finishedAt <- withHistory.endedAt
            linesProcessed <- quickStates.getProgress(id)
          } yield {
            val startAtSeconds = startedAt.toSeconds
            val finishedAtSeconds = finishedAt.toSeconds
            val processingSpeed = linesProcessed/((finishedAtSeconds - startAtSeconds)/60.0)
            ProcessingDetails(startAtSeconds, linesProcessed, processingSpeed)
          },
          withHistory.state,
          if(withHistory.state == TransformTaskStatus.DONE)
            Some(makeFileName(id))
          else None
        )
      }
    }

  def cancelTask(id: TransformTaskId): F[Boolean] = {
    applicative.pure {
      quickStates.reportTaskCancellation(id)
      state.reportTaskCancellation(id) // risk: not atomic
    }
  }

  def serveFile(id: TransformTaskId): F[Option[String]] = {
      if(state.isDone(id))
        files.getOutputFile(makeFileName(id))
      else
        applicative.pure(None)
    }
}

object TransformingService {
  val suffix = ".json"

  private[TransformingService] def makeFileName(id: TransformTaskId) = id.value.toString + suffix

  protected[TransformingService] class PerformantMetrics {
    // FIXME, memory leak, add ~few minutes retention ; could report final processing speed to long lived states
    private val progresses = mutable.Map[TransformTaskId, Int]()
    private val canceled = mutable.Set[TransformTaskId]()

    def getProgress(id: TransformTaskId): Option[Int] = progresses.get(id)
    def reportTaskProcessingProgress(id: TransformTaskId)(lines: Int): Unit = progresses += id -> lines
    def wasRecentlyCanceled(id: TransformTaskId): Boolean = canceled.contains(id)
    def reportTaskCancellation(id: TransformTaskId): Unit = canceled += id
  }
}

