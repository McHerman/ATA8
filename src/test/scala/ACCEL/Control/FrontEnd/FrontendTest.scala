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


class FrontendTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "FrontEnd"

  //val n = 2 + Random.nextInt(30)
  val n = 8

	// Usage

  def assembleExeInstruction(
      field1: Int, // 16 bits
      field2: Int, // 16 bits
      field3: Int, // 16 bits
      field4: Int, // 8 bits
      field5: Int, // 1 bit
      field6: Int, // 1 bit
      field7: Int, // 2 bits
      field8: Int  // 4 bits
  ): BigInt = {
    // Ensure the binary string for each field is left-padded with zeros
    val field1Binary = field1.toBinaryString.reverse.padTo(16, '0').reverse
    val field2Binary = field2.toBinaryString.reverse.padTo(16, '0').reverse
    val field3Binary = field3.toBinaryString.reverse.padTo(16, '0').reverse
    val field4Binary = field4.toBinaryString.reverse.padTo(8, '0').reverse
    val field5Binary = field5.toBinaryString.reverse.padTo(1, '0').reverse
    val field6Binary = field6.toBinaryString.reverse.padTo(1, '0').reverse
    val field7Binary = field7.toBinaryString.reverse.padTo(2, '0').reverse
    val field8Binary = field8.toBinaryString.reverse.padTo(4, '0').reverse

    // Concatenate all binary strings
    val concatenated = s"$field1Binary$field2Binary$field3Binary$field4Binary$field5Binary$field6Binary$field7Binary$field8Binary"

    // Convert concatenated binary string to BigInt
    BigInt(concatenated, 2)
  }

  def assembleLoadInstruction(
      field1: Int, // 32 bits
      field2: Int, // 8 bits
      field3: Int, // 18 bits
      field4: Int, // 1 bits
      field5: Int, // 1 bit
      field6: Int, // 4 bit
  ): BigInt = {
    // Ensure the binary string for each field is left-padded with zeros
    val field1Binary = field1.toBinaryString.reverse.padTo(32, '0').reverse
    val field2Binary = field2.toBinaryString.reverse.padTo(8, '0').reverse
    val field3Binary = field3.toBinaryString.reverse.padTo(18, '0').reverse
    val field4Binary = field4.toBinaryString.reverse.padTo(1, '0').reverse
    val field5Binary = field5.toBinaryString.reverse.padTo(1, '0').reverse
    val field6Binary = field6.toBinaryString.reverse.padTo(4, '0').reverse

    // Concatenate all binary strings
    val concatenated = s"$field1Binary$field2Binary$field3Binary$field4Binary$field5Binary$field6Binary"

    // Convert concatenated binary string to BigInt
    BigInt(concatenated, 2)
  }

  // Usage example with integers for each field



  //implicit val Config = Configuration.default().copy(issueQueueSize = n, simulation = true)

  it should "decode" in {
    //val n = 2 + Random.nextInt(30)
    //val n = 16

    implicit val c = Configuration.test()

    test(new FrontEnd()).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>

      dut.io.exeStream.ready.poke(false.B)
      dut.io.loadStream.ready.poke(false.B)
      dut.io.storeStream.ready.poke(false.B)


      for(i <- 0 until 16){
        dut.io.AXIST.tready.expect(true.B)

        val exeInst = assembleExeInstruction(
          i,   // Replace with integer for the first 16-bit field
          31,   // Replace with integer for the second 16-bit field
          31,   // Replace with integer for the third 16-bit field
          7,   // Replace with integer for the 8-bit field
          1,   // Replace with integer for the first 1-bit field
          0,   // Replace with integer for the second 1-bit field
          1,   // Replace with integer for the 2-bit field
          1    // Replace with integer for the 4-bit field
        )

        println(exeInst)

        dut.io.AXIST.tdata.poke(exeInst.U(64.W))
        dut.io.AXIST.tvalid.poke(true.B)
        dut.io.AXIST.tkeep.poke("hff".U)

        dut.clock.step(1)
      }

      //dut.clock.step(1)

      for(i <- 0 until 8){
        dut.io.exeStream.ready.poke(true.B)
        dut.io.exeStream.valid.expect(true.B)

        dut.io.exeStream.bits.addrs(0).addr.expect(31.U)
        dut.io.exeStream.bits.addrs(0).depend.ready.expect(true.B)

        dut.io.exeStream.bits.addrs(1).addr.expect(31.U)
        dut.io.exeStream.bits.addrs(1).depend.ready.expect(true.B)

        dut.io.exeStream.bits.addrd(0).addr.expect(i.U)
        dut.io.exeStream.bits.addrd(0).tag.expect(i.U)

        dut.clock.step(1)
      }

      dut.io.exeStream.valid.expect(false.B)

      dut.clock.step(1) 

      dut.io.exeStream.ready.poke(false.B)

      for(i <- 0 until 8){
        dut.io.event(0).valid.poke(true.B)
        dut.io.event(0).bits.tag.poke(i.U)

        dut.clock.step(1) 
      }


      for(i <- 0 until 8){
        dut.io.exeStream.ready.poke(true.B)
        dut.io.exeStream.valid.expect(true.B)

        dut.io.exeStream.bits.addrs(0).addr.expect(31.U)
        dut.io.exeStream.bits.addrs(0).depend.ready.expect(true.B)

        dut.io.exeStream.bits.addrs(1).addr.expect(31.U)
        dut.io.exeStream.bits.addrs(1).depend.ready.expect(true.B)

        dut.io.exeStream.bits.addrd(0).addr.expect((i+8).U)
        dut.io.exeStream.bits.addrd(0).tag.expect(i.U)

        dut.clock.step(1)
      }
    }
  }


  it should "decode multible instructions" in {
    //val n = 2 + Random.nextInt(30)
    //val n = 16

    implicit val c = Configuration.test()

    test(new FrontEnd()).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>

      dut.io.exeStream.ready.poke(false.B)
      dut.io.loadStream.ready.poke(false.B)
      dut.io.storeStream.ready.poke(false.B)


      for(i <- 0 until 8){
        dut.io.AXIST.tready.expect(true.B)

        val exeInst = assembleExeInstruction(
          i,   // Replace with integer for the first 16-bit field
          31,   // Replace with integer for the second 16-bit field
          31,   // Replace with integer for the third 16-bit field
          7,   // Replace with integer for the 8-bit field
          1,   // Replace with integer for the first 1-bit field
          0,   // Replace with integer for the second 1-bit field
          1,   // Replace with integer for the 2-bit field
          1    // Replace with integer for the 4-bit field
        )

        //println(exeInst)

        dut.io.AXIST.tdata.poke(exeInst.U(64.W))
        dut.io.AXIST.tvalid.poke(true.B)
        dut.io.AXIST.tkeep.poke("hff".U)

        dut.clock.step(1)
      }

      for(i <- 0 until 8){
        dut.io.AXIST.tready.expect(true.B)

        val loadInst = assembleLoadInstruction(
          (8+i),   // addr 32 
          7,   // size 8 
          0,   // fill 18
          0,   // mode 1 
          0,   // op 1 
          2    // func 4 
        )

        //println(loadInst)
 
        dut.io.AXIST.tdata.poke(loadInst.U(64.W))
        dut.io.AXIST.tvalid.poke(true.B)
        dut.io.AXIST.tkeep.poke("hff".U)

        dut.clock.step(1)
      }

      //dut.clock.step(1)

      for(i <- 0 until 8){
        dut.io.exeStream.ready.poke(true.B)
        dut.io.exeStream.valid.expect(true.B)

        dut.io.exeStream.bits.addrs(0).addr.expect(31.U)
        dut.io.exeStream.bits.addrs(0).depend.ready.expect(true.B)

        dut.io.exeStream.bits.addrs(1).addr.expect(31.U)
        dut.io.exeStream.bits.addrs(1).depend.ready.expect(true.B)

        dut.io.exeStream.bits.addrd(0).addr.expect(i.U)
        dut.io.exeStream.bits.addrd(0).tag.expect(i.U)

        dut.clock.step(1)
      }

      dut.io.exeStream.valid.expect(false.B)

      dut.clock.step(1) 

      dut.io.exeStream.ready.poke(false.B)

      for(i <- 0 until 8){
        dut.io.event(0).valid.poke(true.B)
        dut.io.event(0).bits.tag.poke(i.U)

        dut.clock.step(1) 
      }


      for(i <- 0 until 8){
        dut.io.loadStream.ready.poke(true.B)
        dut.io.loadStream.valid.expect(true.B)

        //dut.io.loadStream.bits.addrs(0).addr.expect(31.U)
        //dut.io.loadStream.bits.addrs(0).depend.ready.expect(true.B)

        //dut.io.loadStream.bits.addrs(1).addr.expect(31.U)
        //dut.io.loadStream.bits.addrs(1).depend.ready.expect(true.B)

        dut.io.loadStream.bits.addrd(0).addr.expect((i+8).U)
        dut.io.loadStream.bits.addrd(0).tag.expect(i.U)

        dut.clock.step(1)
      }
    }
  }
}
