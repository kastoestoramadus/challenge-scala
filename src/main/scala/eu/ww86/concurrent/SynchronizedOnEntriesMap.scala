package eu.ww86.concurrent

import scala.collection.concurrent

class SynchronizedOnEntriesMap[Key, State] {
  private class Lock

  private val createLock= Lock()
  private val locks: concurrent.Map[Key, Lock] = concurrent.TrieMap()
  private val states: concurrent.Map[Key, State] = concurrent.TrieMap()

  def initiateKey(key: Key, initState: State): Unit = createLock.synchronized{
    if (locks.contains(key))
      () // ignore
    else {
      locks.addOne(key -> new Lock)
      states.addOne(key -> initState)
    }
  }
  /*
    * Updated the Map with a new state calculated by the given operation.
    * @param key by which the entries are stored and synchronized, case-sensitive
    * @param update function that produces new state and result of the change
    * @return new state to a given key and the result of the change
    * @throws Exception on Illegal operations and shots to a finished game
   */
  def atomicOperation(key: Key)(update: State => (State, Boolean)): Boolean = {
      if (!locks.contains(key))
        false
      else {
        val oneLock = locks(key)

        oneLock.synchronized {
          val prevStateOrDefault = states(key)
          val pair = update(prevStateOrDefault)
          states.addOne(key -> pair._1)
          pair._2
        }
    }
  }

  def getState(key: Key): Option[State] = states.get(key)
  def getStates: List[(Key, State)] = states.toList
}
