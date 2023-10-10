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
		"h0102030405060708".U(64.W),
		"h0102030405060708".U(64.W),
		"h0102030405060708".U(64.W),
		"h0102030405060708".U(64.W),
		"h0102030405060708".U(64.W),
		"h0102030405060708".U(64.W),
		"h0102030405060708".U(64.W),
		"h0102030405060708".U(64.W)

		// ... add the rest of the rows
	)

	// Usage
	val instruction1 = assembleInstruction(
		"0000 0000 0000 0000",
		"0000 0000 0000 1000",
		"0000 0100",
		"0000 1000 0000 0111",
		"0001 00","01"
	)

	val instruction2 = assembleInstruction(
		"0000 0000 0000 0000",
		"0000 0000 1000 0000",
		"0000 0100",
		"0000 1000 0000 0111",
		"0001 00","01"
	)

	val instruction3 = assembleInstruction(
		"0000 0000 0000 0000",
		"0000 1000 0000 0000",
		"0000 0100",
		"0000 1000 0000 0111",
		"0001 00","01"
	)



  //implicit val Config = Configuration.default().copy(issueQueueSize = n, simulation = true)

  it should "load" in {
    //val n = 2 + Random.nextInt(30)
    //val n = 16

    test(new Load(Configuration.test())).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>
			dut.io.instructionStream.ready.expect(true.B)
			dut.io.instructionStream.valid.poke(true.B)
			dut.io.instructionStream.bits.instruction.poke(instruction1.U)
			
			dut.clock.step(1)

			dut.io.instructionStream.ready.expect(true.B)
			dut.io.instructionStream.valid.poke(true.B)
			dut.io.instructionStream.bits.instruction.poke(instruction1.U)

			dut.clock.step(1)

			dut.io.instructionStream.ready.expect(true.B)
			dut.io.instructionStream.valid.poke(true.B)
			dut.io.instructionStream.bits.instruction.poke(instruction1.U)

			dut.io.AXIST.tready.expect(true.B)

			dut.clock.step(1)

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
			


			



    }
  }
}
