package de.jasminelli.sofleuse.bench

import de.jasminelli.sofleuse.actors._
import scala.actors._
import scala.actors.Actor._
import scala.Console
import util.Barrier
import scala.collection.immutable._
/**
 * @author Stefan Plantikow<plantikow@zib.de>
 *
 * Originally created by User: stepn Date: 15.02.2009 Time: 15:10:47
 */
class PingPongBench(params: BenchParams, verific: Verificator) extends RpcBench(params, verific) {
  override type ActorType = BenchStage

  class BenchStage(obl: Barrier#Obligation, val next: BenchStage, verify: Byte)
          extends LoopingActor with BenchActor {
    override protected val initialObl = obl

    def mainLoopBody =  receive {
        case (lst: List[Byte]) => {
          sleep(params.workDur);
          assert(lst.head == verify, "Verification failed, stages not passed correctly!")
          verific.incrStagesPassed
          Actor.reply((next, lst.tail))
        }
        case (someObl: Barrier#Obligation) => { shutdownAfterScene; finalObl = someObl }
    }

    start
  }

  def mkStage(obl: Barrier#Obligation, next: BenchStage, verifyByte: Byte) =
    new BenchStage(obl, next, verifyByte)

  def nextStage(stage: BenchStage) = stage.next

  def doParRequests(numRqs: Int): Unit = {
    // Send out
    var outstanding: Int = numRqs
    for (r <- 0.until(numRqs)) {
      val chan = new Channel[Any](Actor.self)
      first ! verifyList
    }


    // Collect and dispatch until done
    while (outstanding > 0) {
      Actor.self.receive {
        case (result: (BenchStage, List[Byte])) =>  {
          val next = result._1
          if (next != null) {
            next ! result._2
          }
          else {
            assert(result._2.isEmpty)
            outstanding = outstanding - 1
          }
        }
        case _ => throw new IllegalStateException("Unexpected or wrong result message")
      }
    }
  }
}
