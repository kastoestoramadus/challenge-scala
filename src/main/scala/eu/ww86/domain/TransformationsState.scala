package eu.ww86.domain

import cats.effect.{Clock, IO}

import java.net.URI

// mutable class. Access should be limited by additional wrappers.
// notes for first implementation. To be removed with solid memory version upgrade.
trait TransformationsState {
  // blockable call. One creation at the time
  def scheduleRequest(link: URI): IO[TransformTaskId]

  // not restricted call
  def listTasks(): IO[Set[(TransformTaskId, TransformTaskStatus)]]
  // not restricted call
  def getTaskDetails(id: TransformTaskId): IO[Option[TransformTaskDetails]]

  // one modification of the same task at the time
  def reportTaskCancellation(id: TransformTaskId): IO[Boolean]
  // one modification of the same task at the time
  def reportTaskProcessing(id: TransformTaskId)(linesProcessed: Int): IO[Boolean]
  // one modification of the same task at the time
  def reportTaskDone(id: TransformTaskId): IO[Boolean]
  // one modification of the same task at the time
  def reportTaskError(id: TransformTaskId, msg: String): IO[Boolean]

  // can results be fetched?
  def isFinished(id: TransformTaskId): IO[Boolean]
  // Can start or continue the processing?
  def isProcessible(id: TransformTaskId): IO[Boolean]
}

class InMemoryTransformationsState(timer: Clock[IO]) extends TransformationsState:
  def scheduleRequest(link: URI): IO[TransformTaskId] = ???

  def listTasks(): IO[Set[(TransformTaskId, TransformTaskStatus)]] = ???

  def getTaskDetails(id: TransformTaskId): IO[Option[TransformTaskDetails]] = ???

  def reportTaskCancellation(id: TransformTaskId): IO[Boolean] = ???

  def reportTaskProcessing(id: TransformTaskId)(linesProcessed: Int): IO[Boolean] = ???

  def reportTaskDone(id: TransformTaskId): IO[Boolean] = ???

  def reportTaskError(id: TransformTaskId, msg: String): IO[Boolean] = ???

  def isFinished(id: TransformTaskId): IO[Boolean] = ???

  def isProcessible(id: TransformTaskId): IO[Boolean] = ???
