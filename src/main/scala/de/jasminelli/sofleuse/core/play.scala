package de.jasminelli.sofleuse

import scala.actors.Actor
import scala.actors.Channel

/**
 *  Syntactic sugar for writing simple plays: Shared code between object and trait Play
 *
 * @author Stefan Plantikow <Stefan.Plantikow@googlemail.com>
 **/
sealed protected trait ResponderTools {
  /**
   * Optional end-of-play-responder (produces infinite stream of null)
   */
  def endOfPlay = Responder.constant(null)

  /**
   * Produce a single value for reuse by later scenes
   *
   * (runs inside the current stage actor)
   */
  def remember[T](value: T):Responder[T] = Responder.constant(value)

  /**
   * Compute a single value running for reuse by later scenes
   *
   * (runs inside the current stage)
   */
  def compute[T](thunk: => T):Responder[T] = new Responder[T] {
    def respond(k: T => Unit) = k(thunk)
  }

  /**
   * Compute a single value for reuse by later scenes. The computation is provided with an initial
   * argument obj.
   *
   * (runs inside the current stage actor)
   */
  def computeWith[R, T](obj: R)(thunk: => R => T):Responder[T] = new Responder[T] {
    def respond(k: T => Unit) = k(thunk(obj))
  }

  /**
   * Compute a single for reuse by later scenes.  The computation is provided with the prop of
   * a (likely the current) stage as an initial argument
   *
   * (runs inside the current stage actor)
   */
  def scene[R <: PropSource[P],P,T](stage: R)(thunk: => P => T) = computeWith(stage.prop)(thunk)
    

  /**
   * Runs a play's for-loop over multiple stage actors and finally sends the result value(s)
   * back on the given channel
   *
   * The call is non-blocking
   **/
  def asyncRun[T](resp: Responder[T], chan: Channel[T]):Unit = {
    resp.respond { result => chan ! result }
  }

  /**
   * Runs a play's for-loop over multiple stage actors and waits until it finally receives a single v
   * result value from the play
   *
   * The call is blocking
   **/
  def run[T](resp: Responder[T]):T = {
    val chan = new Channel[T](Actor.self)
    asyncRun(resp, chan)
    chan.receive[T] { case value => value }
  }
}


/**
 * Syntactic sugar for writing simple plays
 *
 * @author Stefan Plantikow <Stefan.Plantikow@googlemail.com>
 **/
object Play extends ResponderTools {
    /**
     * Continue with execution of next scene at given stage
     */
    def goto[R <: ResponsivePlayer](stage: R):Responder[R] = stage.asResponder


    /**
     * Continue with execution of next scene at stage computed by given thunk
     */
    def jump[R <: ResponsivePlayer](thunk: => R):Responder[R] = goto[R] { thunk }

      // doenst work: compute[R] { thunk }

    /**
     * Continue with execution of next scene at stage computed by given thunk
     * (typecasts thunk result)
     */
    def cast[R <: ResponsivePlayer](thunk: => Any):Responder[R] = goto[R] { thunk.asInstanceOf[R] }

      // doesnt work compute[R] { thunk.asInstanceOf[R] }
}

/**
 * Syntactic sugar for writing "plays" (Sequences of scene) with a common upper bound on the type
 * of responsive players (resp. stage actor or stage)
 *
 * Override apply in instances to bring all the utility functions from Play's companion
 * narrowed down to the given type bound into scope
 *
 * (See companion and test code for docs)
 *
 * @author Stefan Plantikow <Stefan.Plantikow@googlemail.com>
 */
abstract trait Play[-R <: ResponsivePlayer, O] extends Function0[Responder[O]] with ResponderTools {
    def goto[V <: R](stage: V): Responder[V] = Play.goto[V](stage)

    def jump[V <: R](thunk: => V): Responder[V] = Play.goto[V] { thunk }

    def cast[V <: R](thunk: => Any): Responder[V] = Play.goto[V] { thunk.asInstanceOf[V] }

    def asyncPlay(chan: Channel[O]): Unit = Play.asyncRun[O](this(), chan)

    def play: O = Play.run[O](this())
}


/**
 * Play that doesn not produce a result value
 *
 * @author Stefan Plantikow <Stefan.Plantikow@googlemail.com>
 */
trait AsyncUnitPlay[-R <: ResponsivePlayer] extends Play[R, Unit] {
  /**
   * Variant of super.play that is non-blocking
   */
    override def play: Unit = Responder.exec(this())
}

