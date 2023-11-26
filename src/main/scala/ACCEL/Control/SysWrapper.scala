package ATA8

import chisel3._
import chisel3.util._
  
class SysWrapper(implicit c: Configuration) extends Module {  
  val io = IO(new Bundle {
    val in = new Readport(new ExecuteInstIssue,0)
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

  SysController.io.out <> SysGrain.io.in 

  (SysGrain.io.writePort zip DMA).foreach{case (portVec,dmaVec) => 
    (portVec zip dmaVec).foreach{case (port,dma) =>
      port <> dma.io.writePort
    }
  }

  (io.scratchIn.flatten zip DMA.flatten).foreach{case (port,dma) => 
    dma.io.scratchIn <> port  
  }

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