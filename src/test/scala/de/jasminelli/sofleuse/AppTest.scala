package de.jasminelli;

import junit.framework._
import sofleuse.{RpcDemo, ExamplePlay};
import Assert._;
object AppTest {
    def suite: Test = {
        val suite = new TestSuite(classOf[AppTest]);
        suite
    }

    def main(args : Array[String]) {
        junit.textui.TestRunner.run(suite);
    }
}

/**
 * Unit test for simple App.
 */
class AppTest extends TestCase("app") {

    /**
     * Rigourous Tests :-)
     */
    def testStageRPC = {
      assert(RpcDemo.rpcDemo == 2)
      assert(ExamplePlay.whatIsSixTimesSeven == 42)
    }
}
