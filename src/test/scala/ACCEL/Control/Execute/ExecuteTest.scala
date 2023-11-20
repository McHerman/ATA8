package ATA8

import chisel3._
import chiseltest._
import org.scalatest.FlatSpec
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.experimental.ChiselEnum

import scala.util.Random

//import org.scalatest.concurrent.Eventually._
//import org.scalatest.time.{Millis, Span}


class ExecuteTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "Execute"

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

  it should "execute with 1 missing depend" in {
    test(new Execute(Configuration.test())).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>
      dut.io.instructionStream.ready.expect(true.B)
      dut.io.instructionStream.valid.poke(true.B)

      dut.io.instructionStream.bits.op.poke(0.U)
      dut.io.instructionStream.bits.mode.poke(0.U)
      dut.io.instructionStream.bits.grainSize.poke(0.U)

      dut.io.instructionStream.bits.addrs(0).addr.poke(0.U)
      dut.io.instructionStream.bits.addrs(0).depend.tag.poke(1.U)
      dut.io.instructionStream.bits.addrs(0).depend.ready.poke(true.B)

      dut.io.instructionStream.bits.addrs(1).addr.poke(64.U)
      dut.io.instructionStream.bits.addrs(1).depend.tag.poke(2.U)
      dut.io.instructionStream.bits.addrs(1).depend.ready.poke(false.B)

      dut.io.instructionStream.bits.addrd(0).addr.poke(128.U)

      dut.io.instructionStream.bits.size.poke(8.U)

      dut.clock.step(1)

      //dut.io.in.ready.expect(false.B)
      //dut.io.in.valid.poke(false.B)

      dut.io.instructionStream.ready.expect(true.B)
      dut.io.instructionStream.valid.poke(true.B)

      dut.clock.step(10)

      dut.io.eventIn.valid.poke(true.B)
      dut.io.eventIn.bits.tag.poke(2.U)

      dut.clock.step(1)

      dut.io.scratchIn(0).request.ready.poke(true.B)
      dut.io.scratchIn(1).request.ready.poke(true.B)

      while (dut.io.scratchIn(0).request.valid.peek().litToBoolean == false) {
        dut.clock.step()
      } 

      dut.io.scratchIn(0).request.bits.addr.expect(0.U)
      dut.io.scratchIn(0).request.bits.burstStride.expect(8.U)
      dut.io.scratchIn(0).request.bits.burstSize.expect(8.U)
      dut.io.scratchIn(0).request.bits.burstCnt.expect(8.U)

      dut.io.scratchIn(1).request.bits.addr.expect(64.U)
      dut.io.scratchIn(0).request.bits.burstStride.expect(8.U)
      dut.io.scratchIn(1).request.bits.burstSize.expect(8.U)
      dut.io.scratchIn(1).request.bits.burstCnt.expect(8.U)

      dut.clock.step(1)

      dut.io.scratchIn(0).data.ready.expect(true.B)
      dut.io.scratchIn(1).data.ready.expect(true.B)

      dut.io.scratchIn(0).data.valid.poke(true.B)
      dut.io.scratchIn(1).data.valid.poke(true.B)

      for(i <- 0 until n){

        /* for(k <- 0 until n){
          dut.io.Memport(0).bits.writeData(k).poke(matrix(i)(k).U(n.W))
        } */

        for(k <- 0 until n){
          dut.io.scratchIn(0).data.bits.readData(k).poke(matrix3(i)(k).U(8.W))
          dut.io.scratchIn(1).data.bits.readData(k).poke(matrix3(i)(k).U(8.W))
        }

        if(i == n - 1){
          dut.io.scratchIn(0).data.bits.last.poke(true.B)
          dut.io.scratchIn(1).data.bits.last.poke(true.B)
        }

        dut.clock.step()
      }

      dut.io.scratchIn(0).data.valid.poke(false.B)
      dut.io.scratchIn(1).data.valid.poke(false.B)

      dut.io.scratchOut.request.ready.poke(true.B)

      while (dut.io.scratchOut.request.valid.peek().litToBoolean == false) {
        dut.clock.step()
      } 

      val expectedResult = matrixDotProduct(matrix3, matrix3)

      dut.io.scratchOut.request.bits.addr.expect(128.U)
      dut.io.scratchIn(0).request.bits.burstStride.expect(8.U)
      dut.io.scratchOut.request.bits.burstSize.expect(8.U)
      dut.io.scratchOut.request.bits.burstCnt.expect(8.U)

      dut.io.scratchOut.data.ready.poke(true.B)

      dut.clock.step()

      dut.io.scratchOut.data.valid.expect(true.B)

      for (row <- 0 until n) {
        for (col <- 0 until n) {
          dut.io.scratchOut.data.bits.writeData(col).expect(expectedResult(row)(col).U(8.W))
        }
        
        if (row == n - 1) {
          dut.io.scratchOut.data.bits.last.expect(true.B)
        } else {
          dut.io.scratchOut.data.bits.last.expect(false.B)
        }

        dut.clock.step()
      }

      dut.io.eventOut.valid.expect(true.B)
      dut.io.eventOut.bits.tag.expect(0.U)

    }
  }


  it should "execute with 2 missing depend" in {
    test(new Execute(Configuration.test())).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>
      dut.io.instructionStream.ready.expect(true.B)
      dut.io.instructionStream.valid.poke(true.B)

      dut.io.instructionStream.bits.op.poke(0.U)
      dut.io.instructionStream.bits.mode.poke(0.U)
      dut.io.instructionStream.bits.grainSize.poke(0.U)

      dut.io.instructionStream.bits.addrs(0).addr.poke(0.U)
      dut.io.instructionStream.bits.addrs(0).depend.tag.poke(1.U)
      dut.io.instructionStream.bits.addrs(0).depend.ready.poke(false.B)

      dut.io.instructionStream.bits.addrs(1).addr.poke(64.U)
      dut.io.instructionStream.bits.addrs(1).depend.tag.poke(2.U)
      dut.io.instructionStream.bits.addrs(1).depend.ready.poke(false.B)

      dut.io.instructionStream.bits.addrd(0).addr.poke(128.U)

      dut.io.instructionStream.bits.size.poke(8.U)

      dut.clock.step(1)

      //dut.io.in.ready.expect(false.B)
      //dut.io.in.valid.poke(false.B)

      dut.io.instructionStream.ready.expect(true.B)
      dut.io.instructionStream.valid.poke(true.B)

      dut.clock.step(10)

      dut.io.eventIn.valid.poke(true.B)
      dut.io.eventIn.bits.tag.poke(1.U)

      dut.clock.step(10)

      dut.io.eventIn.valid.poke(true.B)
      dut.io.eventIn.bits.tag.poke(2.U)

      dut.clock.step(1)

      dut.io.scratchIn(0).request.ready.poke(true.B)
      dut.io.scratchIn(1).request.ready.poke(true.B)

      while (dut.io.scratchIn(0).request.valid.peek().litToBoolean == false) {
        dut.clock.step()
      } 

      dut.io.scratchIn(0).request.bits.addr.expect(0.U)
      //dut.io.scratchIn(0).request.bits.burst.expect(8.U)
      dut.io.scratchIn(0).request.bits.burstStride.expect(8.U)
      dut.io.scratchIn(0).request.bits.burstSize.expect(8.U)
      dut.io.scratchIn(0).request.bits.burstCnt.expect(8.U)


      dut.io.scratchIn(1).request.bits.addr.expect(64.U)
      //dut.io.scratchIn(1).request.bits.burst.expect(8.U)
      dut.io.scratchIn(0).request.bits.burstStride.expect(8.U)
      dut.io.scratchIn(1).request.bits.burstSize.expect(8.U)
      dut.io.scratchIn(1).request.bits.burstCnt.expect(8.U)

      dut.clock.step(1)

      dut.io.scratchIn(0).data.ready.expect(true.B)
      dut.io.scratchIn(1).data.ready.expect(true.B)

      dut.io.scratchIn(0).data.valid.poke(true.B)
      dut.io.scratchIn(1).data.valid.poke(true.B)

      for(i <- 0 until n){

        /* for(k <- 0 until n){
          dut.io.Memport(0).bits.writeData(k).poke(matrix(i)(k).U(n.W))
        } */

        for(k <- 0 until n){
          dut.io.scratchIn(0).data.bits.readData(k).poke(matrix3(i)(k).U(8.W))
          dut.io.scratchIn(1).data.bits.readData(k).poke(matrix3(i)(k).U(8.W))
        }

        if(i == n - 1){
          dut.io.scratchIn(0).data.bits.last.poke(true.B)
          dut.io.scratchIn(1).data.bits.last.poke(true.B)
        }

        dut.clock.step()
      }

      dut.io.scratchIn(0).data.valid.poke(false.B)
      dut.io.scratchIn(1).data.valid.poke(false.B)

      dut.io.scratchOut.request.ready.poke(true.B)

      while (dut.io.scratchOut.request.valid.peek().litToBoolean == false) {
        dut.clock.step()
      } 

      val expectedResult = matrixDotProduct(matrix3, matrix3)

      dut.io.scratchOut.request.bits.addr.expect(128.U)
      //dut.io.scratchOut.request.bits.burst.expect(8.U)
      dut.io.scratchOut.request.bits.burstStride.expect(8.U)
      dut.io.scratchOut.request.bits.burstSize.expect(8.U)
      dut.io.scratchOut.request.bits.burstCnt.expect(8.U)


      dut.io.scratchOut.data.ready.poke(true.B)

      dut.clock.step()

      dut.io.scratchOut.data.valid.expect(true.B)

      for (row <- 0 until n) {
        for (col <- 0 until n) {
          dut.io.scratchOut.data.bits.writeData(col).expect(expectedResult(row)(col).U(8.W))
        }
        
        if (row == n - 1) {
          dut.io.scratchOut.data.bits.last.expect(true.B)
        } else {
          dut.io.scratchOut.data.bits.last.expect(false.B)
        }

        dut.clock.step()
      }

      dut.io.eventOut.valid.expect(true.B)
      dut.io.eventOut.bits.tag.expect(0.U)
    }
  }

  it should "allocate multible instructions" in {
    test(new Execute(Configuration.test())).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>
      dut.io.instructionStream.ready.expect(true.B)
      dut.io.instructionStream.valid.poke(true.B)

      dut.io.instructionStream.bits.op.poke(0.U)
      dut.io.instructionStream.bits.mode.poke(0.U)
      dut.io.instructionStream.bits.grainSize.poke(0.U)

      dut.io.instructionStream.bits.addrs(0).addr.poke(0.U)
      dut.io.instructionStream.bits.addrs(0).depend.tag.poke(1.U)
      dut.io.instructionStream.bits.addrs(0).depend.ready.poke(false.B)

      dut.io.instructionStream.bits.addrs(1).addr.poke(64.U)
      dut.io.instructionStream.bits.addrs(1).depend.tag.poke(2.U)
      dut.io.instructionStream.bits.addrs(1).depend.ready.poke(false.B)

      dut.io.instructionStream.bits.addrd(0).addr.poke(256.U)
      dut.io.instructionStream.bits.addrd(0).tag.poke(5.U)

      dut.io.instructionStream.bits.size.poke(8.U)

      dut.clock.step(1)

      dut.io.instructionStream.ready.expect(true.B)
      dut.io.instructionStream.valid.poke(true.B)

      dut.io.instructionStream.bits.op.poke(0.U)
      dut.io.instructionStream.bits.mode.poke(0.U)
      dut.io.instructionStream.bits.grainSize.poke(0.U)

      dut.io.instructionStream.bits.addrs(0).addr.poke(128.U)
      dut.io.instructionStream.bits.addrs(0).depend.tag.poke(3.U)
      dut.io.instructionStream.bits.addrs(0).depend.ready.poke(false.B)

      dut.io.instructionStream.bits.addrs(1).addr.poke(192.U)
      dut.io.instructionStream.bits.addrs(1).depend.tag.poke(4.U)
      dut.io.instructionStream.bits.addrs(1).depend.ready.poke(false.B)

      dut.io.instructionStream.bits.addrd(0).addr.poke(320.U)
      dut.io.instructionStream.bits.addrd(0).tag.poke(6.U)

      dut.io.instructionStream.bits.size.poke(8.U)

      dut.clock.step(1)

      //dut.io.in.ready.expect(false.B)
      //dut.io.in.valid.poke(false.B)

      dut.io.instructionStream.ready.expect(true.B)
      dut.io.instructionStream.valid.poke(false.B)

      dut.clock.step(10)

      dut.io.eventIn.valid.poke(true.B)
      dut.io.eventIn.bits.tag.poke(3.U)

      dut.clock.step(1)

      dut.io.eventIn.valid.poke(true.B)
      dut.io.eventIn.bits.tag.poke(4.U)

      dut.clock.step(1)

      dut.io.eventIn.valid.poke(true.B)
      dut.io.eventIn.bits.tag.poke(1.U)

      dut.clock.step(1)

      dut.io.eventIn.valid.poke(true.B)
      dut.io.eventIn.bits.tag.poke(2.U)

      dut.clock.step(1)

      dut.io.scratchIn(0).request.ready.poke(true.B)
      dut.io.scratchIn(1).request.ready.poke(true.B)

      while (dut.io.scratchIn(0).request.valid.peek().litToBoolean == false) {
        dut.clock.step()
      } 

      dut.io.scratchIn(0).request.bits.addr.expect(0.U)
      dut.io.scratchIn(0).request.bits.burstStride.expect(8.U)
      dut.io.scratchIn(0).request.bits.burstSize.expect(8.U)
      dut.io.scratchIn(0).request.bits.burstCnt.expect(8.U)

      dut.io.scratchIn(1).request.bits.addr.expect(64.U)
      dut.io.scratchIn(0).request.bits.burstStride.expect(8.U)
      dut.io.scratchIn(1).request.bits.burstSize.expect(8.U)
      dut.io.scratchIn(1).request.bits.burstCnt.expect(8.U)

      dut.clock.step(1)

      dut.io.scratchIn(0).data.ready.expect(true.B)
      dut.io.scratchIn(1).data.ready.expect(true.B)

      dut.io.scratchIn(0).data.valid.poke(true.B)
      dut.io.scratchIn(1).data.valid.poke(true.B)

      for(i <- 0 until n){

        /* for(k <- 0 until n){
          dut.io.Memport(0).bits.writeData(k).poke(matrix(i)(k).U(n.W))
        } */

        for(k <- 0 until n){
          dut.io.scratchIn(0).data.bits.readData(k).poke(matrix3(i)(k).U(8.W))
          dut.io.scratchIn(1).data.bits.readData(k).poke(matrix3(i)(k).U(8.W))
        }

        if(i == n - 1){
          dut.io.scratchIn(0).data.bits.last.poke(true.B)
          dut.io.scratchIn(1).data.bits.last.poke(true.B)
        }

        dut.clock.step()
      }

      dut.io.scratchIn(0).data.valid.poke(false.B)
      dut.io.scratchIn(1).data.valid.poke(false.B)

      dut.io.scratchIn(0).data.bits.last.poke(false.B)
      dut.io.scratchIn(1).data.bits.last.poke(false.B)

      dut.io.scratchIn(0).request.ready.poke(false.B)
      dut.io.scratchIn(1).request.ready.poke(false.B)

      dut.io.scratchOut.request.ready.poke(true.B)

      while (dut.io.scratchOut.request.valid.peek().litToBoolean == false) {
        dut.clock.step()
      } 

      val expectedResult = matrixDotProduct(matrix3, matrix3)

      dut.io.scratchOut.request.bits.addr.expect(256.U)
      dut.io.scratchOut.request.bits.burstStride.expect(8.U)
      dut.io.scratchOut.request.bits.burstSize.expect(8.U)
      dut.io.scratchOut.request.bits.burstCnt.expect(8.U)

      dut.io.scratchOut.data.ready.poke(true.B)

      dut.clock.step()

      dut.io.scratchOut.data.valid.expect(true.B)

      for (row <- 0 until n) {
        for (col <- 0 until n) {
          dut.io.scratchOut.data.bits.writeData(col).expect(expectedResult(row)(col).U(8.W))
        }
        
        if (row == n - 1) {
          dut.io.scratchOut.data.bits.last.expect(true.B)
        } else {
          dut.io.scratchOut.data.bits.last.expect(false.B)
        }

        dut.clock.step()
      }

      dut.io.scratchOut.request.ready.poke(false.B)
      dut.io.scratchOut.data.ready.poke(false.B)

      dut.io.eventOut.valid.expect(true.B)
      dut.io.eventOut.bits.tag.expect(5.U)

      // Second instruction 

      dut.io.scratchIn(0).request.ready.poke(true.B)
      dut.io.scratchIn(1).request.ready.poke(true.B)

      while (dut.io.scratchIn(0).request.valid.peek().litToBoolean == false) {
        dut.clock.step()
      } 

      dut.io.scratchIn(0).request.bits.addr.expect(128.U)
      dut.io.scratchIn(0).request.bits.burstStride.expect(8.U)
      dut.io.scratchIn(0).request.bits.burstSize.expect(8.U)
      dut.io.scratchIn(0).request.bits.burstCnt.expect(8.U)

      dut.io.scratchIn(1).request.bits.addr.expect(192.U)
      dut.io.scratchIn(0).request.bits.burstStride.expect(8.U)
      dut.io.scratchIn(1).request.bits.burstSize.expect(8.U)
      dut.io.scratchIn(1).request.bits.burstCnt.expect(8.U)

      dut.clock.step(1)

      dut.io.scratchIn(0).data.ready.expect(true.B)
      dut.io.scratchIn(1).data.ready.expect(true.B)

      dut.io.scratchIn(0).data.valid.poke(true.B)
      dut.io.scratchIn(1).data.valid.poke(true.B)

      for(i <- 0 until n){

        /* for(k <- 0 until n){
          dut.io.Memport(0).bits.writeData(k).poke(matrix(i)(k).U(n.W))
        } */

        for(k <- 0 until n){
          dut.io.scratchIn(0).data.bits.readData(k).poke(matrix3(i)(k).U(8.W))
          dut.io.scratchIn(1).data.bits.readData(k).poke(matrix3(i)(k).U(8.W))
        }

        if(i == n - 1){
          dut.io.scratchIn(0).data.bits.last.poke(true.B)
          dut.io.scratchIn(1).data.bits.last.poke(true.B)
        }

        dut.clock.step()
      }

      dut.io.scratchIn(0).data.valid.poke(false.B)
      dut.io.scratchIn(1).data.valid.poke(false.B)

      dut.io.scratchIn(0).data.bits.last.poke(false.B)
      dut.io.scratchIn(1).data.bits.last.poke(false.B)

      dut.io.scratchIn(0).request.ready.poke(false.B)
      dut.io.scratchIn(1).request.ready.poke(false.B)

      dut.io.scratchOut.request.ready.poke(true.B)

      while (dut.io.scratchOut.request.valid.peek().litToBoolean == false) {
        dut.clock.step()
      } 

      val expectedResult2 = matrixDotProduct(matrix3, matrix3)

      dut.io.scratchOut.request.bits.addr.expect(320.U)
      dut.io.scratchIn(0).request.bits.burstStride.expect(8.U)
      dut.io.scratchOut.request.bits.burstSize.expect(8.U)
      dut.io.scratchOut.request.bits.burstCnt.expect(8.U)

      dut.io.scratchOut.data.ready.poke(true.B)

      dut.clock.step()

      dut.io.scratchOut.data.valid.expect(true.B)

      for (row <- 0 until n) {
        for (col <- 0 until n) {
          dut.io.scratchOut.data.bits.writeData(col).expect(expectedResult2(row)(col).U(8.W))
        }
        
        if (row == n - 1) {
          dut.io.scratchOut.data.bits.last.expect(true.B)
        } else {
          dut.io.scratchOut.data.bits.last.expect(false.B)
        }

        dut.clock.step()
      }

      dut.io.eventOut.valid.expect(true.B)
      dut.io.eventOut.bits.tag.expect(6.U)
    }
  }


  
}
