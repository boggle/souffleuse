package de.jasminelli.sofleuse.stage

import de.jasminelli.sofleuse.actors._

import scala.actors.Actor
import scala.actors.Actor._


/**
 * A Stage is an actor that executes "substeps (scene = lambda) in workflows (plays = for-loops)"
 * over its associated prop (the state and behaviour encapsulated by this actor)
 *
 * @author Stefan Plantikow <Stefan.Plantikow@googlemail.com>
 *
 * Originally created by User: stepn Date: 16.01.2009 Time: 23:45:03
 */
trait Stage[+P] extends ContCapturingActor with PropSource[P] ;


/**
 * Source for a stage
 *
 * A stage is a refined implementation of StageActor that encapsulates all of its behaviour
 * in a single (potentially exchangeable) object, its prop.
 *
 * @see Stage
 * @see StageActor
 *
 * @author Stefan Plantikow <Stefan.Plantikow@googlemail.com>
 */
trait StageSource[+H <: Stage[_]] { def stage: H }


/**
 * Stage drain
 *
 * @see StageSource
 *
 * @author Stefan Plantikow <Stefan.Plantikow@googlemail.com>
 */
trait StageDrain[-H <: Stage[_]] { def stage_=(newStage: H): Unit }


/**
 * @see StageSource
 * @see StageDrain
 *
 * @author Stefan Plantikow <Stefan.Plantikow@googlemail.com>
 */
trait StageCell[H <: Stage[_]] extends StageSource[H] with StageDrain[H] ;

/**
 * Implements StageCell with a single public var
 *
 * @see StageCell
 *
 * @author Stefan Plantikow <Stefan.Plantikow@googlemail.com>
 */
trait SimpleStageCell[H <: Stage[_]] extends StageCell[H] {
  var stage: H = null.asInstanceOf[H]
}


/**
 * Stage whose prop can be exchanged at runtime
 *
 * @author Stefan Plantikow <Stefan.Plantikow@googlemail.com>
 */
abstract trait Settable[P] extends Stage[P] with PropCell[P] ;


/**
 * Stage that provides some StageDrain (like its prop) with a reference to itself during onScene
 *
 * @author Stefan Plantikow <Stefan.Plantikow@googlemail.com>
 */
abstract trait Reflection[P <: StageDrain[_ >: S], S <: Stage[P]] extends Stage[P] {
  self: S =>
  
  override def onScene(scene: this.type => Unit) = {
      prop.stage_=(self)
      super.onScene(scene)
    }
}