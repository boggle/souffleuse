package de.jasminelli.sofleuse.bench

import _root_.de.jasminelli.sofleuse.actors.{SceneCapturing, StageActor}
import _root_.de.jasminelli.sofleuse.core.Play
import _root_.scala.actors.{Actor, Channel, ActorGC}
/**
 * ThingAMagic.
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




abstract class RpcBench(params: BenchParams) {
  def startup: Channel[Any]

  def request(channel: Channel[Any]) = ()

  def shutdown: Unit

  def execute: Long = {
    0.until(params.warmUp).foreach { _ => executeOnce }
    0.until(params.times).foldLeft[Long](0) { (sum: Long, iter: Int) => sum + executeOnce }
  }

  def executeOnce(): Long = {
    try {
      Actor.clearSelf
      System.gc
      waitForRequests('started, params.numStages, startup)
      val start = System.currentTimeMillis
      waitForRequests('done, (params.load).requiredRequests, generateLoad)
      System.currentTimeMillis - start
    }
    finally { shutdown; ActorGC.gc }    
  }

  def generateLoad: Channel[Any] = {
    val channel = new Channel[Any](Actor.self)
    val doRequest_ = doRequest(channel)
    params.load match {
      case (load: LinRqLoad) =>
        0.until(load.numRequests).foreach(doRequest_)
      case (load: BulkRqLoad) => 
        0.until(load.numRequests).foreach { _ => Actor.actor { doRequest_() } }
      case (load: ParRqLoad) =>
        0.until(load.numPartitions).foreach { _ =>
                Actor.actor { 0.until(load.numRequests/load.numPartitions).foreach(doRequest_) } }
      case _ =>
        throw new IllegalArgumentException("Unsupported load type")
    }
    channel
  }

  def waitForRequests(tag: Symbol, rqCount: Int, channel: Channel[Any]) = {
    var outstandingRequests = rqCount
    while(outstandingRequests > 0) {
      channel.receive {
        case msg => if (msg == tag) outstandingRequests = outstandingRequests - 1
                    else throw new IllegalStateException("Unexpected Message")
      }
    }
  }

  def doRequest(channel: Channel[Any]): (Any => Unit) = {
    _ => request(channel)
  }

  def generateResult(tag: String): Long = {
    val dur: Long = execute
    Console.println(tag, params, params.times, dur, dur/params.times)
    dur
  }
}

class PingPongBench(params: BenchParams) extends RpcBench(params) {
  var stages: Array[Actor] = null

  class BenchStage(channel: Channel[Any]) extends Actor {
    def act() = {
      var isRunning = true
      channel ! 'started
      Actor.loopWhile(isRunning) { receive {
        case null => isRunning = false
        case _ => Actor.reply(1)
      } }
    }

    start
  }

  def startup: Channel[Any] = {
    val channel = new Channel[Any](Actor.self)
    stages = new Array[Actor](params.numStages)
    0.until(params.numStages).foreach { sid => stages(sid) = new BenchStage(channel) }
    channel
  }

  override def request(channel: Channel[Any]) = {
    0.until(params.numStages).foreach { sid => stages(sid) !? 'compute }
    channel ! 'done
  }

  def shutdown = {
    0.until(params.numStages).foreach { sid => stages(sid) ! null }
    stages = null
  }
}


class ACDCBench(params: BenchParams) extends RpcBench(params) {
  var first: BenchStage = null

  class BenchStage(channel: Channel[Any], val next: BenchStage) extends StageActor {
    var rqNum: Int = 0

    override def act(): Unit = { channel ! 'started; super.act }
    
    override def mainLoop = {
      rqNum = rqNum + 1;
      super.mainLoop;
    }

    override def isRunning = super.isRunning && (rqNum <= params.load.requiredRequests)

    start
  }

  def startup: Channel[Any] = {
    val channel = new Channel[Any](Actor.self)
    first = 0.until(params.numStages).foldRight[BenchStage](null)
              { (id, next) => new BenchStage(channel, next) }
    channel
  }

  def sendRequest(rqStage: BenchStage, cont: (BenchStage => Unit)): Unit =
    (for (stage <- Play.goto(rqStage);
         _ <- Play.compute {
                if (stage.next == null) cont(rqStage) else sendRequest(stage.next, cont)
         })
    yield ()).respond { _ => () }

  override def request(channel: Channel[Any]) = sendRequest(first, { _ => channel ! 'done })

  def shutdown = { first = null }

}


object ACDCvsPingPongBench {

  def main(args: Array[String]): Unit = {
    val stages = 2
    val rq = 512*8
    
    new ACDCBench(BenchParams(BulkRqLoad(rq), stages, 2, 8)).generateResult("acdc-lin");
    new ACDCBench(BenchParams(BulkRqLoad(rq), stages, 2, 8)).generateResult("acdc-lin");

    new PingPongBench(BenchParams(BulkRqLoad(rq), stages, 2, 8)).generateResult("pingpong-lin");
    new PingPongBench(BenchParams(BulkRqLoad(rq), stages, 2, 8)).generateResult("pingpong-lin");

    new ACDCBench(BenchParams(BulkRqLoad(rq), stages, 2, 8)).generateResult("acdc-lin");

    //new ACDCBench(BenchParams(BulkRqLoad(rq), stages, 2, 8)).generateResult("acdc-bulk");
    //new ACDCBench(BenchParams(ParRqLoad(rq, rq/stages), stages, 2, 8)).generateResult("acdc-par");

    //new PingPongBench(BenchParams(BulkRqLoad(rq), stages, 2, 8)).generateResult("pingpong-bulk");
    //new PingPongBench(BenchParams(ParRqLoad(rq, rq/stages), stages, 2, 8)).generateResult("pingpong-par");

    //new PingPongBench(BenchParams(LinRqLoad(rq), stages, 2, 8)).generateResult("pingpong-lin");
    //new PingPongBench(BenchParams(BulkRqLoad(rq), stages, 2, 8)).generateResult("pingpong-bulk");
    //new PingPongBench(BenchParams(ParRqLoad(rq, rq/stages), stages, 2, 8)).generateResult("pingpong-par");
  }
}