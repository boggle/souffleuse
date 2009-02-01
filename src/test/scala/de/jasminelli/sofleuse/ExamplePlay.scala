package de.jasminelli.sofleuse

import scala.actors.{Futures, Actor, Channel, Future}

/**
 * ExamplePlay demonstrates rpc-like computation over two stages, "stage reflection"
 * and capturing request continuations.
 * 
 * @author Stefan Plantikow <Stefan.Plantikow@googlemail.com>
 * @version $Id$ 
 *
 * Originally created by User: stepn Date: 21.01.2009 Time: 17:30:24
 */
object ExamplePlay {

  
    // Preliminaries


    // Create some global counter
    var sceneCount: Int = 0
    def nextCount = { sceneCount = sceneCount + 1; sceneCount }


    // Actual state of stages in this test (= prop)
    class TestProp(val name: String, val next: TestStage[TestProp]) ;


    // We extend Stage to show how scene-execution can be modified
    class TestStage[+P <: TestProp](override val prop: P) extends Stage[P] {

        override protected def onScene(scene: Scene) = {
            val sceneStr = "(Scene " + nextCount + ")"
            println("> My name is: " + prop.name + ".  I am an obedient Stage " + sceneStr + ".")
            super.onScene(scene)
            println("< Hah! If I work harder, I might be allowed to idle " + sceneStr + ".")
        }

        override def onStopActing = println("Shutdown!")

        def println(p: String) =
          Console.println("[Thread: " + Thread.currentThread.getId + "] " + p)
    }

  
    // Subclassing of stage prop
    class SpecialProp(override val name: String, override val next: TestStage[TestProp])
            extends TestProp(name, next) with SimpleStageCell[TestStage[SpecialProp]] {

        def somethingSpecial:Int = {
          stage.println("I can do something special for you");
          42
        }
    }


    // Stage Reflection
    //
    // Stage with exchangeable prop and prop depending on stage type at the same time!
    class SpecialStage(override val prop: SpecialProp) extends TestStage[SpecialProp](prop)
            with Reflection[SpecialProp, TestStage[SpecialProp]];


    /**
     * Actual test
     */
    def whatIsSixTimesSeven: Int = {

        // Create two linked stages
        val last = new SpecialStage(new SpecialProp("Mr. White", null))
        val first = new TestStage[TestProp](new TestProp("Mr. Pink", last))


        // Run them in separate threads
        first.start
        last.start


        // Execute workflow of acts over the two stages
        //
        // Notice that everything is properly typed... yay!
        //
        object play extends Play[TestStage[TestProp], Int] {

            override def apply(): Responder[Int] = {
                for(/* select first stage to execute in */
                    stage <- goto(first);
                    /* remember this stage for later (locally, i.e. w/o shared global state)*/
                    firstStage <- remember(stage);
                    stage <- cast[TestStage[SpecialProp]] {
                        /* execute in first stage and compute second stage to execute in */
                        stage.println("Step 1 knows he is performed by " + stage.prop.name)
                        stage.prop.next
                    };
                    answer <- compute[Int] {
                        /* execute in second stage and compute third stage to execute in */
                        stage.println("Step 2 knows he is performed by " + stage.prop.name)
                        stage.shutdownAfterScene
                        /* result is stored for later user */
                        stage.prop.somethingSpecial
                    };
                    stage <- goto(firstStage);
                    _ <- jump {
                        /* execute in third stage and signal end of processing after next round */
                        stage.println("Step 3 knows he is performed by " + stage.prop.name)
                        /* allow for one more computation in stage */
                        /* this demos continuation capturing, see call-nc.scala */
                        stage.shutdownAfterNextScene
                    };
                    _ <- compute {
                      /* execute again (w/o interleaving scenes from other plays) in third stage
                         and print result just before stage shutdown
                       */
                      stage.println("The answer to the question computed in step 2 was: " + answer)
                    })
                        /* actual for-body just fills in result */
                        yield answer
            }
        }

        play.play
    }

    def main(p: Array[String]): Unit = {
        whatIsSixTimesSeven
    }
}