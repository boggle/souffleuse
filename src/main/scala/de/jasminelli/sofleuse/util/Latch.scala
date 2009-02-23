package de.jasminelli.sofleuse.util

import scala.actors.{Actor, Channel}
import scala.actors.Actor._


/**
 * Latch: Wait for a fixed-size number of external processes to complete
 *
 *   val bar = new Latch('test)
 *   val o1 = bar.newObligation
 *   val o2 = bar.newObligation
 *   Actor.actor { o1.fullfill }
 *   Actor.actor { o2.fullfill }
 *   bar.await
 *   Console.println("Yep.")
 *
 * Notes: All obligations must be acquired before calling await; await may only be
 * called in the actor that instantiated the Latch; Latchs can be used only once
 *
 * @author Stefan Plantikow<stefan.plantikow@googlemail.com>
 *
 * Originally created by User: stepn Date: 15.02.2009 Time: 04:29:14
 */

class Latch(tag: Any, size: Int) {
  var jLatch = new java.util.concurrent.CountDownLatch(size)
  var remaining = size


  sealed class Obligation {
    var counted_down = false
    def fullfill = synchronized { if (! counted_down) { jLatch.countDown; counted_down = true } }
  }


  def newObligation: Obligation = synchronized {
    if (remaining > 0) { remaining = remaining - 1; new Obligation }
    else throw new IllegalStateException("No obligations left")
  }


  def await: Unit = jLatch.await
}
