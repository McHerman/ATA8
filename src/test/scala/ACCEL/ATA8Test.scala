package ATA8

import chisel3._
import chiseltest._
import org.scalatest.FlatSpec
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.experimental.ChiselEnum
import scala.io.Source

import scala.util.Random

//import org.scalatest.concurrent.Eventually._
//import org.scalatest.time.{Millis, Span}


class ATA8Test extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "ATA8"

  //val n = 2 + Random.nextInt(30)
  val n = 8

	val matrix: Seq[UInt] = Seq(
		"h0102030401020304".U(64.W),
		"h0102030401020304".U(64.W),
		"h0102030401020304".U(64.W),
		"h0102030401020304".U(64.W),
		"h0102030401020304".U(64.W),
		"h0102030401020304".U(64.W),
		"h0102030401020304".U(64.W),
		"h0102030401020304".U(64.W)

		// ... add the rest of the rows
	)

  // "h14283C5014283C50".U(64.W),

  val resultMatrix: Seq[UInt] = Seq(
		"h14283C5014283C50".U(64.W),
		"h14283C5014283C50".U(64.W),
		"h14283C5014283C50".U(64.W),
		"h14283C5014283C50".U(64.W),
		"h14283C5014283C50".U(64.W),
		"h14283C5014283C50".U(64.W),
		"h14283C5014283C50".U(64.W),
		"h14283C5014283C50".U(64.W)

		// ... add the rest of the rows
	)

  val matrix3: Array[Array[Int]] = Array(
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

	// Usage

  def assembleInstruction(fields: String*): BigInt = {
		val concatenated = fields.mkString.replace(" ", "")  // Concatenate all fields and remove spaces
		BigInt(concatenated, 2)  // Convert binary string to BigInt
	}

	val exeInst = assembleInstruction(
		"0000 0000 0000 0011",
		"0000 0000 0000 0010",
		"0000 0000 0000 0001", 
		"0000 0111",
		"1", "0", "01","0001"
	)

	val loadInst = assembleInstruction(
		"0000 0000 0000 0000 0000 0000 0000 0100",
		"0000 0100",
		"0000 1000 0000 0000 00", //fill 18
		"0", "1","0010"
	)

	val storeInst = assembleInstruction(
		"0000 0000 0000 0000 0000 0000 0000 0000",
		"0000 0100",
		"0000 1000 0000 0000 000", //fill 19
		"1","11","0011"
	)



  //implicit val Config = Configuration.default().copy(issueQueueSize = n, simulation = true)

  it should "execute" in {
    //val n = 2 + Random.nextInt(30)
    //val n = 16

    test(new ATA8(Configuration.default())).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>
      val source = Source.fromFile("output.txt")

      dut.io.AXIST_inInst.tready.expect(true.B)
      dut.io.AXIST_inData.tready.expect(false.B)
      
      dut.io.AXIST_out.tvalid.expect(false.B)

      for (instruction <- source.getLines()) {
        dut.clock.step(Random.nextInt(10))
        
        dut.io.AXIST_inInst.tready.expect(true.B)

        val instBigInt = BigInt(instruction, 2)

        dut.io.AXIST_inInst.tready.expect(true.B)
        dut.io.AXIST_inInst.tvalid.poke(true.B)
        dut.io.AXIST_inInst.tstrb.poke(255.U)

        dut.io.AXIST_inInst.tdata.poke(instBigInt.U(64.W))
        //c.io.instruction.poke(instBigInt.U(64.W))

        dut.clock.step(1)

        dut.io.AXIST_inInst.tvalid.poke(false.B)
      }

      println("instructions written")

      dut.io.AXIST_inInst.tlast.poke(true.B)
      
      dut.clock.step(1)

      dut.io.AXIST_inInst.tvalid.poke(false.B)
      dut.io.AXIST_inInst.tlast.poke(false.B)

      while (dut.io.AXIST_inData.tready.peek().litToBoolean == false) {
        dut.clock.step()
      } 

      println("addr1 loaded")

      for (row <- 0 until n) {
				dut.io.AXIST_inData.tdata.poke(matrix(row))
				dut.io.AXIST_inData.tstrb.poke(255.U)
        dut.io.AXIST_inData.tvalid.poke(true.B)

				if (row == n - 1) {
					dut.io.AXIST_inData.tlast.poke(true.B)  // assert tlast on the last line of the matrix
				}
				dut.clock.step(1)
			}

      dut.io.AXIST_inData.tready.expect(false.B)

      dut.io.AXIST_inData.tvalid.poke(false.B)
      dut.io.AXIST_inData.tlast.poke(false.B)

      while (dut.io.AXIST_inData.tready.peek().litToBoolean == false) {
        dut.clock.step()
      } 

      println("addr2 loaded")

      for (row <- 0 until n) {
				dut.io.AXIST_inData.tdata.poke(matrix(row))
				dut.io.AXIST_inData.tstrb.poke(255.U)
        dut.io.AXIST_inData.tvalid.poke(true.B)

				if (row == n - 1) {
					dut.io.AXIST_inData.tlast.poke(true.B)  // assert tlast on the last line of the matrix
				}
				dut.clock.step(1)
			}

      dut.io.AXIST_inData.tready.expect(false.B)

      dut.io.AXIST_inInst.tvalid.poke(false.B)
      dut.io.AXIST_inInst.tlast.poke(false.B)

      dut.io.AXIST_out.tready.poke(true.B)

      while (dut.io.AXIST_out.tvalid.peek().litToBoolean == false) {
        dut.clock.step()
      } 

      for (row <- 0 until n) {
				//dut.io.AXIST_inData.tdata.poke(matrix(row))
				dut.io.AXIST_out.tdata.expect(resultMatrix(row))

				if (row == n - 1) {
					dut.io.AXIST_out.tlast.expect(true.B)  // assert tlast on the last line of the matrix
				}
				dut.clock.step(1)
			}
    }
  }
}
