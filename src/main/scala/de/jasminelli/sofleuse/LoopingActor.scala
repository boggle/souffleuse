package de.jasminelli.sofleuse

import scala.actors.Actor
import scala.actors.Actor.loopWhile

/**
 * Support for simple actors that consist of a single react/receive loop with apropriate hooks
 * for startup and shutdown.
 *
 */
trait LoopingActor extends Actor {

  // running state of main loop
  private var _running = false

  def isRunning: Boolean = this._running

  /**
   * Calls onStartActing, the mainLoop, and finally onStopActing
   *
   * @see onStartActing
   * @see onStopActing
   */
  def act(): Unit = {
    onStartActing
    loopWhile(isRunning) {
      mainLoop
      if (!isRunning) onStopActing
    }
  }


  /**
   * Does nothing.  Override in subtypes.
   *
   */
  protected def mainLoop: Unit = ()


  /**
   * Called by main loop during actor startup, before receiving messages
   */
  protected def onStartActing = ()


  /**
   * Called by main loop just before it finishes
   */
  protected def onStopActing = ()


  /**
   * Sets running state in start to avoid a race conditing between starting code and the main loop
   */
  override def start: Actor = {
    _running = true
    try { super.start }
    catch { case t: Throwable => _running = false; throw t }
  }


  /**
   * Shutdown this actor after finishing the current scene
   */

  def shutdownAfterScene: Unit = _running = false
}