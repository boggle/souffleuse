package de.jasminelli.souffleuse.actors

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
trait SceneCapturing extends StageActor {
  final type SceneHook = Scene => Unit

  var sceneHook: SceneHook = null

  private var _deferredScene: Scene = null
  private var _currentScene: Scene = null


  /**
   * Returns the current scene continuation
   *
   * While it may seem futile to get acces to the already executing continuation,
   * it may be useful for routing requests based on stage state.
   */
  final def currentScene: Scene = _currentScene


  /**
   * Registers newHook to be called with the next continuation scene that is to be played
   * at this actor
   *
   * Overwrites any previously registered hook
   */
  final def onNextScene(newHook: => SceneHook): this.type = {
    sceneHook = newHook
    this
  }


  /**
   * Calls super.playScene unless a continuation hook was registered and this == Actor.self.
   * In this case, remembers the scene as continuation argument for the hook and does nothing.
   * The hook will then be called later with the cached continuation by an upper stack frame of
   * onScene.
   *
   * @see onScene
   */
  override def playLocalScene(scene: this.type => Unit): Unit =
    /* register scene for hook */
    if (sceneHook != null) _deferredScene = scene

  
  /**
   * Like StageActor.onScene but otionally calls a registered onNextScene-hook instead if
   * one was registered earlier and a cached continuation is available for the hook
   *
   * @see Play
   */
  override def performScene(scene: this.type => Unit): Unit = {
    val oldScene = _currentScene
    try {
      if (sceneHook == null) {
        _currentScene = scene
        super.performScene(scene)
      }
      else
        /* next scene arrived from outside */
        _deferredScene = scene

      while (sceneHook != null && _deferredScene != null) {
        val sceneHookCopy = sceneHook
        val sceneCopy = _deferredScene
        sceneHook = null
        _deferredScene = null

        try { sceneHookCopy(sceneCopy) }
        catch { case t: Throwable => onSceneFailure(t) }
      }
    }
    finally { _currentScene = oldScene }
  }

  /**
   * Ensures actor shutdown will be called *after* the next scene has been executed
   */
  def shutdownAfterNextScene: this.type = onNextScene {
    try { onScene(_) }
    finally { shutdownAfterScene }
  }
}


