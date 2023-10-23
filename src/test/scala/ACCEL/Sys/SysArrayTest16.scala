package ATA8

import chisel3._
import chiseltest._
import org.scalatest.FlatSpec
import org.scalatest.flatspec.AnyFlatSpec

import scala.util.Random
import scala.io.Source

class SysArrayTest16 extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "Grain16"
  
	val n = 16

	val matrix: Array[Array[Int]] = Array(
		Array(1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4),
    Array(1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4),
		Array(1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4),
    Array(1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4),
		Array(1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4),
    Array(1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4),
		Array(1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4),
    Array(1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4),
		Array(1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4),
    Array(1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4),
		Array(1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4),
    Array(1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4),
		Array(1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4),
    Array(1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4),
		Array(1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4),
    Array(1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4)
	)

  def matrixDotProduct(A: Array[Array[Int]], B: Array[Array[Int]]): Array[Array[Int]] = {
    val n = A.length
    Array.tabulate(n, n) { (i, j) =>
      (0 until n).map(k => A(i)(k) * B(k)(j)).sum
    }
  }

	
  "16test " should "pass" in {
    test(new Grain(Configuration.test16())).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>
      for (i <- 0 until n) {
        dut.io.Memport(0).valid.poke(true.B)
        dut.io.Memport(0).bits.wenable.poke(true.B)

        dut.io.Memport(1).valid.poke(true.B)
        dut.io.Memport(1).bits.wenable.poke(true.B)

        for(k <- 0 until n){
          dut.io.Memport(0).bits.writeData(k).poke(matrix(i)(k).U(8.W))
          dut.io.Memport(1).bits.writeData(k).poke(matrix(i)(k).U(8.W))
        }
   
        /* dut.io.State.poke(0.U)
        dut.io.Trigger.poke(false.B)
        dut.io.Size.poke(n.U) */

        //dut.io.in.valid.poke(false.B)
        //dut.io.in.bits.mode.poke(0.U)
        //dut.io.in.bits.size.poke(n.U)

        //dut.io.Readport.request.valid.poke(false.B)
  
        dut.clock.step()
      }

      dut.io.Memport(0).valid.poke(false.B)
      dut.io.Memport(1).valid.poke(false.B)



      dut.clock.step(10) 

      /* dut.io.Trigger.poke(true.B)
      dut.io.Size.poke(n.U) */

      dut.io.in.valid.poke(true.B)
      dut.io.in.bits.mode.poke(0.U)
      dut.io.in.bits.size.poke(n.U)

      dut.clock.step()

      dut.io.in.valid.poke(false.B)

      while (dut.io.completed.valid.peek().litToBoolean == false) {
        dut.clock.step()
      } 

      dut.io.completed.valid.expect(true.B)

      //dut.clock.step(80)

      dut.io.Readport.request.ready.expect(true.B)
      
      dut.io.Readport.request.valid.poke(true.B)
      dut.io.Readport.request.bits.addr.poke(0.U)

      val expectedResult = matrixDotProduct(matrix, matrix)

      dut.io.Readport.response.valid.expect(true.B)

      for (row <- 0 until n) {
        for (col <- 0 until n) {
          //dut.io.scratchOut.data.bits.writeData(col).expect(expectedResult(row)(col).U(8.W))
          //dut.io.Readport.response.bits.readData(col).expect(expectedResult(row)(col).U(8.W))
        }
        
        //dut.io.scratchOut.data.bits.writeData.expect(VecInit(expectedResult(row).map(_.U(8.W))))

        dut.clock.step()
      }

      //dut.clock.step(16)

      dut.io.Readport.request.valid.poke(false.B)

      //dut.clock.step(30)

			/* val product = Array.ofDim[Int](16, 16)

			for (i <- 0 until 16) {
				for (j <- 0 until 16) {
					var sum = 0
					for (k <- 0 until 16) {
						sum += matrix(i)(k) * matrix(k)(j)
					}
					product(i)(j) = sum
				}
			}

			// Print the product matrix
			for (row <- product) {
				println(row.mkString("\t"))
			} */
    }
  }
}  
