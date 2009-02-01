package de.jasminelli.sofleuse.actors

import scala.actors.Actor

/**
 * Adds the ability to capture the next scene continuation that will be executed by this actor.
 * The capured continuation is handed to a previously-registering hook.  This allows to get access
 * to the "rest-of-the-current-play" in a StageActor (aka weird form of "deferred" call-cc).
 *
 * The implementation avoids building up a big stack by ensuring that the "Hook-registering"
 * stackframe has been left before activating the hook.
 *
 * <b>EXPERIMENTAL</b>
 *
 * @see StageActor
 *
 *
 * @author Stefan Plantikow <Stefan.Plantikow@googlemail.com>
 */
trait ContCapturingActor extends StageActor {
  final type ContHook = Scene => Unit

  private var contHook: ContHook = null
  private var cont: Scene = null

  def onNextCont(newHook: => ContHook): this.type = { contHook = newHook; this }

  /**
   * Calls super.playScene unless a continuation hook was registered and this == Actor.self.
   * In this case, remembers the scene as continuation argument for the hook and does nothing.
   * The hook will then be called later with the cached continuation by an upper stack frame of
   * onScene.
   *
   * @see onScene
   */
  override def playScene(scene: this.type => Unit): Unit =
    if (this == Actor.self && contHook != null)
      cont = scene
    else
      super.playScene(scene)

  /**
   * Like StageActor.onScene but otionally calls a registered onNextContinuation-hook instead if
   * one was registered earlier and a cached continuation is available for the hook
   *
   * @see Play
   */
  override def performScene(scene: this.type => Unit): Unit = {
      super.performScene(scene)

      if (contHook != null && cont != null) {
        val contHookCopy = contHook
        val contCopy = cont
        contHook = null
        cont = null

        try { contHookCopy(contCopy) }
        catch { case t: Throwable => onSceneFailure(t) }
    }
  }

  /**
   * Ensures actor shutdown will be called *after* the next scene has been executed
   */
  def shutdownAfterNextScene: this.type = onNextCont {
    try { onScene(_) }
    finally { shutdownAfterScene }
  }
}


