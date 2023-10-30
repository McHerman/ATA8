package ATA8

import chisel3._
import chisel3.experimental._
import chisel3.util._

class ATA8(config: Configuration) extends Module {
  implicit val c = config

  val io = IO(new Bundle {
    val AXIST_out = new AXIST_2(64,2,1,1,1) 
    val AXIST_inData = Flipped(new AXIST_2(64,2,1,1,1))
    val AXIST_inInst = Flipped(new AXIST_2(64,2,1,1,1))
  })

  val axi_aclk = IO(Input(Clock()))
  val axi_arst = IO(Input(Reset()))
  val sysClk = IO(Input(Clock()))
  val sysRst = IO(Input(Reset()))

  val FrontEnd = Module(new FrontEnd)


  val AXIDataBuffer = Module(new AXI_in())

  AXIDataBuffer.io.enq_clk := axi_aclk
  AXIDataBuffer.io.enq_rst := axi_arst
  AXIDataBuffer.io.deq_clk := sysClk
  AXIDataBuffer.io.deq_rst := sysRst

  //val Execute = Module(new Execute(config))
  val Execute = withClockAndReset(sysClk, sysRst) { Module(new Execute(config)) }
  //val Load = Module(new Load(config))
  val Load = withClockAndReset(sysClk, sysRst) { Module(new Load(config)) }
  //val Store = Module(new Store(config))
  val Store = withClockAndReset(sysClk, sysRst) { Module(new Store(config)) }

  //val Scratchpad = Module(new Scratchpad)
  //val Scratchpad = Module(new ScratchpadWrapper)
  val Scratchpad = withClockAndReset(sysClk, sysRst) { Module(new ScratchpadWrapper) }

  //// FRONTEND ////

  FrontEnd.io.AXIST <> io.AXIST_inInst
  Execute.io.instructionStream <> FrontEnd.io.exeStream
  Load.io.instructionStream <> FrontEnd.io.loadStream
  Store.io.instructionStream <> FrontEnd.io.storeStream
  
  FrontEnd.io.event(0) := Load.io.event
  FrontEnd.io.event(1) := Execute.io.eventOut

  //// EXECUTE ////

  Execute.io.scratchOut <> Scratchpad.io.Writeport(0)
  Execute.io.scratchIn(0) <> Scratchpad.io.Readport(0)
  Execute.io.scratchIn(1) <> Scratchpad.io.Readport(1)
  Execute.io.eventIn <> Load.io.event

  //// LOAD //// 

  //Load.io.AXIST <> io.AXIST_inData
  //Load.io.scratchOut <> Scratchpad.io.Writeport(1)

  AXIDataBuffer.io.AXIST <> io.AXIST_inData

  Load.io.AXIData <> AXIDataBuffer.io.deq_out
  //Load.io.scratchOut <> Scratchpad.io.Writeport(1)

  //// STORE //// 

  Store.io.AXIST <> io.AXIST_out
  Store.io.readPort <> Scratchpad.io.Readport(2)
  Store.io.event(0) := Load.io.event
  Store.io.event(1) := Execute.io.eventOut
  

  
  
}

object ATA8 extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new ATA8(Configuration.default()))
}