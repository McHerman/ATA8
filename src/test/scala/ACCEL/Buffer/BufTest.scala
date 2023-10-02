package ATA8

import chisel3._
import chiseltest._
import org.scalatest.FlatSpec
import org.scalatest.flatspec.AnyFlatSpec

import scala.util.Random
import scala.io.Source

class BufTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "Buffer"
  
	val n = 8

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



	
  "BufTest " should "pass" in {
    //test(new Grain(n,n,8)).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>
    test(new Buffer(Configuration.test())).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>

      for (i <- 0 until n) {
        dut.io.Writeport(0).request.valid.poke(true.B)
        dut.io.Writeport(0).request.bits.addr.poke(n.U)


        dut.io.Writeport(0).data.valid.poke(true.B)

        for(k <- 0 until n){
          dut.io.Writeport(0).data.bits.writeData(k).poke(WMatrix(i)(k).U)
          dut.io.Writeport(0).data.bits.strb(k).poke(true.B)
        }
        
      
        dut.clock.step()
      }

      dut.io.Writeport(0).request.valid.poke(false.B)
      dut.io.Writeport(0).data.valid.poke(false.B)

      for (i <- 0 until n) {
        dut.io.Readport(0).request.valid.poke(true.B)
        dut.io.Readport(0).request.bits.addr.poke(n.U)
      
        dut.clock.step()
      }

    }
  }

  "BufTest2 " should "pass" in {
    //test(new Grain(n,n,8)).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>
    test(new Buffer(Configuration.buftest())).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>

      for (i <- 0 until n) {
        dut.io.Writeport(0).request.valid.poke(true.B)
        dut.io.Writeport(0).request.bits.addr.poke(i.U)


        dut.io.Writeport(0).data.valid.poke(true.B)

        for(k <- 0 until n){
          dut.io.Writeport(0).data.bits.writeData(k).poke(WMatrix(i)(k).U)
          dut.io.Writeport(0).data.bits.strb(k).poke(true.B)
        }

        dut.io.Writeport(1).request.valid.poke(true.B)
        dut.io.Writeport(1).request.bits.addr.poke((n+i).U)


        dut.io.Writeport(1).data.valid.poke(true.B)

        for(k <- 0 until n){
          dut.io.Writeport(1).data.bits.writeData(k).poke(WMatrix(k)(i).U)
          dut.io.Writeport(1).data.bits.strb(k).poke(true.B)
        }
        
        
        dut.clock.step()
      }

      dut.io.Writeport(0).request.valid.poke(false.B)
      dut.io.Writeport(0).data.valid.poke(false.B)

      for (i <- 0 until 2*n) {
        dut.io.Readport(0).request.valid.poke(true.B)
        dut.io.Readport(0).request.bits.addr.poke(i.U)
      
        dut.clock.step()
      }

    }
  }
}  
