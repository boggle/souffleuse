package de.jasminelli.souffleuse.util

import junit.framework._
import scala.actors.Actor

/**
 * @see Latch
 * 
 * @author Stefan Plantikow<stefan.plantikow@googlemail.com> 
 *
 * Originally created by User: stepn Date: 15.02.2009 Time: 18:57:35
 */

object LatchTest {
  def suite: Test = {
      val suite = new TestSuite(classOf[LatchTest]);
      suite
  }

  def main(args : Array[String]) {
      junit.textui.TestRunner.run(suite);
  }
}


class LatchTest extends TestCase("app")  {

  def testSync = {
    val bar = new Latch('test, 2)
    val o1 = bar.newObligation
    val o2 = bar.newObligation
    Actor.actor { o1.fullfill }
    Actor.actor { o2.fullfill }
    bar.await
    Console.println("Yep.")
  }

  def testASync = {
    val bar = new Latch('test, 2)
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