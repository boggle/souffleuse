package de.jasminelli.sofleuse

import scala.actors.Actor
import scala.actors.Channel

/**
 *  Syntactic sugar for writing simple plays: Shared code between object and trait Play
 *
 **/
sealed protected trait ResponderTools {
  /**
   * Optional end-of-play-action
   */
  def endOfPlay = Responder.constant(null)

  /**
   * Produce a single value running inside the current stage/prop for
   * reuse by later scenes
   */
  def remember[T](value: T):Responder[T] = Responder.constant(value)

  /**
   * Compute a single value running inside the current stage/prop for
   * reuse by later scenes
   */
  def compute[T](thunk: => T):Responder[T] = new Responder[T] {
    def respond(k: T => Unit) = k(thunk)
  }

  /**
   * Compute a single value running inside the current stage/prop for
   * reuse by later scenes
   *
   * The computation is provided with an initial argument obj
   *
   */
  def computeWith[R, T](obj: R)(thunk: => R => T):Responder[T] = new Responder[T] {
    def respond(k: T => Unit) = k(thunk(obj))
  }

  /**
   * Compute a single value running inside the current stage/prop for
   * reuse by later scenes
   *
   * The computation is provided with the prop of stage as an initial argument
   *
   */
  def scene[R <: PropSource[P],P,T](stage: R)(thunk: => P => T) = computeWith(stage.prop)(thunk)
    

  /**
   * Runs a play's for-loop that will send it's final result values on the given channel
   *
   * The call is non-blocking
   *
   * @throws MissingResponderValueException
   **/
  def asyncRun[T](resp: Responder[T], chan: Channel[T]):Unit = {
    resp.respond { result => chan ! result }
  }

  /**
   * Runs a play's for-loop and receives a single value from it
   *
   * The call is blocking
   *
   * @throws MissingResponderValueException
   **/
  def run[T](resp: Responder[T]):T = {
    val chan = new Channel[T](Actor.self)
    asyncRun(resp, chan)
    chan.receive[T] { case value => value }
  }
}


/**
 *  Syntactic sugar for writing simple plays
 *
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

class MissingResponderValueException extends RuntimeException ;

/**
 * Syntactic sugar for writing "plays" (Sequences of acts) with a common upper bound on stage-types
 *
 * Override apply in instances to bring all the utility functions from Play's companion
 * narrowed down to the given type bound into scope
 *
 * (See companion and test code for docs)
 */
abstract trait Play[-R <: ResponsivePlayer, O] extends Function0[Responder[O]] with ResponderTools {
    def goto[V <: R](stage: V): Responder[V] = Play.goto[V](stage)

    def jump[V <: R](thunk: => V): Responder[V] = Play.goto[V] { thunk }

    def cast[V <: R](thunk: => Any): Responder[V] = Play.goto[V] { thunk.asInstanceOf[V] }

    def asyncPlay(chan: Channel[O]): Unit = Play.asyncRun[O](this(), chan)

    def play: O = Play.run[O](this())
}


trait UnitPlay[-R <: ResponsivePlayer] extends Play[R, Unit] {
    override def play: Unit = Responder.exec(this())  
}

