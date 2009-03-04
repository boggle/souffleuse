package de.jasminelli.souffleuse.stage


import de.jasminelli.souffleuse.core.{ResponsivePlayer, Play}


/**
 * Prop Source
 *
 * A prop contains all encapsulated behaviour and state of a stage
 *
 * @author Stefan Plantikow <Stefan.Plantikow@googlemail.com>
 *
 * Originally created by User: stepn Date: 16.01.2009 Time: 23:45:03
 */
trait PropSource[+H] { def prop: H }


/**
 * Prop drain
 *
 * @see PropSource
 *
 * @author Stefan Plantikow <Stefan.Plantikow@googlemail.com>
 */
trait PropDrain[-H] { def prop_=(newProp: H): Unit }


/**
 * @see PropSource
 * @see PropDrain
 *
 * @author Stefan Plantikow <Stefan.Plantikow@googlemail.com>
 */
trait PropCell[H] extends PropSource[H] with PropDrain[H] ;


/**
 * Implements PropCell with a single public var
 *
 * @see PropCell
 *
 * @author Stefan Plantikow <Stefan.Plantikow@googlemail.com>
 */
trait SimplePropCell[H] extends PropCell[H] {
  var prop: H
}


/**
 * Extends Play with an additional utility function for computing thunks in the current stage
 * over the stage's prop
 *
 * @see Play
 *
 * @author Stefan Plantikow <Stefan.Plantikow@googlemail.com>
 */
trait PropPlay[-R <: ResponsivePlayer, O] extends Play[R, O] {
  /**
   * Compute a single for reuse by later scenes.  The computation is provided with the prop of
   * a (likely the current) stage as an initial argument
   *
   * (runs inside the current stage actor)
   */
  def scene[R <: PropSource[P],P,T](stage: R)(thunk: => P => T) = computeWith(stage.prop)(thunk)
}