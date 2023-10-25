package ATA8

import chisel3._
import chiseltest._
import org.scalatest.FlatSpec
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.experimental.ChiselEnum

import scala.util.Random


class LoadTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "Load"

  //val n = 2 + Random.nextInt(30)
  val n = 8

	def assembleInstruction(fields: String*): BigInt = {
		val concatenated = fields.mkString.replace(" ", "")  // Concatenate all fields and remove spaces
		BigInt(concatenated, 2)  // Convert binary string to BigInt
	}

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


  //implicit val Config = Configuration.default().copy(issueQueueSize = n, simulation = true)

  it should "load" in {
    //val n = 2 + Random.nextInt(30)
    //val n = 16

    test(new Load(Configuration.test())).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>
			dut.io.instructionStream.ready.expect(true.B)
			dut.io.instructionStream.valid.poke(true.B)

			//dut.io.instructionStream.bits.instruction.poke(instruction1.U)

      dut.io.instructionStream.bits.op.poke(0.U)
      dut.io.instructionStream.bits.mode.poke(0.U)

      dut.io.instructionStream.bits.size.poke(8.U)

      dut.io.instructionStream.bits.addr.addr.poke(1.U)
      dut.io.instructionStream.bits.addr.tag.poke(1.U)

			dut.clock.step(10)

      /*
			dut.io.instructionStream.ready.expect(true.B)
			dut.io.instructionStream.valid.poke(true.B)
			
      //dut.io.instructionStream.bits.instruction.poke(instruction1.U)

      dut.io.instructionStream.bits.op.poke(0.U)
      dut.io.instructionStream.bits.mode.poke(0.U)

      dut.io.instructionStream.bits.size.poke(8.U)

      dut.io.instructionStream.bits.addr.addr.poke(16.U)
      dut.io.instructionStream.bits.addr.tag.poke(2.U)

			dut.clock.step(1)

			dut.io.instructionStream.ready.expect(true.B)
			dut.io.instructionStream.valid.poke(true.B)
			
      //dut.io.instructionStream.bits.instruction.poke(instruction1.U)

      dut.io.instructionStream.bits.op.poke(0.U)
      dut.io.instructionStream.bits.mode.poke(0.U)

      dut.io.instructionStream.bits.size.poke(8.U)

      dut.io.instructionStream.bits.addr.addr.poke(24.U)
      dut.io.instructionStream.bits.addr.tag.poke(3.U)

      dut.clock.step(1)

      */

      dut.io.scratchOut.request.ready.poke(true.B)

      while (dut.io.scratchOut.request.valid.peek().litToBoolean == false) {
        dut.clock.step()
      } 

      dut.io.scratchOut.request.valid.expect(true.B)

      dut.io.scratchOut.request.bits.addr.expect(1.U)
      dut.io.scratchOut.request.bits.burst.expect(8.U)

      dut.clock.step(1)

      dut.io.scratchOut.data.ready.poke(true.B)

      dut.io.AXIST.tready.expect(true.B)
			dut.io.AXIST.tvalid.poke(true.B)

			for (row <- 0 until n) {
				dut.io.AXIST.tdata.poke(matrix(row))
				dut.io.AXIST.tstrb.poke(255.U)

        for (col <- 0 until n) {
          dut.io.scratchOut.data.bits.writeData(col).expect(matrix3(row)((n-1)-col).U(8.W))
        }

				if (row == n - 1) {
					dut.io.AXIST.tlast.poke(true.B)  // assert tlast on the last line of the matrix
          dut.io.scratchOut.data.bits.last.expect(true.B)
				}
				dut.clock.step(1)
			}

      dut.clock.step(1)

      dut.io.event.valid.expect(true.B)
      dut.io.event.bits.tag.expect(1.U)

      /*

			dut.io.AXIST.tvalid.poke(false.B)
			dut.io.AXIST.tlast.poke(false.B)

			dut.io.AXIST.tready.expect(false.B)

			dut.clock.step(1)

			dut.io.AXIST.tvalid.poke(false.B)
			dut.io.AXIST.tlast.poke(false.B)

			dut.io.AXIST.tready.expect(false.B)

      dut.io.event.valid.expect(true.B)
      dut.io.event.bits.tag.expect(true.B)

			dut.clock.step(1)

			dut.io.AXIST.tready.expect(true.B)

			dut.io.AXIST.tvalid.poke(true.B)

			for (i <- 0 until n) {
				dut.io.AXIST.tdata.poke(matrix(i))
				dut.io.AXIST.tstrb.poke(255.U)
				if (i == n - 1) {
					dut.io.AXIST.tlast.poke(true.B)  // assert tlast on the last line of the matrix
				}
				dut.clock.step(1)
			}

			dut.io.AXIST.tvalid.poke(false.B)
			dut.io.AXIST.tlast.poke(false.B)

			dut.io.AXIST.tready.expect(false.B)

			dut.clock.step(1)

			dut.io.AXIST.tvalid.poke(false.B)
			dut.io.AXIST.tlast.poke(false.B)

			dut.io.AXIST.tready.expect(false.B)

			dut.clock.step(1) 

			dut.io.AXIST.tready.expect(true.B)

			dut.io.AXIST.tvalid.poke(true.B)

			for (i <- 0 until n) {
				dut.io.AXIST.tdata.poke(matrix(i))
				dut.io.AXIST.tstrb.poke(255.U)
				if (i == n - 1) {
					dut.io.AXIST.tlast.poke(true.B)  // assert tlast on the last line of the matrix
				}
				dut.clock.step(1)
			}
			

      */


			



    }
  }
}