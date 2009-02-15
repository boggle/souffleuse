package de.jasminelli.sofleuse.util

import scala.actors.Actor
import util.Barrier

/**
 * @see Barrier
 * 
 * @author Stefan Plantikow<stefan.plantikow@googlemail.com> 
 *
 * Originally created by User: stepn Date: 15.02.2009 Time: 18:57:35
 */

object BarrierTest {

  def main(args: Array[String]) = {
     val bar = new Barrier('test)
     val o1 = bar.newObligation
     val o2 = bar.newObligation
     Actor.actor { o1.fullfill }
     Actor.actor { o2.fullfill }
     bar.await
     Console.println("Yep.")
  }
}