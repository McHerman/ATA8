package ATA8

import chisel3._
import chiseltest._
import org.scalatest.FlatSpec
import org.scalatest.flatspec.AnyFlatSpec

import scala.util.Random
import scala.io.Source

class OSTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "Grain16"
  
	val n = 8

  val matrix: Array[Array[Int]] = Array(
    Array(1, 2, 3, 4, 1, 2, 3, 4),
    Array(1, 2, 3, 4, 1, 2, 3, 4),
    Array(1, 2, 3, 4, 1, 2, 3, 4),
    Array(1, 2, 3, 4, 1, 2, 3, 4),
    Array(1, 2, 3, 4, 1, 2, 3, 4),
    Array(1, 2, 3, 4, 1, 2, 3, 4),
    Array(1, 2, 3, 4, 1, 2, 3, 4),
    Array(1, 2, 3, 4, 1, 2, 3, 4)
  )

  def matrixDotProduct(A: Array[Array[Int]], B: Array[Array[Int]]): Array[Array[Int]] = {
      val n = A.length
      Array.tabulate(n, n) { (i, j) =>
          (0 until n).map(k => A(i)(k) * B(k)(j)).sum
      }
  }

  implicit val c = Configuration.test()
	
  "OSTest " should "pass" in {
    test(new Grain()).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>

      for (i <- 0 until 8) {

        dut.io.writePort(1)(0).ready.expect(true.B)
        dut.io.writePort(1)(0).valid.poke(true.B)

        for(k <- 0 until 8){
          dut.io.writePort(1)(0).bits(k).poke(matrix(k)(i).U(8.W))
        }


        /* dut.io.State.poke(0.U)
        dut.io.Trigger.poke(false.B)
        dut.io.Size.poke(8.U) */

        dut.io.in.valid.poke(false.B)
        dut.io.in.bits.mode.poke(0.U)
        dut.io.in.bits.size.poke(8.U)

        dut.clock.step()
      }

      dut.io.writePort(1)(0).valid.poke(false.B)

      for (i <- 0 until 8) {
        dut.io.writePort(0)(0).ready.expect(true.B)
        dut.io.writePort(0)(0).valid.poke(true.B)


        for(k <- 0 until 8){
          dut.io.writePort(0)(0).bits(k).poke(matrix(i)(k).U(8.W))
        }
   
        /* dut.io.State.poke(0.U)
        dut.io.Trigger.poke(false.B)
        dut.io.Size.poke(8.U) */

        dut.io.in.valid.poke(false.B)
        dut.io.in.bits.mode.poke(0.U)
        dut.io.in.bits.size.poke(8.U)
  
        dut.clock.step()
      }

      dut.io.writePort(0)(0).valid.poke(false.B)

      dut.clock.step(10) 

      /* dut.io.Trigger.poke(true.B)
      dut.io.Size.poke(8.U) */
      
      dut.io.in.valid.poke(true.B)
      dut.io.in.bits.mode.poke(1.U)
      dut.io.in.bits.size.poke(8.U)

      dut.clock.step()

      dut.io.in.valid.poke(false.B)


      //dut.io.Trigger.poke(false.B)

      dut.clock.step(80)
      
      /* val expectedResult = matrixDotProduct(matrix, matrix)

      dut.io.readPort(0).request.ready.expect(true.B)
      dut.io.readPort(0).request.valid.poke(true.B)

      for (row <- 0 until n) {
        dut.io.readPort(0).response.valid.expect(true.B)

        for (col <- 0 until n) {
          dut.io.readPort(0).response.bits.readData(col).expect(expectedResult(row)(col).U(8.W))
        }
        
        dut.clock.step()
      } */
    } 
  }
}  
