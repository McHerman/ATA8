package ATA8

import chisel3._
import chiseltest._
import org.scalatest.FlatSpec
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.experimental.ChiselEnum

import scala.util.Random


class DecodeTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "Decode"

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

	val exeInst = assembleInstruction(
		"0000 0000 0000 0011",
		"0000 0000 0000 0010",
		"0000 0000 0000 0001", 
		"0000 1000",
		"1", "0", "01","0001"
	)

	val loadInst = assembleInstruction(
		"0000 0000 0000 0000 0000 0000 0000 0100",
		"0000 1000",
		"0000 1000 0000 0000 00", //fill 18
		"0", "1","0010"
	)

	val storeInst = assembleInstruction(
		"0000 0000 0000 0000 0000 0000 0000 0000",
		"0000 1000",
		"0000 1000 0000 0000 000", //fill 19
		"1","0011"
	)



  //implicit val Config = Configuration.default().copy(issueQueueSize = n, simulation = true)

  it should "decode" in {
    //val n = 2 + Random.nextInt(30)
    //val n = 16

    test(new Decoder(Configuration.test())).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>
			dut.clock.step(1)

			/* dut.io.instructionStream.ready.expect(true.B)
			dut.io.instructionStream.valid.poke(true.B)
			dut.io.instructionStream.bits.instruction.poke(exeInst.U) */

			dut.io.instructionStream.request.ready.poke(true.B)
			dut.io.instructionStream.request.valid.expect(true.B)
			dut.io.instructionStream.response.bits.readData.instruction.poke(exeInst.U)
			dut.io.instructionStream.response.valid.poke(true.B)

			dut.clock.step(1)

			dut.io.instructionStream.response.valid.poke(false.B)

			dut.io.exeStream.ready.poke(true.B)

			//////////////// ROB STUFF ////////////////

			dut.io.tagFetch(0).request.valid.expect(true.B)
			dut.io.tagFetch(1).request.valid.expect(true.B)

			dut.io.tagFetch(0).response.valid.poke(false.B)
			dut.io.tagFetch(1).response.valid.poke(false.B)


			dut.io.tagRegister.addr.ready.poke(true.B)
			dut.io.tagRegister.addr.valid.expect(true.B)
			dut.io.tagRegister.addr.bits.addr.expect(3.U)

			dut.io.tagRegister.tag.valid.poke(true.B)
			dut.io.tagRegister.tag.bits.poke(1.U)

			//////////////// - ////////////////

			/* dut.io.instructionStream.ready.expect(true.B)
			dut.io.instructionStream.valid.poke(true.B)
			dut.io.instructionStream.bits.instruction.poke(loadInst.U) */

			dut.io.instructionStream.request.ready.poke(true.B)
			dut.io.instructionStream.request.valid.expect(true.B)
			dut.io.instructionStream.response.bits.readData.instruction.poke(loadInst.U)
			dut.io.instructionStream.response.valid.poke(true.B)

			dut.clock.step(1)

			dut.io.instructionStream.response.valid.poke(false.B)

			dut.io.loadStream.ready.poke(true.B)

			/* dut.io.instructionStream.ready.expect(true.B)
			dut.io.instructionStream.valid.poke(true.B)
			dut.io.instructionStream.bits.instruction.poke(storeInst.U) */

			dut.io.instructionStream.request.ready.poke(true.B)
			dut.io.instructionStream.request.valid.expect(true.B)
			dut.io.instructionStream.response.bits.readData.instruction.poke(storeInst.U)
			dut.io.instructionStream.response.valid.poke(true.B)

			
			//////////////// ROB STUFF ////////////////

			dut.io.tagRegister.addr.ready.poke(true.B)
			dut.io.tagRegister.addr.valid.expect(true.B)
			dut.io.tagRegister.addr.bits.addr.expect(4.U)

			dut.io.tagRegister.tag.valid.poke(true.B)
			dut.io.tagRegister.tag.bits.poke(2.U)

			//////////////// OUTPUT STUFF ////////////////

			dut.io.exeStream.ready.poke(true.B)
			dut.io.exeStream.valid.expect(true.B)

			dut.io.exeStream.bits.size.expect(8.U)

			dut.clock.step(1)

			dut.io.instructionStream.response.valid.poke(false.B)

			dut.io.storeStream.ready.poke(true.B)

			//////////////// ROB STUFF ////////////////

			dut.io.tagFetch(0).request.valid.expect(true.B)
			dut.io.tagFetch(0).response.valid.poke(true.B)

			dut.io.tagFetch(0).response.bits.tag.poke(2.U)
			dut.io.tagFetch(0).response.bits.ready.poke(false.B)

			//////////////// OUTPUT STUFF ////////////////

			dut.io.loadStream.ready.poke(true.B)
			dut.io.loadStream.valid.expect(true.B)
			
			dut.io.loadStream.bits.size.expect(8.U)

			
			dut.clock.step(1)

			//////////////// OUTPUT STUFF ////////////////

			dut.io.storeStream.ready.poke(true.B)
			dut.io.storeStream.valid.expect(true.B)

			dut.io.storeStream.bits.size.expect(8.U)

			


    }
  }
}
