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

  class BenchStage(obl: Barrier#Obligation,
                  val next: BenchStage, val nextId: Int, val verifyList: Array[Byte])
          extends LoopingActor with BenchActor {

    override protected val initialObl = obl

    protected val verifyByte = if (next == null) verifyList.last else verifyList(nextId - 1)

    def mainLoopBody =  receive {
        case (verify: Byte) => {
          sleep(params.workDur);
          assert(verifyByte == verify, "Verification failed, stages not passed correctly!")
          verific.incrStagesPassed
          Actor.reply(nextId)
        }
        case (someObl: Barrier#Obligation) => { shutdownAfterScene; finalObl = someObl }
    }

    start
  }

  def mkStage(obl: Barrier#Obligation, next: BenchStage, nextId: Int, verifyList: Array[Byte]) =
    new BenchStage(obl, next, if (next == null) -1 else nextId, verifyList)

  def nextStage(stage: BenchStage) = stage.next

  def doParRequests(numRqs: Int): Unit = {
    // Send out
    var outstanding: Int = numRqs
    for (r <- 0.until(numRqs))
      first ! verifyList(0)


    // Collect and dispatch until done
    while (outstanding > 0) {
      Actor.self.receive {
        case (nextId: Int) =>  {
          if (nextId >= 0)
            stages(nextId) ! verifyList(nextId)
          else {
            assert(nextId == -1)
            outstanding = outstanding - 1
          }
        }
        case _ => throw new IllegalStateException("Unexpected or wrong result message")
      }
    }
  }
}
