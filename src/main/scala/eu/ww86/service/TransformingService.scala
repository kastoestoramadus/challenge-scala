package eu.ww86.service

import cats.Applicative
import cats.effect.kernel.{Async, Concurrent}
import cats.effect.std.Supervisor
import cats.effect.{Clock, IO}
import eu.ww86.domain.*
import eu.ww86.myio.FilesService
import eu.ww86.myio.FilesService.TransformingHooks
import eu.ww86.service.TransformingService.makeFileName
import io.circe.Json

import java.net.URI
import scala.annotation.tailrec
import scala.collection.mutable
import scala.concurrent.duration.{Duration, MILLISECONDS}
import fs2.io.file.{Files, Path}
import fs2.Stream

class TransformingService(val files: FilesService, val state: TransformationsState)
                         (implicit supervisor: Supervisor[IO]) {
  private val applicative = IO

  import TransformingService.makeFileName

  private val quickStates = new TransformingService.PerformantMetrics
  private val backgroundRoutines = new BackgroundRoutines(files, quickStates, state)
  
  def createTask(uri: URI): IO[TransformTaskId] = {
    val r = state.scheduleRequest(uri)
    for {
      fib <- supervisor.supervise( backgroundRoutines.consumeUntilEmptyWithLimit())
      r <- applicative.pure(r)
    } yield r
  }

  def listTasks(): IO[List[(TransformTaskId, TransformTaskStatus)]] = applicative.pure(state.listTasks())

  def getTaskDetails(id: TransformTaskId): IO[Option[TransformTaskDetails]] =
    applicative.pure {
      state.getTask(id).map { withHistory =>
        TransformTaskDetails(
          withHistory.requestedCsv,
          for {
            startedAt <- withHistory.processingStartedAt
            finishedAt <- withHistory.endedAt
            linesProcessed <- quickStates.getProgress(id)
          } yield {
            val startAtSeconds = startedAt.toSeconds
            val finishedAtSeconds = finishedAt.toSeconds
            val processingSpeed = linesProcessed / ((finishedAtSeconds - startAtSeconds) / 60.0)
            ProcessingDetails(startAtSeconds, linesProcessed, processingSpeed)
          },
          withHistory.state,
          if (withHistory.state == TransformTaskStatus.DONE)
            Some(makeFileName(id))
          else None
        )
      }
    }

  def cancelTask(id: TransformTaskId): IO[Boolean] = {
    applicative.pure {
      quickStates.reportTaskCancellation(id)
      state.reportTaskCancellation(id) // risk: not atomic
    }
  }

  def serveFile(id: TransformTaskId): IO[Either[Unit, Stream[IO, Byte]]] = {
    if (state.isDone(id))
      files.getOutputFile(makeFileName(id))
    else
      applicative.pure(Left(()))
  }
}

object TransformingService {
  val suffix = ".json"
  val reportEachNLine = 300

  private[service] def makeFileName(id: TransformTaskId) = id.value.toString + suffix

  protected[service] class PerformantMetrics {
    // FIXME, memory leak, add ~few minutes retention ; could report final processing speed to long lived states
    private val progresses = mutable.Map[TransformTaskId, Int]()
    private val canceled = mutable.Set[TransformTaskId]()

    def getProgress(id: TransformTaskId): Option[Int] = progresses.get(id)

    def reportTaskProcessingProgress(id: TransformTaskId)(lines: Int): Unit = progresses += id -> lines

    def wasRecentlyCanceled(id: TransformTaskId): Boolean = canceled.contains(id)

    def reportTaskCancellation(id: TransformTaskId): Unit = canceled += id
  }
  
}

