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

  val WMatrix: Array[Array[Int]] = Array(
    Array(1, 2, 3, 4, 1, 2, 3, 4),
    Array(1, 2, 3, 4, 1, 2, 3, 4),
    Array(1, 2, 3, 4, 1, 2, 3, 4),
    Array(1, 2, 3, 4, 1, 2, 3, 4),
    Array(1, 2, 3, 4, 1, 2, 3, 4),
    Array(1, 2, 3, 4, 1, 2, 3, 4),
    Array(1, 2, 3, 4, 1, 2, 3, 4),
    Array(1, 2, 3, 4, 1, 2, 3, 4)
  )

  val AMatrix: Array[Array[Int]] = Array(
    Array(1, 2, 3, 4, 1, 2, 3, 4),
    Array(1, 2, 3, 4, 1, 2, 3, 4),
    Array(1, 2, 3, 4, 1, 2, 3, 4),
    Array(1, 2, 3, 4, 1, 2, 3, 4),
    Array(1, 2, 3, 4, 1, 2, 3, 4),
    Array(1, 2, 3, 4, 1, 2, 3, 4),
    Array(1, 2, 3, 4, 1, 2, 3, 4),
    Array(1, 2, 3, 4, 1, 2, 3, 4)
  )

  "Test " should "pass" in {
    //val n = 2 + Random.nextInt(30)
    //val n = 16

    

    test(new Grain(Configuration.test())).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>

      for (i <- 0 until 8) {
        dut.io.Memport.valid.poke(true.B)
        dut.io.Memport.bits.addr.poke(2.U)
        dut.io.Memport.bits.wenable.poke(true.B)

        for(k <- 0 until 8){
          dut.io.Memport.bits.writeData(k).poke(WMatrix(7-i)(k).U(8.W))
        }


        /* dut.io.State.poke(0.U)
        dut.io.Trigger.poke(false.B)
        dut.io.Size.poke(8.U) */

        dut.io.in.valid.poke(false.B)
        dut.io.in.bits.mode.poke(0.U)
        dut.io.in.bits.size.poke(8.U)

        dut.io.Readport.request.valid.poke(false.B)

        dut.clock.step()
      }

      for (i <- 0 until 8) {
        dut.io.Memport.valid.poke(true.B)
        dut.io.Memport.bits.addr.poke(1.U)
        dut.io.Memport.bits.wenable.poke(true.B)


        for(k <- 0 until 8){
          dut.io.Memport.bits.writeData(k).poke(AMatrix(i)(k).U(8.W))
        }
   
        /* dut.io.State.poke(0.U)
        dut.io.Trigger.poke(false.B)
        dut.io.Size.poke(8.U) */

        dut.io.in.valid.poke(false.B)
        dut.io.in.bits.mode.poke(0.U)
        dut.io.in.bits.size.poke(8.U)

        dut.io.Readport.request.valid.poke(false.B)
  
        dut.clock.step()
      }

      dut.io.Memport.valid.poke(false.B)



      dut.clock.step(10) 

      /* dut.io.Trigger.poke(true.B)
      dut.io.Size.poke(8.U) */
      
      dut.io.in.valid.poke(true.B)
      dut.io.in.bits.mode.poke(0.U)
      dut.io.in.bits.size.poke(8.U)

      dut.clock.step()

      dut.io.Trigger.poke(false.B)

      dut.clock.step(80)
      
      dut.io.Readport.request.valid.poke(true.B)
      dut.io.Readport.request.bits.addr.poke(0.U)

      dut.clock.step(8)

      dut.io.Readport.request.valid.poke(false.B)





      dut.clock.step(30)





    }
  }
}  
