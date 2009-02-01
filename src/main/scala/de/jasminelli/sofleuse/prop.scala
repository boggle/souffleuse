package de.jasminelli.sofleuse

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
 */
trait PropDrain[-H] { def prop_=(newProp: H): Unit }

/**
 * @see PropSource
 * @see PropDrain
 */
trait PropCell[H] extends PropSource[H] with PropDrain[H] ;


/**
 * Implements PropCell with a single public var
 *
 * @see PropCell
 */
trait SimplePropCell[H] extends PropCell[H] {
  var prop: H
}