package de.jasminelli.sofleuse

import scala.actors.Actor

import Play._

/**
 * Simple Demo of RPC based on StageActor
 * 
 * @author Stefan Plantikow <Stefan.Plantikow@googlemail.com>
 * @version $Id$ 
 *
 * Originally created by User: stepn Date: 01.02.2009 Time: 00:06:57
 */

object RpcDemo {

  def rpcDemo: Int = {

    // Stage which guards access to some resource/computation
    object Stage extends StageActor {
      def intenseComputation(in: Int): Int = in + 1
    }

    Stage.start

    // Request representing distributed computation
    val request =

      for (
          // Select stage to compute in
          stage <- goto(Stage);
      
          // Compute in stage
          result <- compute {
            stage.shutdownAfterScene;
            println(" is having an intense computation")
            stage.intenseComputation(1)
          })
          yield result

    println( " is asking a difficult question")
    val result = run(request)
    println(" received an answer: " + result)
    result
  }

  def main(args: Array[String]): Unit = rpcDemo

  def println(s: String) = Console.println("Thread " + Thread.currentThread.getId + s)
}