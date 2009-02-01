package de.jasminelli.sofleuse

trait ScenePlayer {
  def playScene(scene: this.type => Unit): Unit
}

trait SelfResponsive {
  def asResponder: Responder[this.type]
}

trait ResponsivePlayer extends ScenePlayer with SelfResponsive ;

