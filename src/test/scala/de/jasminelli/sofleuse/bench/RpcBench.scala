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
final case class BenchParams(val load: RqLoad,
                             val workDur: Long,
                             val numStages: Int,
                             val warmUp: Int, val times: Int);

abstract sealed class RqLoad(val numRequests: Int) {
  val requiredRequests = numRequests
  val numObligations = numRequests
}

case class LinRqLoad(requests: Int) extends RqLoad(requests) ;

case class BulkRqLoad(requests: Int) extends RqLoad(requests) ;

case class ParRqLoad(requests: Int, val numPartitions: Int) extends RqLoad(requests) {
  assert(((numRequests/numPartitions) * numPartitions) == numRequests)
}

case class NBParRqLoad(requests: Int, val numPartitions: Int) extends RqLoad(requests) {
  assert(numPartitions > 0)

  override val numObligations = numPartitions
}


/**
 * Abstract super class of sofleuse vs. classic-scala ping-pong actor messaging benchmark
 */
abstract class RpcBench(params: BenchParams) {
  def startup: Barrier

  def request(obl: Barrier#Obligation): Int

  def parRequests(obl: Barrier#Obligation, numRqs: Int): Unit

  def shutdown: Barrier

  def execute: Long = {
    Console.print("# ")
    0.until(params.warmUp).foreach { _ => executeOnce }
    Console.print(" /* post-warmup */ ")
    val results: List[Long] =
      (for (i <- 0.until(params.times)) yield executeOnce).toList
    Console.println()
    results.sort(_<=_)(params.times/2)
  }

  def executeOnce(): Long = {
    try {
      Console.print("(" + threadCount)
      val startBarrier = startup
      startBarrier.await
      val startTime = System.currentTimeMillis
      generateLoad.await
      System.currentTimeMillis - startTime
    }
    finally {
      Console.print(", " + threadCount + ") ")
      shutdown.await
      Actor.clearSelf
      ActorGC.gc
      System.gc
    }
  }

  def threadCount = {
    Thread.currentThread.getThreadGroup.activeCount
  }
  def generateLoad: Barrier = {
    val bar = new Barrier('load, params.load.numObligations)

    params.load match {
      case (load: LinRqLoad) =>
        for (r <- 0.until(load.numRequests))
          doRequest(bar.newObligation)

      case (load: BulkRqLoad) =>
        for (r <-0.until(load.numRequests)) {
            val obl = bar.newObligation
            Actor.actor { doRequest(obl); Actor.self.exit }
        }

      case (load: ParRqLoad) => {
        val requestsPerActor = load.numRequests/load.numPartitions
        for (p <- 0.until(load.numPartitions)) {
            val actorObls = new Array[Barrier#Obligation](requestsPerActor)
            for (r <- 0.until(requestsPerActor))
              actorObls(r) = bar.newObligation
            Actor.actor {
              for (r <- 0.until(requestsPerActor))
                doRequest(actorObls(r))
              Actor.self.exit
            }
        }
      }

      case (load: NBParRqLoad) => {
        val requestsPerActor = load.numRequests/load.numPartitions
        val remainingRequests = load.numRequests % load.numPartitions
        for (p <- 0.until(load.numPartitions)) {
          val obl = bar.newObligation
          val rqs = if ((p+1) == load.numPartitions) remainingRequests else requestsPerActor
          Actor.actor {
            parRequests(obl, rqs)
            Actor.self.exit
          }
        }
      }

      case _ =>
        throw new IllegalArgumentException("Unsupported load type")
    }

    bar
  }

  def doRequest(obl: Barrier#Obligation) = {
    assert(request(obl) == params.numStages)
    obl.fullfill
  }

  def generateResult(tag: String): Long = {
    Console.println
    Console.print("# ")
    Console.println(tag, params)
    val dur: Long = execute
    Console.format("%s\t%s\t%s\t%s\t%s\t%s\t%s",
      tag, params.load.requiredRequests, params.numStages, params.times, dur, dur/params.times,
      threadCount)
    dur
  }

  def sleep(dur: Long) {
    if (dur > 0L) {
      var now: Long = System.currentTimeMillis
      val end: Long = now + dur
      do {
        try {
          Thread.sleep(end - now)
        }
        catch { case (_: InterruptedException) => () }
        now = System.currentTimeMillis
      } while (now < end)
    }
  }
}
