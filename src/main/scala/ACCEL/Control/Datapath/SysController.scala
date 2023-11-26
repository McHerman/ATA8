package ATA8

import chisel3._
import chisel3.util._
//import chisel3.util.experimental.BoringUtils

object VectorFillerFunctions {
  // Helper function to truncate values to y
  private def truncate(value: UInt, y: Int): UInt = Mux(value > y.U, y.U, value)

  // Recursive function to build the binary tree and calculate the vector
  def buildTree(n: UInt, x: Int, y: Int): Vec[UInt] = {
    def recurse(currentValue: UInt, subtractor: UInt, depth: Int, maxDepth: Int): Vec[UInt] = {
      if (depth == maxDepth) {
        VecInit(Seq(truncate(currentValue, y)))
      } else {
        val nextSubtractor = subtractor >> 1
        val withSubtraction = recurse(Mux(currentValue >= subtractor, currentValue - subtractor, 0.U), nextSubtractor, depth + 1, maxDepth)
        val withoutSubtraction = recurse(currentValue, nextSubtractor, depth + 1, maxDepth)
        
        VecInit(withoutSubtraction ++ withSubtraction)
      }
    }

    val initialSubtractor = ((x * y) / 2).U
    recurse(n, initialSubtractor, 0, log2Ceil(x))
  }
}

  
class SysController(implicit c: Configuration) extends Module {
  val io = IO(new Bundle {
    val in = new Readport(new ExecuteInstIssue,0)

    val dmaRead = Vec(2,Vec(c.grainDim, new DMARead()))
    val dmaWrite = Vec(c.grainDim, new DMAWrite())

		val out = Decoupled(new SysOP)
    val sysCompleted = Flipped(Valid(new Bundle{val tag = UInt(c.tagWidth.W)}))
    val event = Valid(new Event)
    val debug = new ExeDebug
  })

  val opbuffer = Module(new BufferFIFO(8,new SysOP)) // FIXME: Magic number
  val readBuffer = Module(new BufferFIFO(8, new Bundle{val addr = UInt(16.W); val tag = UInt(c.tagWidth.W); val size = UInt(8.W)}))

  io.in.request.valid := false.B
	io.in.request.bits := DontCare

  io.out.valid := false.B
  io.out.bits := DontCare

  io.dmaRead(0).foreach{element => element.request.valid := false.B; element.request.bits := DontCare; element.response.ready := false.B}
  io.dmaRead(1).foreach{element => element.request.valid := false.B; element.request.bits := DontCare; element.response.ready := false.B}

  io.dmaWrite.foreach{element => element.request.valid := false.B; element.request.bits := DontCare; element.response.ready := false.B}

  io.event.valid := false.B
  io.event.bits := DontCare

	opbuffer.io.WriteData.valid := false.B
	opbuffer.io.WriteData.bits := DontCare
	
	opbuffer.io.ReadData.request.valid := false.B
  opbuffer.io.ReadData.request.bits := DontCare

  readBuffer.io.WriteData.valid := false.B
	readBuffer.io.WriteData.bits := DontCare
	
	readBuffer.io.ReadData.request.valid := false.B
  readBuffer.io.ReadData.request.bits := DontCare

	val reg = Reg(new ExecuteInstIssue)
  val StateReg = RegInit(0.U(4.W))

  /// DEBUG ///

  io.debug.state := StateReg

