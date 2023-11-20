package ATA8

import chisel3._
import chisel3.util._
  
class SysWrapper(config: Configuration) extends Module {

  implicit val c = config
  
  val io = IO(new Bundle {
    //val in = Flipped(Decoupled(new ExecuteInstIssue))
    val in = new Readport(new ExecuteInstIssue,0)
    //val scratchOut = new WriteportScratch
    //val scratchIn = Vec(2,new ReadportScratch)
    val scratchOut = Vec(c.grainDim, new WriteportScratch)
    val scratchIn = Vec(2,Vec(c.grainDim, new ReadportScratch))
    val event = Valid(new Event)
    val debug = new ExeDebug
  })

  val SysController = Module(new SysController())
  val SysGrain = Module(new Grain())

  val DMA = Seq.fill(2, c.grainDim)(Module(new SysDMA()))
  val WriteDMA = Seq.fill(c.grainDim)(Module(new SysWriteDMA()))

  SysController.io.in <> io.in
  //SysController.io.scratchIn <> io.scratchIn
  //SysController.io.scratchOut <> io.scratchOut

  SysController.io.out <> SysGrain.io.in 
  //SysController.io.memport <> SysGrain.io.Memport
  //SysController.io.readPort <> SysGrain.io.Readport

  //SysGrain.io.writePort(0) := VecInit(DMA(0).map(_.io.writePort))
  //SysGrain.io.writePort(1) := VecInit(DMA(0).map(_.io.writePort))

  (SysGrain.io.writePort zip DMA).foreach{case (portVec,dmaVec) => 
    (portVec zip dmaVec).foreach{case (port,dma) =>
      port <> dma.io.writePort
    }
  }
  
  //io.scratchIn.flatten <> VecInit(DMA.flatten.map(_.io.scratchIn))
  //VecInit(io.scratchIn.flatten) <> VecInit(DMA.flatten.map(_.io.scratchIn))

  (io.scratchIn.flatten zip DMA.flatten).foreach{case (port,dma) => 
    dma.io.scratchIn <> port  
  }

  //SysController.io.dmaRead(0) <> VecInit(DMA(0).map(_.io.in))
  //SysController.io.dmaRead(1) <> VecInit(DMA(1).map(_.io.in))  

  (SysController.io.dmaRead.flatten zip DMA.flatten).foreach{case (command,dma) => 
    command <> dma.io.in
  }

  (io.scratchOut zip WriteDMA).foreach{case (port,dma) => 
    dma.io.scratchOut <> port  
  }

  (SysController.io.dmaWrite zip WriteDMA).foreach{case (command,dma) => 
    dma.io.in <> command
  }

  (SysGrain.io.readPort zip WriteDMA).foreach{case (port,dma) =>
    port <> dma.io.readPort
  }


  SysController.io.sysCompleted <> SysGrain.io.completed

  io.event := SysController.io.event

  /// DEBUG /// 

  SysController.io.debug <> io.debug
}

object SysWrapper extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new SysWrapper(Configuration.default()))
}
