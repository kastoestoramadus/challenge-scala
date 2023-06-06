package eu.ww86.domain

import cats.FlatMap.ops.toAllFlatMapOps
import cats.effect.{Clock, IO}
import eu.ww86.concurrent.SynchronizedOnEntriesMap
import eu.ww86.domain.TransformTaskHistory

import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

// mutable class. Planned for solid memory. Should be used for performant access.
// notes for first implementation.
trait TransformationsState {
  // blockable call. One creation at the time
  def scheduleRequest(link: URI): TransformTaskId

  // not restricted call
  def listTasks(): List[(TransformTaskId, TransformTaskStatus)]
  // not restricted call
  def getTask(id: TransformTaskId): Option[TransformTaskHistory]

  // one modification of the same task at the time
  def reportTaskCancellation(id: TransformTaskId): Boolean
  // one modification of the same task at the time
  def reportTaskProcessing(id: TransformTaskId): Boolean
  // one modification of the same task at the time
  def reportTaskDone(id: TransformTaskId): Boolean
  // one modification of the same task at the time
  def reportTaskError(id: TransformTaskId, msg: String): Boolean

  // can results be fetched?
  def isDone(id: TransformTaskId): Boolean = getTask(id).exists(_.state == TransformTaskStatus.DONE)
  // Can start or continue the processing?
  def isProcessable(id: TransformTaskId): Boolean = getTask(id).exists(el => el.state == TransformTaskStatus.SCHEDULED)
}
// TODO: logging
class InMemoryTransformationsState() extends TransformationsState {

  import TransformTaskStatus._

  private val states = new SynchronizedOnEntriesMap[TransformTaskId, TransformTaskHistory]()

  private def now() = FiniteDuration(System.currentTimeMillis(), TimeUnit.MILLISECONDS)

  def scheduleRequest(link: URI): TransformTaskId = {
    val newKey = TransformTaskId(UUID.randomUUID())
    states.initiateKey(newKey, TransformTaskHistory(link,
      SCHEDULED,
      now(), // FIXME: Static clock, Clock typeclass? or Calendar
      None, None))
    newKey
  }

  def listTasks(): List[(TransformTaskId, TransformTaskStatus)] = states.getStates.map(pair => pair._1 -> pair._2.state)

  def getTask(id: TransformTaskId): Option[TransformTaskHistory] = states.getState(id)

  def reportTaskCancellation(id: TransformTaskId): Boolean = states.atomicOperation(id) { oldState =>
    if (oldState.state == SCHEDULED || oldState.state == RUNNING)
      oldState.copy(state = CANCELED) -> true
    else
      oldState -> false
  }

  def reportTaskProcessing(id: TransformTaskId): Boolean = states.atomicOperation(id) { oldState =>
    if (oldState.state == SCHEDULED)
      oldState.copy(state = RUNNING, processingStartedAt = Some(now())) -> true
    else
      oldState -> false 
  }

  def reportTaskDone(id: TransformTaskId): Boolean = states.atomicOperation(id) { oldState =>
    if (oldState.state != RUNNING)
      oldState -> false
    else
      oldState.copy(state = DONE, endedAt = Some(now())) -> true
  }

  def reportTaskError(id: TransformTaskId, msg: String): Boolean = states.atomicOperation(id) { oldState =>
    if (oldState.state == RUNNING || oldState.state == SCHEDULED)
      oldState.copy(state = FAILED, endedAt = Some(now())) -> true
    else
      oldState -> false
  }
}
