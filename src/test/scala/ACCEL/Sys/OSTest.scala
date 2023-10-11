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


	
  "OSTest " should "pass" in {
    test(new Grain(Configuration.test())).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>

      for (i <- 0 until n) {
        dut.io.Memport.valid.poke(true.B)
        dut.io.Memport.bits.addr.poke(2.U)
        dut.io.Memport.bits.wenable.poke(true.B)

        for(k <- 0 until n){
          dut.io.Memport.bits.writeData(k).poke(matrix(i)(k).U(8.W))
        }


        /* dut.io.State.poke(0.U)
        dut.io.Trigger.poke(false.B)
        dut.io.Size.poke(n.U) */

        dut.io.in.valid.poke(false.B)
        dut.io.in.bits.mode.poke(0.U)
        dut.io.in.bits.size.poke(n.U)


        dut.io.Readport.request.valid.poke(false.B)

        dut.clock.step()
      }

      for (i <- 0 until n) {
        dut.io.Memport.valid.poke(true.B)
        dut.io.Memport.bits.addr.poke(1.U)
        dut.io.Memport.bits.wenable.poke(true.B)


        for(k <- 0 until n){
          dut.io.Memport.bits.writeData(k).poke(matrix(k)(i).U(8.W))
        }
   
        /* dut.io.State.poke(0.U)
        dut.io.Trigger.poke(false.B)
        dut.io.Size.poke(n.U) */

        dut.io.in.valid.poke(false.B)
        dut.io.in.bits.mode.poke(0.U)
        dut.io.in.bits.size.poke(n.U)

        dut.io.Readport.request.valid.poke(false.B)
  
        dut.clock.step()
      }

      dut.io.Memport.valid.poke(false.B)

      dut.clock.step(10) 

      /* dut.io.State.poke(1.U)
      dut.io.Trigger.poke(true.B)
      dut.io.Size.poke(n.U) */

      dut.io.in.valid.poke(true.B)
      dut.io.in.bits.mode.poke(1.U)
      dut.io.in.bits.size.poke(n.U)

      dut.clock.step()

      dut.io.Trigger.poke(false.B)

      dut.clock.step(80)
      
      dut.io.Readport.request.valid.poke(true.B)
      dut.io.Readport.request.bits.addr.poke(0.U)

      dut.clock.step(n)

      dut.io.Readport.request.valid.poke(false.B)

      dut.clock.step(30)

			val product = Array.ofDim[Int](n, n)

			for (i <- 0 until n) {
				for (j <- 0 until n) {
					var sum = 0
					for (k <- 0 until n) {
						sum += matrix(i)(k) * matrix(k)(j)
					}
					product(i)(j) = sum
				}
			}

			// Print the product matrix
			for (row <- product) {
				println("OS")
				println(row.mkString("\t"))
			}
    }
  }
}  
