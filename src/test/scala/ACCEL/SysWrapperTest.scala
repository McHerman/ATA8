package ATA8

import chisel3._
import chiseltest._
import org.scalatest.FlatSpec
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.experimental.ChiselEnum

import scala.util.Random

//import org.scalatest.concurrent.Eventually._
//import org.scalatest.time.{Millis, Span}


class SysWrapperTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "SysWrapper"

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

  val matrix2: Seq[UInt] = Seq(
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

  it should "execute WS" in {
    //val n = 2 + Random.nextInt(30)
    //val n = 16

    test(new SysWrapper(Configuration.test())).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>
			
      /* dut.io.in.ready.expect(true.B)
      dut.io.in.valid.poke(true.B)

      dut.io.in.bits.op.poke(0.U)
      dut.io.in.bits.mode.poke(0.U)
      dut.io.in.bits.grainSize.poke(0.U)

      dut.io.in.bits.addrs(0).addr.poke(0.U)
      dut.io.in.bits.addrs(1).addr.poke(8.U)

      dut.io.in.bits.addrd.addr.poke(16.U)

      dut.io.in.bits.size.poke(8.U)
      */

      dut.io.in.request.ready.poke(true.B)
      dut.io.in.request.valid.expect(true.B)

      dut.io.in.response.valid.poke(true.B)

      dut.io.in.response.bits.readData.op.poke(0.U)
      dut.io.in.response.bits.readData.mode.poke(0.U)
      dut.io.in.response.bits.readData.grainSize.poke(0.U)

      dut.io.in.response.bits.readData.addrs(0).addr.poke(0.U)
      dut.io.in.response.bits.readData.addrs(1).addr.poke(8.U)

      dut.io.in.response.bits.readData.addrd(0).addr.poke(16.U)

      dut.io.in.response.bits.readData.size.poke(8.U)

      dut.clock.step(1)

      //dut.io.in.ready.expect(false.B)
      //dut.io.in.valid.poke(false.B)

      dut.io.in.request.ready.poke(false.B)
      dut.io.in.request.valid.expect(false.B)

      dut.io.scratchIn(0).request.ready.poke(true.B)
      dut.io.scratchIn(1).request.ready.poke(true.B)

      while (dut.io.scratchIn(0).request.valid.peek().litToBoolean == false) {
        dut.clock.step()
      } 

      dut.io.scratchIn(0).request.bits.addr.expect(0.U)
      //dut.io.scratchIn(0).request.bits.burst.expect(8.U)
      dut.io.scratchIn(0).request.bits.burstSize.expect(8.U)
      dut.io.scratchIn(0).request.bits.burstCnt.expect(8.U)


      dut.io.scratchIn(1).request.bits.addr.expect(8.U)
      //dut.io.scratchIn(1).request.bits.burst.expect(8.U)
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

        dut.clock.step()
      }

      dut.io.scratchIn(0).data.valid.poke(false.B)
      dut.io.scratchIn(1).data.valid.poke(false.B)

      dut.io.scratchOut.request.ready.poke(true.B)

      while (dut.io.scratchOut.request.valid.peek().litToBoolean == false) {
        dut.clock.step()
      } 

      val expectedResult = matrixDotProduct(matrix3, matrix3)

      dut.io.scratchOut.request.bits.addr.expect(16.U)
      //dut.io.scratchOut.request.bits.burst.expect(8.U)
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

      /* val product = Array.ofDim[Int](n, n)

      for (i <- 0 until n) {
				for (j <- 0 until n) {
					var sum = 0
					for (k <- 0 until n) {
						sum += matrix3(i)(k) * matrix3(k)(j)
					}
					product(i)(j) = sum
				}
			}

			// Print the product matrix
			for (row <- product) {
				println(row.mkString("\t"))
			} */

    }
  }



  it should "execute OS" in {
    //val n = 2 + Random.nextInt(30)
    //val n = 16

    test(new SysWrapper(Configuration.test())).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>
			
      /* dut.io.in.ready.expect(true.B)
      dut.io.in.valid.poke(true.B)

      dut.io.in.bits.op.poke(0.U)
      dut.io.in.bits.mode.poke(1.U)
      dut.io.in.bits.grainSize.poke(0.U)

      dut.io.in.bits.addrs(0).addr.poke(0.U)
      dut.io.in.bits.addrs(1).addr.poke(8.U)

      dut.io.in.bits.addrd.addr.poke(16.U)

      dut.io.in.bits.size.poke(8.U)
      */

      dut.io.in.request.ready.poke(true.B)
      dut.io.in.request.valid.expect(true.B)

      dut.io.in.response.valid.poke(true.B)

      dut.io.in.response.bits.readData.op.poke(0.U)
      dut.io.in.response.bits.readData.mode.poke(1.U)
      dut.io.in.response.bits.readData.grainSize.poke(0.U)

      dut.io.in.response.bits.readData.addrs(0).addr.poke(0.U)
      dut.io.in.response.bits.readData.addrs(1).addr.poke(8.U)

      dut.io.in.response.bits.readData.addrd(0).addr.poke(16.U)

      dut.io.in.response.bits.readData.size.poke(8.U)

      dut.clock.step(1)

      //dut.io.in.ready.expect(false.B)
      //dut.io.in.valid.poke(false.B)

      dut.io.in.request.ready.poke(false.B)
      dut.io.in.request.valid.expect(false.B)

      dut.io.scratchIn(0).request.ready.poke(true.B)
      dut.io.scratchIn(1).request.ready.poke(true.B)

      while (dut.io.scratchIn(0).request.valid.peek().litToBoolean == false) {
        dut.clock.step()
      } 

      dut.io.scratchIn(0).request.bits.addr.expect(0.U)
      //dut.io.scratchIn(0).request.bits.burst.expect(8.U)
      dut.io.scratchIn(0).request.bits.burstSize.expect(8.U)
      dut.io.scratchIn(0).request.bits.burstCnt.expect(8.U)

      dut.io.scratchIn(1).request.bits.addr.expect(8.U)
      //dut.io.scratchIn(1).request.bits.burst.expect(8.U)
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
          dut.io.scratchIn(0).data.bits.readData(k).poke(matrix3(k)(i).U(8.W))
          dut.io.scratchIn(1).data.bits.readData(k).poke(matrix3(i)(k).U(8.W))
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

      dut.io.scratchOut.request.bits.addr.expect(16.U)
      //dut.io.scratchOut.request.bits.burst.expect(8.U)
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

      /* val product = Array.ofDim[Int](n, n)

      for (i <- 0 until n) {
				for (j <- 0 until n) {
					var sum = 0
					for (k <- 0 until n) {
						sum += matrix3(i)(k) * matrix3(k)(j)
					}
					product(i)(j) = sum
				}
			}

			// Print the product matrix
			for (row <- product) {
				println(row.mkString("\t"))
			} */
    }
  }
}
