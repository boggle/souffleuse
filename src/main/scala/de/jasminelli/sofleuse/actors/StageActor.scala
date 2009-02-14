package de.jasminelli.sofleuse.actors

import de.jasminelli.sofleuse.core._

import scala.actors.Actor
import scala.actors.Actor.loopWhile

/**
 * Default implementation trait for an Actor that is a ResponsivePlayer
 *
 * @see Stage
 *
 * @author Stefan Plantikow <Stefan.Plantikow@googlemail.com>
 */
trait StageActor extends LoopingActor with ResponsivePlayer {
  self: StageActor =>

  final type Scene = this.type => Unit

  /**
   * Main loop of this actor, receives messages and matches them with matcher
   */
  override protected def mainLoop =  receive(matcher)


  /**
   * Matcher used by mainLoop's receive
   *
   * By default dispatches scenes to onScene and everything else to onMessage
   */
  protected val matcher: PartialFunction[Any, Unit] = {
        case (scene: (Any => Unit)) => performScene(scene)
        case msg: Any => onMessage(msg)
  }


  /**
   * Executes given scene with this actor as input. Calls onSceneFailure if an exception
   * is thrown during execution of the scene.
   *
   * Normally not overriden by user-code
   *
   * @see performScene
   * @see onSceneFailure
   */
  def performScene(scene: Scene): Unit = {
    assert(self == Actor.self, "onScene called from outside of actor")
    try { onScene(scene) }
    catch {
      case h: StageActor.HandledSceneException => ()
      case t: Throwable => onSceneFailure(t)
    }
  }


  /***
   * Inner, actual execution of a single scene.  If you want to wrap around scene execution,
   * override this one.
   */
  protected def onScene(scene: Scene): Unit = scene(this)

  
  /**
   * Called by matcher for every non-scene message
   *
   * By default calls onUnknownMessage
   *
   * @see matcher
   * @see onUnknownMessage
   */
  def onMessage(msg: Any):Unit = {
    assert(self == Actor.self, "onMessage called from outside of actor")
    onUnknownMessage(msg)
  }


  /**
   * @throws UnknownMessageException
   */
  protected def onUnknownMessage(msg: Any):Unit = throw new StageActor.UnknownMessageException


  /**
   * Called by onScene when an exception is thrown during scene execution
   *
   * @see onScene
   */
  protected def onSceneFailure(t: Throwable) = throw t


  /**
   * Send's the given continuation to this actor's main loop where it
   * is executed with the actor as input. This can be used to schedule lambdas
   * explicitely to actors/threads in a for-loop (cont passing style dsl for
   * writing request handling logic over multiple stages in a single piece of
   * code)
   *
   * Implementations cannot override this.  Instead they may only override
   * playLocalScene for different behaviour when submitting scenes from inside the actor.
   *
   * @see Play
   */
  final def playScene(scene: Scene): Unit =
    if (this == Actor.self) playLocalScene(scene) else playRemoteScene(scene)


  /**
   * Default implementation calls playRemoteScene(scene)
   *
   * @see playScene
   */
  protected def playLocalScene(scene: Scene): Unit = playRemoteScene(scene)


  /**
   * Sends scene to self for execution
   *
   * When overriding, be very careful when communicating with remote actors
   * since StageActor.playRemoteScene might not work anymore
   *
   * @see playScene
   */
  protected final def playRemoteScene(scene: Scene): Unit =
    StageActor.sendScene[this.type](self, scene)


  /**
   * Call to notify the stage that request processing is done.  Allows a premature end of
   * scene executing.
   */
  def finishScene = throw new StageActor.HandledSceneException
}


/**
 * Companion that holds different utility classes/traits
 *
 * @see StageActor
 *
 * @author Stefan Plantikow <Stefan.Plantikow@googlemail.com>
 */
object StageActor {

  /**
   * Turns an arbitrary (potentially remote) actor into a StageActor-subtype responder
   *
   * Please consider that the result is not memoized while you might want to do so
   */
  def remoteStageActor[V <: StageActor](actor: Actor): Responder[V] = new Responder[V] {
    def respond(k: V => Unit): Unit = sendScene(actor, k)
  }


  def sendScene[V <: StageActor](actor: Actor, k: V => Unit) = actor ! k

  /**
   * Thrown by StageActor to indicate that a message was not understood
   *
   * @author Stefan Plantikow <Stefan.Plantikow@googlemail.com>
   */
  class UnknownMessageException extends IllegalArgumentException ;

  
  /**
   * StageActors that want to use react instead of receive should include this trait
   *
   * @author Stefan Plantikow <Stefan.Plantikow@googlemail.com>
   */
  trait Reacting {
    this: StageActor =>

    protected def mainLoop = react(matcher)
  }

  
  /**
   * Used to signal a premature end of scene execution
   *
   * @see StageActor.finishScene
   *
   * @author Stefan Plantikow <Stefan.Plantikow@googlemail.com>
   */
  class HandledSceneException extends RuntimeException ;
}
