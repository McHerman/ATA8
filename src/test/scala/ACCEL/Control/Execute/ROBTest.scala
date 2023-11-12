package ATA8

import chisel3._
import chiseltest._
import org.scalatest.FlatSpec
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.experimental.ChiselEnum

import scala.util.Random

//import org.scalatest.concurrent.Eventually._
//import org.scalatest.time.{Millis, Span}


class ROBTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "ROB"

  //val n = 2 + Random.nextInt(30)
  val n = 8

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

  //implicit val Config = Configuration.default().copy(issueQueueSize = n, simulation = true)

  it should "allocate n, fetch, and deallocate" in {
    test(new ROB(8,1,0)(Configuration.test())).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>
      for(i <- 0 until n){
        dut.io.Writeport.addr.ready.expect(true.B)

        dut.io.Writeport.addr.valid.poke(true.B)
        dut.io.Writeport.addr.bits.addr.poke(i.U)
        dut.io.Writeport.addr.bits.ready.poke(false.B)

        dut.io.Writeport.tag.valid.expect(true.B)
        dut.io.Writeport.tag.bits.expect(i.U)

        dut.clock.step(1)
      }

      dut.io.Writeport.addr.valid.poke(false.B)
      dut.io.Writeport.addr.ready.expect(false.B)

      for(i <- 0 until n){
        dut.io.ReadData(0).request.valid.poke(true.B)
        dut.io.ReadData(0).request.bits.addr.poke(i.U)
        
        dut.io.ReadData(0).response.valid.expect(true.B)
        dut.io.ReadData(0).response.bits.tag.expect(i.U)
        dut.io.ReadData(0).response.bits.ready.expect(false.B)

        dut.clock.step(1)
      }

      for(i <- 0 until n){
        dut.io.event(0).valid.poke(true.B)
        dut.io.event(0).bits.tag.poke(i.U)

        dut.clock.step(1)
      }

      dut.clock.step(10)

      for(i <- 0 until n){
        dut.io.ReadData(0).request.valid.poke(true.B)
        dut.io.ReadData(0).request.bits.addr.poke(i.U)
        
        dut.io.ReadData(0).response.valid.expect(false.B)
        //dut.io.ReadData(0).response.bits.tag.expect(i.U)
        //dut.io.ReadData(0).response.bits.ready.expect(false.B)

        dut.clock.step(1)
      }

      for(i <- 0 until n){
        dut.io.Writeport.addr.ready.expect(true.B)

        dut.io.Writeport.addr.valid.poke(true.B)
        dut.io.Writeport.addr.bits.addr.poke(i.U)
        dut.io.Writeport.addr.bits.ready.poke(false.B)

        dut.io.Writeport.tag.valid.expect(true.B)
        dut.io.Writeport.tag.bits.expect(i.U)

        dut.clock.step(1)
      }







    }
  }
}

