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
 * called in the actor that instantiated the barrier; barriers can be reused by calling
 * reset after await.
 *
 * @author Stefan Plantikow<plantikow@zib.de>
 *
 * Originally created by User: stepn Date: 15.02.2009 Time: 04:29:14
 */

class Barrier(tag: Any) {
  // Channel
  private val chan: Channel[Any] = new Channel[Any](Actor.self)

  @volatile private var waiting = false /* if true, we're in await() */
  @volatile private var outstanding: Int = 0 /* obligations yet to be fulfilled */

  class Obligation {
    self: Barrier#Obligation =>

    @volatile 
    private var fullfilled: Boolean = false

    def fullfill(): Unit = synchronized {
      if (fullfilled == true)
        throw new IllegalStateException(tag+ "-Obligation already fullfilled " + this)
      else {
        chan ! self
        fullfilled = true
      }
    }
  }

  def newObligation: Obligation = synchronized {
    if (waiting)
      throw new IllegalStateException("Cant create new obligations while barrier is waiting")
    outstanding = outstanding + 1;
    new Obligation()
  }

  def await: Unit = {
    synchronized {
      if (waiting == false)
          waiting = true
        else
          throw new IllegalStateException("Already waiting")
    }
    recAwait
  }

  private def recAwait: Unit = if (outstanding > 0) { chan.receive(matcher); return recAwait }

  protected val matcher: PartialFunction[Any, Unit] = {
        case (obl: Barrier#Obligation) =>
          outstanding = outstanding - 1
        case argl => {
          throw new IllegalArgumentException("Invalid obligation encountered: " + argl)
       }
  }


  def reset = synchronized {
    if (outstanding > 0)
      throw new IllegalStateException("Outstanding obligations")
      else { outstanding = 0; waiting = false }
  }
}
