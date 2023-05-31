package eu.ww86

import cats.effect.IO
import eu.ww86.domain.*
import eu.ww86.myio.FilesService
import cats.effect.Clock
import io.circe.Json

import java.net.URI

class TransformingService (val files: FilesService, val state: TransformationsState, timer: Clock[IO]) {
  def createTask(uri: URI): IO[TransformTaskId] = ???

  def listTasks(): IO[List[(TransformTaskId, TransformTaskStatus)]] = ???

  def getTaskDetails(id: TransformTaskId): IO[Option[TransformTaskDetails]] = ???

  def cancelTask(id: TransformTaskId): IO[Boolean] = ???

  def serveFile(id: TransformTaskId): IO[Some[Json]] = ??? // FIXME
}
