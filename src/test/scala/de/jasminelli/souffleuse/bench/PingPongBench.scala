package de.jasminelli.souffleuse.bench

import de.jasminelli.souffleuse.actors._
import scala.actors._
import scala.Console
import scala.collection.immutable._
import util.{Latch, DeferredSending}
/**
 * @author Stefan Plantikow<plantikow@zib.de>
 *
 * Originally created by User: stepn Date: 15.02.2009 Time: 15:10:47
 */
class PingPongBench(params: BenchParams, verific: Verificator) extends RpcBench(params, verific) {
  override type ActorType = BenchStage

  class BenchStage(obl: Latch#Obligation,
                  val next: BenchStage, val nextId: Int, val verifyList: Array[Byte])
          extends LoopingActor with BenchActor with DeferredSending  {

    override protected val initialObl = obl

    protected val verifyByte = if (next == null) verifyList.last else verifyList(nextId - 1)

    def mainLoopBody =  receive {
        case (verify: Byte) => {
          sleep(params.workDur);
          if (verific.stageBasedVerify)
            assert(verifyByte == verify, "Verification failed, stages not passed correctly!")
          verific.incrStagesPassed
          
          if (params.deferredSending) {
            val sender = this.sender
            sendDeferred(sender, nextId)
          }
          else
            sender.!(nextId)
        }
        case (someObl: Latch#Obligation) => { shutdownAfterScene; finalObl = someObl }
    }

    override def onStartActing = {
      super.onStartActing
      if (params.deferredSending) startDeferredSending(Actor.self)
    }

    override def onStopActing = {
      stopDeferredSending
      super.onStopActing
    }

    start
  }

  def mkStage(obl: Latch#Obligation, next: BenchStage, nextId: Int, verifyList: Array[Byte]) =
    new BenchStage(obl, next, if (next == null) -1 else nextId, verifyList)

  def nextStage(stage: BenchStage) = stage.next

  def doParRequests(numRqs: Int): Unit = {
    val selfActor = Actor.self

    // Send out
    var outstanding: Int = numRqs
    for (r <- 0.until(numRqs))
      first.!(verifyList(0))


    // Collect and dispatch until done
    while (outstanding > 0) {
      // Console.println("outstanding: " + outstanding)
      selfActor.receive {
        case (nextId: Int) =>
          if (nextId >= 0)
            stages(nextId).!(verifyList(nextId))
          else {
            assert(nextId == -1)
            outstanding = outstanding - 1
          }
        // case (msg: Any) => throw new IllegalStateException("Unexpected or wrong result message: " + msg)
      }
    }
  }
}
