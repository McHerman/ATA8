import chisel3._
import chiseltest._
import org.scalatest.FlatSpec
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.experimental.ChiselEnum
import scala.io.Source

import scala.util.Random

class VectorFillerTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "VectorFiller"

  it should "fill" in {
    test(new VectorFiller(1,8)).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>
      dut.io.n.poke(8.U)

      dut.io.vector(0).expect(8.U)
/*       dut.io.vector(1).expect(8.U)    
      dut.io.vector(2).expect(0.U)    
      dut.io.vector(3).expect(0.U)     */


      dut.clock.step(10)
    }
  }
}
