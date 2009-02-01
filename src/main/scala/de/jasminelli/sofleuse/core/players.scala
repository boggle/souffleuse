package de.jasminelli.sofleuse

/**
 * ScenePlayer executes (plays) functions that require the ScenePlayer as input argument
 *
 * @author Stefan Plantikow <Stefan.Plantikow@googlemail.com>
 */
trait ScenePlayer {
  // Note: We cant simply extend Function1[this.type, Unit] since this.type is not available
  // in trait or class declaration heads

  def playScene(scene: this.type => Unit): Unit
}


/**
 * Implementors are convertible into a responder over themselves
 *
 * @author Stefan Plantikow <Stefan.Plantikow@googlemail.com>
 */
trait SelfResponsive {
  def asResponder: Responder[this.type]
}


/**
 * Shared top-level trait of sofluese stages/actors
 *
 * @see Play
 * 
 * @author Stefan Plantikow <Stefan.Plantikow@googlemail.com>
 */
trait ResponsivePlayer extends ScenePlayer with SelfResponsive {
  self: ResponsivePlayer =>
  
  object responder extends Responder[this.type] {
    def respond(k: ResponsivePlayer.this.type => Unit): Unit = self.playScene(k)
  }

  def asResponder: Responder[this.type] = responder
}

