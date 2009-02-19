package de.jasminelli.sofleuse.util

import junit.framework._
import scala.actors.Actor

/**
 * @see Barrier
 * 
 * @author Stefan Plantikow<stefan.plantikow@googlemail.com> 
 *
 * Originally created by User: stepn Date: 15.02.2009 Time: 18:57:35
 */

object BarrierTest {
  def suite: Test = {
      val suite = new TestSuite(classOf[BarrierTest]);
      suite
  }

  def main(args : Array[String]) {
      junit.textui.TestRunner.run(suite);
  }
}


class BarrierTest extends TestCase("app")  {

  def testSync = {
    val bar = new Barrier('test, 2)
    val o1 = bar.newObligation
    val o2 = bar.newObligation
    Actor.actor { o1.fullfill }
    Actor.actor { o2.fullfill }
    bar.await
    Console.println("Yep.")
  }

  def testASync = {
    val bar = new Barrier('test, 2)
    Actor.actor { bar.newObligation.fullfill }
    Actor.actor { bar.newObligation.fullfill }
    bar.await
    Console.println("Yep.")
  }

  def main(args: Array[String]) = {
    testSync
    testASync
  }
}