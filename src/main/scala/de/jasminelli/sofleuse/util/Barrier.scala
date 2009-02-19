package de.jasminelli.sofleuse.util

import scala.actors.{Actor, Channel}
import scala.actors.Actor._


/**
 * Barrier: Wait for a fixed-size number of external processes to complete
 *
 *   val bar = new Barrier('test)
 *   val o1 = bar.newObligation
 *   val o2 = bar.newObligation
 *   Actor.actor { o1.fullfill }
 *   Actor.actor { o2.fullfill }
 *   bar.await
 *   Console.println("Yep.")
 *
 * Notes: All obligations must be acquired before calling await; await may only be
 * called in the actor that instantiated the barrier; barriers can be used only once
 *
 * @author Stefan Plantikow<stefan.plantikow@googlemail.com>
 *
 * Originally created by User: stepn Date: 15.02.2009 Time: 04:29:14
 */

class Barrier(tag: Any, size: Int) {
  val channel: Channel[Any] = new Channel[Any](Actor.self)

  sealed case class Obligation(nr: Int) { self: Obligation => def fullfill = channel ! self }

  var freeObligations: List[Obligation] = 0.until(size).toList.map { id => Obligation(id) }

  var openObligations: Set[Obligation] = collection.immutable.HashSet.empty[Obligation]

  def newObligation: Obligation = synchronized {
    if (freeObligations.isEmpty) throw new IllegalStateException("No obligations left")
    val ret = freeObligations.head
    freeObligations = freeObligations.tail
    openObligations = openObligations + ret
    ret
  }

  def await: Unit = {
    while (! readyToGo) ()

    while (! done)
      channel.receive {
        case (o: Obligation) => synchronized { openObligations = openObligations - o }
        case _ => throw new IllegalArgumentException("Invalid obligation from channel")
      }
  }

  private def readyToGo = synchronized { freeObligations.isEmpty }

  private def done = synchronized { openObligations.isEmpty }
}
