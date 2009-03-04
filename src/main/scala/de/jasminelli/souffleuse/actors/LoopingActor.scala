package de.jasminelli.souffleuse.actors

import scala.actors.Actor

/**
 * Support for simple actors that consist of a single react/receive loop with apropriate hooks
 * for startup and shutdown.
 *
 * @author Stefan Plantikow <Stefan.Plantikow@googlemail.com>
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
    mainLoop
    if (!isRunning) onStopActing
  }


 /**
   * Main loop of this actor, receives messages and matches them with matcher
   */
  protected def mainLoop: Unit = while (isRunning) mainLoopBody

  protected def mainLoopBody: Unit


  /**
   * Called by main loop during actor startup, before receiving messages
   */
  protected def onStartActing = ()


  /**
   * Called by main loop just before it finishes
   */
  protected def onStopActing = exit


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