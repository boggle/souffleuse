package de.jasminelli.sofleuse.bench

import actors.{SceneCapturing, StageActor, LoopingActor}
import de.jasminelli.sofleuse.core.Play
import java.util.concurrent.atomic.AtomicInteger
import scala.actors.{Actor, Channel, ActorGC}
import util.Latch

/**
 * Abstract super class of sofleuse vs. classic-scala ping-pong actor messaging benchmark
 */
abstract class RpcBench[T](params: BenchParams, verific: Verificator) {
  type ActorType <: Actor

  var first: ActorType = null.asInstanceOf[ActorType]

  private var _verifyList: Array[Byte] = null
  protected def verifyList = synchronized { _verifyList }

  // useful for shutdown, avoids unneccesary list operations
  protected var stages: Array[ActorType] = new Array[ActorType](params.numStages)


  def startup: Latch = {
    val bar = new Latch('startup, params.numStages)
    first = null.asInstanceOf[ActorType]
    var last: ActorType = null.asInstanceOf[ActorType]
    for (count <- 0.until(params.numStages)) {
      var nextId = params.numStages - count
      var id = nextId - 1
      last = mkStage(bar.newObligation, last, nextId, verifyList)
      stages(id) = last
    }
    first = last
    bar
  }

  def mkStage(obl: Latch#Obligation,
             next: ActorType, nextId: Int, verifyList: Array[Byte]):ActorType

  def nextStage(stage: ActorType): ActorType
  
  def parRequests(obl: Latch#Obligation, numRqs: Int): Unit = {
    doParRequests(numRqs)
    obl.fullfill
  }

  def doParRequests(numRqs: Int): Unit

  def shutdown: Latch = {
    val bar = new Latch('shutdown, stages.length)
    for (id <- 0.until(params.numStages)) {
      stages(id) ! bar.newObligation
      stages(id) = null.asInstanceOf[ActorType]
    }
    first = null.asInstanceOf[ActorType]
    bar
  }

  def execute: Long = {
    Console.print("# ")
    for (_ <- 0.until(params.warmUp)) executeOnce
    Console.print(" /* post-warmup */ ")
    val results: List[Long] =
      (for (i <- 0.until(params.times)) yield executeOnce).toList
    Console.println()
    results.sort(_<=_)(params.times/2)
  }

  def executeOnce(): Long = {
    try {
      _verifyList = verific.generateList(params.numStages)
      verific.resetStagesPassed
      
      Console.print("(" + threadCount)
      val startLatch = startup
      startLatch.await
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
      assert(verific.testStagesPassed(params.numStages * params.load.numRequests))
    }
  }

  def threadCount = {
    Thread.currentThread.getThreadGroup.activeCount
  }

  def generateLoad: Latch = {
    val bar = new Latch('load, params.load.numObligations)

    params.load match {
      case (load: LinRqLoad) =>
        val obls = new Array[Latch#Obligation](load.numRequests)
        for (r <- 0.until(load.numRequests))
          obls(r) = bar.newObligation
        for (r <- 0.until(load.numRequests)) parRequests(obls(r), 1)

      case (load: BulkRqLoad) =>
        for (r <-0.until(load.numRequests)) {
            val obl = bar.newObligation
            Actor.actor { parRequests(obl, 1) }
        }

      case (load: ParRqLoad) => {
        val requestsPerActor = load.numRequests/load.numPartitions
        for (p <- 0.until(load.numPartitions)) {
            val actorObls = new Array[Latch#Obligation](requestsPerActor)
            for (r <- 0.until(requestsPerActor))
              actorObls(r) = bar.newObligation
            Actor.actor {
              for (r <- 0.until(requestsPerActor))
                parRequests(actorObls(r), 1)
            }
        }
      }

      /* This is the relevant one for the actual benchmark */
      case (load: NBParRqLoad) => {
        val requestsPerActor = load.numRequests/load.numPartitions
        val remainingRequests = load.numRequests % load.numPartitions
        for (p <- 0.until(load.numPartitions)) {
          val obl = bar.newObligation
          Actor.actor {
            parRequests(obl, requestsPerActor)
          }
        }
        if (remainingRequests > 0) {
          val obl = bar.newObligation
          Actor.actor {
            parRequests(obl, remainingRequests)
          }
        }
      }

      case _ =>
        throw new IllegalArgumentException("Unsupported load type")
    }

    bar
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
