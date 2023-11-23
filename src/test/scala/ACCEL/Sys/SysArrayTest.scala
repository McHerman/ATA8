package ATA8

import chisel3._
import chiseltest._
import org.scalatest.FlatSpec
import org.scalatest.flatspec.AnyFlatSpec

import scala.util.Random
import scala.io.Source

class SysArrayTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "Grain"

  //val n = 2 + Random.nextInt(30)
  val n = 8
  val weight = "W_Matrix.txt"
  val activation = "A_Matrix.txt"

/*   val WMatrix: Array[Array[Int]] = Array(
    Array(1, 2, 3, 4, 5, 6, 7, 8),
    Array(9, 10, 11, 12, 13, 14, 15, 16),
    Array(17, 18, 19, 20, 21, 22, 23, 24),
    Array(25, 26, 27, 28, 29, 30, 31, 32),
    Array(33, 34, 35, 36, 37, 38, 39, 40),
    Array(41, 42, 43, 44, 45, 46, 47, 48),
    Array(49, 50, 51, 52, 53, 54, 55, 56),
    Array(57, 58, 59, 60, 61, 62, 63, 64)
  )

  val AMatrix: Array[Array[Int]] = Array(
    Array(1, 2, 3, 4, 5, 6, 7, 8),
    Array(9, 10, 11, 12, 13, 14, 15, 16),
    Array(17, 18, 19, 20, 21, 22, 23, 24),
    Array(25, 26, 27, 28, 29, 30, 31, 32),
    Array(33, 34, 35, 36, 37, 38, 39, 40),
    Array(41, 42, 43, 44, 45, 46, 47, 48),
    Array(49, 50, 51, 52, 53, 54, 55, 56),
    Array(57, 58, 59, 60, 61, 62, 63, 64)
  )
 */

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

  "Test " should "pass" in {
    //val n = 2 + Random.nextInt(30)
    //val n = 16

    implicit val c = Configuration.test()

    test(new Grain()).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>

      for (i <- 0 until 8) {

        dut.io.writePort(1)(0).ready.expect(true.B)
        dut.io.writePort(1)(0).valid.poke(true.B)

        for(k <- 0 until 8){
          dut.io.writePort(1)(0).bits(k).poke(matrix(i)(k).U(8.W))
        }


        /* dut.io.State.poke(0.U)
        dut.io.Trigger.poke(false.B)
        dut.io.Size.poke(8.U) */

        /* dut.io.in.valid.poke(false.B)
        dut.io.in.bits.mode.poke(0.U)
        dut.io.in.bits.size.poke(8.U) */

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

        /* dut.io.in.valid.poke(false.B)
        dut.io.in.bits.mode.poke(0.U)
        dut.io.in.bits.size.poke(8.U) */
  
        dut.clock.step()
      }

      dut.io.writePort(0)(0).valid.poke(false.B)

      dut.clock.step(10) 

      /* dut.io.Trigger.poke(true.B)
      dut.io.Size.poke(8.U) */
      
      dut.io.in.valid.poke(true.B)
      dut.io.in.bits.mode.poke(0.U)
      dut.io.in.bits.size.poke(8.U)
      dut.io.in.bits.sizes(0).poke(8.U)

      dut.clock.step()

      dut.io.in.valid.poke(false.B)


      //dut.io.Trigger.poke(false.B)

      dut.clock.step(80)
      
      val expectedResult = matrixDotProduct(matrix, matrix)

      dut.io.readPort(0).request.ready.expect(true.B)
      dut.io.readPort(0).request.valid.poke(true.B)

      for (row <- 0 until n) {
        dut.io.readPort(0).response.valid.expect(true.B)

        for (col <- 0 until n) {
          dut.io.readPort(0).response.bits.readData(col).expect(expectedResult(row)(col).U(8.W))
        }
        
        dut.clock.step()
      }
    }
  }
}  
