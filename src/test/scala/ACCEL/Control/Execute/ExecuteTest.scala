package ATA8

import chisel3._
import chiseltest._
import org.scalatest.FlatSpec
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.experimental.ChiselEnum

import scala.util.Random


class ExecuteTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "Execute"

  //val n = 2 + Random.nextInt(30)
  val n = 8

	def assembleInstruction(fields: String*): BigInt = {
		val concatenated = fields.mkString.replace(" ", "")  // Concatenate all fields and remove spaces
		BigInt(concatenated, 2)  // Convert binary string to BigInt
	}

	// Usage
	val instruction1 = assembleInstruction(
		"0000 0000 0000 0001",
		"0000 0000 0000 0000",
		"0000 0000 0000 1000",
		"0000 0111",
		"0001 0001"
	)

	val instruction2 = assembleInstruction(
		"0000 0000 0000 0010",
		"0000 0000 0000 0001",
		"0000 0000 0000 1001",
		"0000 0111",
		"0001 0001"
	)

	val instruction3 = assembleInstruction(
		"0000 0000 0000 0011",
		"0000 0000 0000 0010",
		"0000 0000 0000 1010",
		"0000 0111",
		"0001 0001"
	)



  //implicit val Config = Configuration.default().copy(issueQueueSize = n, simulation = true)

  it should "issue in order" in {
    //val n = 2 + Random.nextInt(30)
    //val n = 16

    test(new Execute(Configuration.test())).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>
			dut.io.instructionStream.ready.expect(true.B)
			dut.io.instructionStream.valid.poke(true.B)
			dut.io.instructionStream.bits.instruction.poke(instruction1.U)

			dut.io.tagFetchfromLoad(0).request.valid.expect(true.B)
			dut.io.tagFetchfromLoad(1).request.valid.expect(true.B)

			dut.io.tagFetchfromLoad(0).request.bits.addr.expect(0.U)
			dut.io.tagFetchfromLoad(1).request.bits.addr.expect(1.U)

			dut.io.tagFetchfromLoad(0).response.valid.poke(true.B)
			dut.io.tagFetchfromLoad(1).response.valid.poke(true.B)

			dut.io.tagFetchfromLoad(0).response.bits.tag.poke(8.U)
			dut.io.tagFetchfromLoad(1).response.bits.tag.poke(9.U)
			
			dut.io.tagFetchfromLoad(0).response.bits.ready.poke(true.B)
			dut.io.tagFetchfromLoad(1).response.bits.ready.poke(false.B)

			dut.clock.step(1)

			dut.io.instructionStream.ready.expect(true.B)
			dut.io.instructionStream.valid.poke(true.B)
			dut.io.instructionStream.bits.instruction.poke(instruction2.U)

			dut.io.tagFetchfromLoad(0).request.valid.expect(true.B)
			dut.io.tagFetchfromLoad(1).request.valid.expect(true.B)

			dut.io.tagFetchfromLoad(0).request.bits.addr.expect(1.U)
			dut.io.tagFetchfromLoad(1).request.bits.addr.expect(2.U)

			dut.io.tagFetchfromLoad(0).response.valid.poke(true.B)
			dut.io.tagFetchfromLoad(1).response.valid.poke(true.B)

			dut.io.tagFetchfromLoad(0).response.bits.tag.poke(10.U)
			dut.io.tagFetchfromLoad(1).response.bits.tag.poke(11.U)
			
			dut.io.tagFetchfromLoad(0).response.bits.ready.poke(true.B)
			dut.io.tagFetchfromLoad(1).response.bits.ready.poke(false.B)

			dut.clock.step(1)

			dut.io.instructionStream.ready.expect(true.B)
			dut.io.instructionStream.valid.poke(true.B)
			dut.io.instructionStream.bits.instruction.poke(instruction3.U)

			dut.io.tagFetchfromLoad(0).request.valid.expect(true.B)
			dut.io.tagFetchfromLoad(1).request.valid.expect(true.B)

			dut.io.tagFetchfromLoad(0).request.bits.addr.expect(2.U)
			dut.io.tagFetchfromLoad(1).request.bits.addr.expect(3.U)

			dut.io.tagFetchfromLoad(0).response.valid.poke(true.B)
			dut.io.tagFetchfromLoad(1).response.valid.poke(true.B)

			dut.io.tagFetchfromLoad(0).response.bits.tag.poke(12.U)
			dut.io.tagFetchfromLoad(1).response.bits.tag.poke(13.U)
			
			dut.io.tagFetchfromLoad(0).response.bits.ready.poke(true.B)
			dut.io.tagFetchfromLoad(1).response.bits.ready.poke(false.B)

			dut.clock.step(1)

			dut.io.instructionStream.ready.expect(true.B)
			dut.io.instructionStream.valid.poke(false.B)

			dut.clock.step(1)

			dut.io.tagFetchfromLoad(0).request.valid.expect(false.B)
			dut.io.tagFetchfromLoad(1).request.valid.expect(false.B)

			dut.io.event.valid.poke(true.B)
			dut.io.event.bits.tag.poke(9.U)

			dut.clock.step(1)

			dut.io.DMARead(0).ready.poke(true.B)
			dut.io.DMARead(0).valid.expect(true.B)

			dut.io.DMARead(0).bits.ids(0).tag.expect(8.U)
			dut.io.DMARead(0).bits.ids(1).tag.expect(9.U)
			
			dut.io.event.valid.poke(true.B)
			dut.io.event.bits.tag.poke(11.U)

			dut.clock.step(1)

			dut.io.DMARead(0).ready.poke(true.B)
			dut.io.DMARead(0).valid.expect(true.B)

			dut.io.DMARead(0).bits.ids(0).tag.expect(10.U)
			dut.io.DMARead(0).bits.ids(1).tag.expect(11.U)
			
			dut.io.event.valid.poke(true.B)
			dut.io.event.bits.tag.poke(13.U)

			dut.clock.step(1)

			dut.io.DMARead(0).ready.poke(true.B)
			dut.io.DMARead(0).valid.expect(true.B)

			dut.io.DMARead(0).bits.ids(0).tag.expect(12.U)
			dut.io.DMARead(0).bits.ids(1).tag.expect(13.U)
			
			//dut.io.event.valid.poke(true.B)
			//dut.io.event.bits.tag.poke(13.U)


    }
  }

	it should "issue out of order" in {
    //val n = 2 + Random.nextInt(30)
    //val n = 16

    test(new Execute(Configuration.test())).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>
			dut.io.instructionStream.ready.expect(true.B)
			dut.io.instructionStream.valid.poke(true.B)
			dut.io.instructionStream.bits.instruction.poke(instruction1.U)

			dut.io.tagFetchfromLoad(0).request.valid.expect(true.B)
			dut.io.tagFetchfromLoad(1).request.valid.expect(true.B)

			dut.io.tagFetchfromLoad(0).request.bits.addr.expect(0.U)
			dut.io.tagFetchfromLoad(1).request.bits.addr.expect(1.U)

			dut.io.tagFetchfromLoad(0).response.valid.poke(true.B)
			dut.io.tagFetchfromLoad(1).response.valid.poke(true.B)

			dut.io.tagFetchfromLoad(0).response.bits.tag.poke(8.U)
			dut.io.tagFetchfromLoad(1).response.bits.tag.poke(9.U)
			
			dut.io.tagFetchfromLoad(0).response.bits.ready.poke(true.B)
			dut.io.tagFetchfromLoad(1).response.bits.ready.poke(false.B)

			dut.clock.step(1)

			dut.io.instructionStream.ready.expect(true.B)
			dut.io.instructionStream.valid.poke(true.B)
			dut.io.instructionStream.bits.instruction.poke(instruction2.U)

			dut.io.tagFetchfromLoad(0).request.valid.expect(true.B)
			dut.io.tagFetchfromLoad(1).request.valid.expect(true.B)

			dut.io.tagFetchfromLoad(0).request.bits.addr.expect(1.U)
			dut.io.tagFetchfromLoad(1).request.bits.addr.expect(2.U)

			dut.io.tagFetchfromLoad(0).response.valid.poke(true.B)
			dut.io.tagFetchfromLoad(1).response.valid.poke(true.B)

			dut.io.tagFetchfromLoad(0).response.bits.tag.poke(10.U)
			dut.io.tagFetchfromLoad(1).response.bits.tag.poke(11.U)
			
			dut.io.tagFetchfromLoad(0).response.bits.ready.poke(true.B)
			dut.io.tagFetchfromLoad(1).response.bits.ready.poke(false.B)

			dut.clock.step(1)

			dut.io.instructionStream.ready.expect(true.B)
			dut.io.instructionStream.valid.poke(true.B)
			dut.io.instructionStream.bits.instruction.poke(instruction3.U)

			dut.io.tagFetchfromLoad(0).request.valid.expect(true.B)
			dut.io.tagFetchfromLoad(1).request.valid.expect(true.B)

			dut.io.tagFetchfromLoad(0).request.bits.addr.expect(2.U)
			dut.io.tagFetchfromLoad(1).request.bits.addr.expect(3.U)

			dut.io.tagFetchfromLoad(0).response.valid.poke(true.B)
			dut.io.tagFetchfromLoad(1).response.valid.poke(true.B)

			dut.io.tagFetchfromLoad(0).response.bits.tag.poke(12.U)
			dut.io.tagFetchfromLoad(1).response.bits.tag.poke(13.U)
			
			dut.io.tagFetchfromLoad(0).response.bits.ready.poke(true.B)
			dut.io.tagFetchfromLoad(1).response.bits.ready.poke(false.B)

			dut.clock.step(1)

			dut.io.instructionStream.ready.expect(true.B)
			dut.io.instructionStream.valid.poke(false.B)

			dut.clock.step(1)

			dut.io.tagFetchfromLoad(0).request.valid.expect(false.B)
			dut.io.tagFetchfromLoad(1).request.valid.expect(false.B)

			dut.io.event.valid.poke(true.B)
			dut.io.event.bits.tag.poke(11.U)

			dut.clock.step(1)

			dut.io.DMARead(0).ready.poke(true.B)
			dut.io.DMARead(0).valid.expect(true.B)

			dut.io.DMARead(0).bits.ids(0).tag.expect(10.U)
			dut.io.DMARead(0).bits.ids(1).tag.expect(11.U)
			
			dut.io.event.valid.poke(true.B)
			dut.io.event.bits.tag.poke(13.U)

			dut.clock.step(1)

			dut.io.DMARead(0).ready.poke(true.B)
			dut.io.DMARead(0).valid.expect(true.B)

			dut.io.DMARead(0).bits.ids(0).tag.expect(12.U)
			dut.io.DMARead(0).bits.ids(1).tag.expect(13.U)
			
			dut.io.event.valid.poke(true.B)
			dut.io.event.bits.tag.poke(9.U)

			dut.clock.step(1)

			dut.io.DMARead(0).ready.poke(true.B)
			dut.io.DMARead(0).valid.expect(true.B)

			dut.io.DMARead(0).bits.ids(0).tag.expect(8.U)
			dut.io.DMARead(0).bits.ids(1).tag.expect(9.U)
			//dut.io.event.valid.poke(true.B)
			//dut.io.event.bits.tag.poke(13.U)


    }
  }

  it should "stall" in {
    //val n = 2 + Random.nextInt(30)
    //val n = 16

    test(new Execute(Configuration.test())).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>
			dut.io.instructionStream.ready.expect(true.B)
			dut.io.instructionStream.valid.poke(true.B)
			dut.io.instructionStream.bits.instruction.poke(instruction1.U)

			dut.io.tagFetchfromLoad(0).request.valid.expect(true.B)
			dut.io.tagFetchfromLoad(1).request.valid.expect(true.B)

			dut.io.tagFetchfromLoad(0).request.bits.addr.expect(0.U)
			dut.io.tagFetchfromLoad(1).request.bits.addr.expect(1.U)

			dut.io.tagFetchfromLoad(0).response.valid.poke(true.B)
			dut.io.tagFetchfromLoad(1).response.valid.poke(false.B)

			dut.io.tagFetchfromLoad(0).response.bits.tag.poke(8.U)
			dut.io.tagFetchfromLoad(1).response.bits.tag.poke(9.U)
			
			dut.io.tagFetchfromLoad(0).response.bits.ready.poke(true.B)
			dut.io.tagFetchfromLoad(1).response.bits.ready.poke(false.B)

			dut.clock.step(1)

			dut.io.instructionStream.ready.expect(false.B)
			dut.io.instructionStream.valid.poke(true.B)
			//dut.io.instructionStream.bits.instruction.poke(instruction2.U)

			dut.io.tagFetchfromLoad(0).request.valid.expect(true.B)
			dut.io.tagFetchfromLoad(1).request.valid.expect(true.B)

			dut.io.tagFetchfromLoad(0).request.bits.addr.expect(0.U)
			dut.io.tagFetchfromLoad(1).request.bits.addr.expect(1.U)

			dut.io.tagFetchfromLoad(0).response.valid.poke(true.B)
			dut.io.tagFetchfromLoad(1).response.valid.poke(true.B)

			dut.io.tagFetchfromLoad(0).response.bits.tag.poke(8.U)
			dut.io.tagFetchfromLoad(1).response.bits.tag.poke(9.U)
			
			dut.io.tagFetchfromLoad(0).response.bits.ready.poke(true.B)
			dut.io.tagFetchfromLoad(1).response.bits.ready.poke(false.B)


      dut.clock.step(1)

			dut.io.instructionStream.ready.expect(true.B)
			dut.io.instructionStream.valid.poke(true.B)
			dut.io.instructionStream.bits.instruction.poke(instruction2.U)

			dut.io.tagFetchfromLoad(0).request.valid.expect(true.B)
			dut.io.tagFetchfromLoad(1).request.valid.expect(true.B)

			dut.io.tagFetchfromLoad(0).request.bits.addr.expect(1.U)
			dut.io.tagFetchfromLoad(1).request.bits.addr.expect(2.U)

			dut.io.tagFetchfromLoad(0).response.valid.poke(true.B)
			dut.io.tagFetchfromLoad(1).response.valid.poke(true.B)

			dut.io.tagFetchfromLoad(0).response.bits.tag.poke(10.U)
			dut.io.tagFetchfromLoad(1).response.bits.tag.poke(11.U)
			
			dut.io.tagFetchfromLoad(0).response.bits.ready.poke(true.B)
			dut.io.tagFetchfromLoad(1).response.bits.ready.poke(false.B)

			dut.clock.step(1)

			dut.io.instructionStream.ready.expect(true.B)
			dut.io.instructionStream.valid.poke(true.B)
			dut.io.instructionStream.bits.instruction.poke(instruction3.U)

			dut.io.tagFetchfromLoad(0).request.valid.expect(true.B)
			dut.io.tagFetchfromLoad(1).request.valid.expect(true.B)

			dut.io.tagFetchfromLoad(0).request.bits.addr.expect(2.U)
			dut.io.tagFetchfromLoad(1).request.bits.addr.expect(3.U)

			dut.io.tagFetchfromLoad(0).response.valid.poke(true.B)
			dut.io.tagFetchfromLoad(1).response.valid.poke(true.B)

			dut.io.tagFetchfromLoad(0).response.bits.tag.poke(12.U)
			dut.io.tagFetchfromLoad(1).response.bits.tag.poke(13.U)
			
			dut.io.tagFetchfromLoad(0).response.bits.ready.poke(true.B)
			dut.io.tagFetchfromLoad(1).response.bits.ready.poke(false.B)

			dut.clock.step(1)

			dut.io.instructionStream.ready.expect(true.B)
			dut.io.instructionStream.valid.poke(false.B)

			dut.clock.step(1)

			dut.io.tagFetchfromLoad(0).request.valid.expect(false.B)
			dut.io.tagFetchfromLoad(1).request.valid.expect(false.B)

			dut.io.event.valid.poke(true.B)
			dut.io.event.bits.tag.poke(9.U)

			dut.clock.step(1)

			dut.io.DMARead(0).ready.poke(true.B)
			dut.io.DMARead(0).valid.expect(true.B)

			dut.io.DMARead(0).bits.ids(0).tag.expect(8.U)
			dut.io.DMARead(0).bits.ids(1).tag.expect(9.U)
			
			dut.io.event.valid.poke(true.B)
			dut.io.event.bits.tag.poke(11.U)

			dut.clock.step(1)

			dut.io.DMARead(0).ready.poke(true.B)
			dut.io.DMARead(0).valid.expect(true.B)

			dut.io.DMARead(0).bits.ids(0).tag.expect(10.U)
			dut.io.DMARead(0).bits.ids(1).tag.expect(11.U)
			
			dut.io.event.valid.poke(true.B)
			dut.io.event.bits.tag.poke(13.U)

			dut.clock.step(1)

			dut.io.DMARead(0).ready.poke(true.B)
			dut.io.DMARead(0).valid.expect(true.B)

			dut.io.DMARead(0).bits.ids(0).tag.expect(12.U)
			dut.io.DMARead(0).bits.ids(1).tag.expect(13.U)

			

    }
  }
  
}
