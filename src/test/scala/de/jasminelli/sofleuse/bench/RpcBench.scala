package de.jasminelli.sofleuse.bench

import actors.{SceneCapturing, StageActor, LoopingActor}
import de.jasminelli.sofleuse.core.Play
import scala.actors.{Actor, Channel, ActorGC}
import util.Barrier

/**
 * Parameters to a run of a RpcBench instance
 * 
 * @author Stefan Plantikow<Stefan.Plantikow@googlemail.com>
 *
 * Originally created by User: stepn Date: 13.02.2009 Time: 15:30:08
 */
case class BenchParams(load: RqLoad, numStages: Int, warmUp: Int, times: Int) ;

abstract sealed class RqLoad(numRequests: Int) { val requiredRequests = numRequests }

case class LinRqLoad(numRequests: Int) extends RqLoad(numRequests) ;

case class BulkRqLoad(numRequests: Int) extends RqLoad(numRequests) ;

case class ParRqLoad(numRequests: Int, numPartitions: Int) extends RqLoad(numRequests) {
  assert(((numRequests/numPartitions) * numPartitions) == numRequests)
}


/**
 * Abstract super class of sofleuse vs. classic-scala ping-pong actor messaging benchmark
 */
abstract class RpcBench(params: BenchParams) {
  def startup: Barrier

  def request(obl: Barrier#Obligation) = ()

  def shutdown: Barrier

  def execute: Long = {
    0.until(params.warmUp).foreach { _ => executeOnce }
    0.until(params.times).foldLeft[Long](0) { (sum: Long, iter: Int) => sum + executeOnce }
  }

  def executeOnce(): Long = {
    try {
      val startBarrier = startup
      startBarrier.await
      val startTime = System.currentTimeMillis
      generateLoad.await
      System.currentTimeMillis - startTime
    }
    finally {
      shutdown.await
      Actor.clearSelf
      ActorGC.gc
      System.gc
    }    
  }

  def generateLoad: Barrier = {
    val bar = new Barrier('load)

    params.load match {
      case (load: LinRqLoad) =>
        0.until(load.numRequests).foreach { (r: Int) => request(bar.newObligation) }

      case (load: BulkRqLoad) => 
        0.until(load.numRequests).foreach { (r: Int) =>
            val obl = bar.newObligation
            Actor.actor { request(obl) }
        }

      case (load: ParRqLoad) => {
        val requestsPerActor = load.numRequests/load.numPartitions
        0.until(load.numPartitions).foreach { (p: Int) =>
            val actorObls = new Array[Barrier#Obligation](requestsPerActor)
            for (r <- 0.until(requestsPerActor))
              actorObls(r) = bar.newObligation
            Actor.actor { 0.until(requestsPerActor).foreach { r => request(actorObls(r)) } }
        }
      }

      case _ =>
        throw new IllegalArgumentException("Unsupported load type")
    }
    bar
  }


  def generateResult(tag: String): Long = {
    val dur: Long = execute
    Console.println(tag, params, params.times, dur, dur/params.times)
    dur
  }
}
