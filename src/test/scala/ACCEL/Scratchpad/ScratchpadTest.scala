package ATA8

import chisel3._
import chiseltest._
import org.scalatest.FlatSpec
import org.scalatest.flatspec.AnyFlatSpec

import scala.util.Random
import scala.io.Source

class ScratchpadTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "ScratchpadWrapper"
  
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

  //implicit val Config = Configuration.default()

	
  "Scratchpad" should "read alligned" in {
    implicit val c = Configuration.test()
    //test(new Grain(n,n,8)).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>
    test(new ScratchpadWrapper()).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>

      dut.io.Writeport(0).request.ready.expect(true.B)

      dut.io.Writeport(0).request.bits.addr.poke(0.U)
      //dut.io.Writeport(0).request.bits.burst.poke(n.U)
      dut.io.Writeport(0).request.bits.burstSize.poke(n.U)
      dut.io.Writeport(0).request.bits.burstCnt.poke(n.U)

      dut.io.Writeport(0).request.valid.poke(true.B)

      dut.clock.step(1)

      dut.io.Writeport(0).request.valid.poke(false.B)

      for(i <- 0 until n){
        dut.io.Writeport(0).data.ready.expect(true.B)

        for(k <- 0 until n){
          dut.io.Writeport(0).data.bits.writeData(k).poke(WMatrix(i)(k).U)
          dut.io.Writeport(0).data.bits.strb(k).poke(true.B)
        }

        dut.io.Writeport(0).data.valid.poke(true.B)

        if(i == (n-1)){
          dut.io.Writeport(0).data.bits.last.poke(true.B)
        }

        dut.clock.step(1)
      }

      dut.io.Writeport(0).data.valid.poke(false.B)

      dut.clock.step(1)

      dut.io.Readport(0).request.ready.expect(true.B)

      dut.io.Readport(0).request.bits.addr.poke(0.U)
      //dut.io.Readport(0).request.bits.burst.poke(n.U)
      dut.io.Readport(0).request.bits.burstSize.poke(n.U)
      dut.io.Readport(0).request.bits.burstCnt.poke(n.U)

      dut.io.Readport(0).request.valid.poke(true.B)

      dut.clock.step(1)

      dut.io.Readport(0).data.ready.poke(true.B)


      while (dut.io.Readport(0).data.valid.peek().litToBoolean == false) {
        dut.clock.step()
      } 

      dut.io.Readport(0).data.valid.expect(true.B)

      for(i <- 0 until n){

        for(k <- 0 until n){
          dut.io.Readport(0).data.bits.readData(k).expect(WMatrix(i)(k).U)
          //dut.io.Writeport(0).data.bits.strb(k).poke(true.B)
        }

        //dut.io.Writeport(0).data.valid.poke(true.B)

        if(i == (n-1)){
          dut.io.Readport(0).data.bits.last.expect(true.B)
        }

        dut.clock.step(1)
      }
    }
  }

  "Scratchpad" should "read unalligned" in {
    implicit val c = Configuration.test()
    //test(new Grain(n,n,8)).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>
    test(new ScratchpadWrapper()).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>

      dut.io.Writeport(0).request.ready.expect(true.B)

      dut.io.Writeport(0).request.bits.addr.poke(1.U)
      //dut.io.Writeport(0).request.bits.burst.poke(n.U)
      dut.io.Writeport(0).request.bits.burstSize.poke(n.U)
      dut.io.Writeport(0).request.bits.burstCnt.poke(n.U)

      dut.io.Writeport(0).request.valid.poke(true.B)

      dut.clock.step(1)

      dut.io.Writeport(0).request.valid.poke(false.B)

      for(i <- 0 until n){
        dut.io.Writeport(0).data.ready.expect(true.B)

        for(k <- 0 until n){
          dut.io.Writeport(0).data.bits.writeData(k).poke(WMatrix(i)(k).U)
          dut.io.Writeport(0).data.bits.strb(k).poke(true.B)
        }

        dut.io.Writeport(0).data.valid.poke(true.B)

        if(i == (n-1)){
          dut.io.Writeport(0).data.bits.last.poke(true.B)
        }

        dut.clock.step(1)
      }

      dut.io.Writeport(0).data.valid.poke(false.B)

      dut.clock.step(1)

      dut.io.Readport(0).request.ready.expect(true.B)

      dut.io.Readport(0).request.bits.addr.poke(1.U)
      //dut.io.Readport(0).request.bits.burst.poke(n.U)
      dut.io.Readport(0).request.bits.burstSize.poke(n.U)
      dut.io.Readport(0).request.bits.burstCnt.poke(n.U)

      dut.io.Readport(0).request.valid.poke(true.B)

      dut.clock.step(1)

      dut.io.Readport(0).data.ready.poke(true.B)


      while (dut.io.Readport(0).data.valid.peek().litToBoolean == false) {
        dut.clock.step()
      } 

      dut.io.Readport(0).data.valid.expect(true.B)

      for(i <- 0 until n){

        for(k <- 0 until n){
          dut.io.Readport(0).data.bits.readData(k).expect(WMatrix(i)(k).U)
          //dut.io.Writeport(0).data.bits.strb(k).poke(true.B)
        }

        //dut.io.Writeport(0).data.valid.poke(true.B)

        if(i == (n-1)){
          dut.io.Readport(0).data.bits.last.expect(true.B)
        }

        dut.clock.step(1)
      }
    }
  }


}  