	switch(StateReg){ 
		is(0.U){ // Recieves instruction from queue 
      when(io.in.request.ready){
        io.in.request.valid := true.B

        when(io.in.response.valid){
          reg := io.in.response.bits.readData
          StateReg := 1.U
        }
      }
		}
		is(1.U){ // Invokes DMA's to write into systolic array buffers
      val readySignals = VecInit(io.dmaRead.flatten.map(_.request.ready))

      when(readySignals.reduceTree(_ && _)){ // All the DMA's are ready
        (reg.addrs zip io.dmaRead).foreach{case (addrs,dmaSeq) => 
          val readSizes = VectorFillerFunctions.buildTree(reg.size, c.grainDim, c.dataBusSize) // Find the widht transactions the dma's are going to be handleing 

          (dmaSeq zip readSizes.zipWithIndex).foreach{case (dma,(size,index)) => // Invokes the dma's 
            val addrSum = if (index == 0) 0.U else readSizes.take(index).reduce(_ + _) // TODO: Kinda questionable, check later

            dma.request.bits.addr := addrs.addr + addrSum
            
            dma.request.bits.burstStride := reg.size
            dma.request.bits.burstSize := size
            dma.request.bits.burstCnt := reg.size
            dma.request.valid := true.B
          }
        }

        StateReg := 2.U
      }
		}
		is(2.U){ // Waits for the DMA's to write into the buffers
      val completedSignals = VecInit(io.dmaRead.flatten.map { comp =>
        comp.response.valid && comp.response.bits.completed
      })

      when(completedSignals.reduceTree(_ && _)){
        io.dmaRead.flatten.foreach{element => element.response.ready := true.B}
        StateReg := 3.U
      }
		}
		is(3.U){ // When data is written to buffers, adds instruction to queue to be fetched by SysCtrl. Also adds read instruction to queue to be executed when systolic array finished computing 
			when(opbuffer.io.WriteData.ready && readBuffer.io.WriteData.ready){
				opbuffer.io.WriteData.valid := true.B
				opbuffer.io.WriteData.bits.mode := reg.mode
				opbuffer.io.WriteData.bits.size := reg.size
        opbuffer.io.WriteData.bits.tag := reg.addrd(0).tag
        opbuffer.io.WriteData.bits.sizes := VectorFillerFunctions.buildTree(reg.size, c.grainDim, c.dataBusSize)

        readBuffer.io.WriteData.valid := true.B
				readBuffer.io.WriteData.bits.addr := reg.addrd(0).addr
        readBuffer.io.WriteData.bits.tag := reg.addrd(0).tag
				readBuffer.io.WriteData.bits.size := reg.size

				StateReg := 0.U
			}
		}
	}


  when(io.sysCompleted.valid && readBuffer.io.ReadData.request.ready){ // When systolic array finishes, logic will fetch read instruction from queue and write data into scratchpad, FIXME: Dangerous, can lock if syscompleted only stays high for one cc
    val readySignals = VecInit(io.dmaWrite.map(_.request.ready))
 
    when(readySignals.reduceTree(_ && _)){
      readBuffer.io.ReadData.request.valid := true.B

      val op = readBuffer.io.ReadData.response.bits.readData
      val writeSizes = VectorFillerFunctions.buildTree(op.size, c.grainDim, c.dataBusSize)

      (io.dmaWrite zip writeSizes.zipWithIndex).foreach{case (dma,(size,index)) => 
        val addrSum = if (index == 0) 0.U else writeSizes.take(index).reduce(_ + _) // TODO: Kinda questionable, check later

        dma.request.bits.addr := op.addr + addrSum
      
        dma.request.bits.burstStride := op.size
        dma.request.bits.burstSize := size
        dma.request.bits.burstCnt := op.size
        dma.request.bits.tag := op.tag

        dma.request.valid := (io.sysCompleted.bits.tag === readBuffer.io.ReadData.response.bits.readData.tag) // checks that the read op matches the finished sys op
      }
    }
  }


  val writeCompletedSignals = VecInit(io.dmaWrite.map { comp =>
    comp.response.valid && comp.response.bits.completed
  })

  when(writeCompletedSignals.reduceTree(_ && _)){ // Broadcasts event when data is read from systolic array back into scratchpad
    io.dmaWrite.foreach{element => element.response.ready := true.B}

    io.event.valid := true.B
    io.event.bits.tag := io.dmaWrite.head.response.bits.tag
  }

  when(opbuffer.io.ReadData.request.ready && io.out.ready){ // Allows SysCtrl to fetch operation from queue. 
    io.out.valid := true.B
    opbuffer.io.ReadData.request.valid := true.B

    io.out.bits <> opbuffer.io.ReadData.response.bits.readData
  }
}