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
    val axi_s0 = Flipped(new CustomAXI4Lite(32, 32))
  })

  val FrontEnd = Module(new FrontEnd)

  val Execute = Module(new Execute(config))
  val Load = Module(new Load(config))
  val Store = Module(new Store(config))

  //val Scratchpad = Module(new Scratchpad)
  val Scratchpad = Module(new ScratchpadWrapper)

  val Config = Module(new Config())

  //// FRONTEND ////

  FrontEnd.io.AXIST <> io.AXIST_inInst
  Execute.io.instructionStream <> FrontEnd.io.exeStream
  Load.io.instructionStream <> FrontEnd.io.loadStream
  Store.io.instructionStream <> FrontEnd.io.storeStream
  
  FrontEnd.io.event(0) := Load.io.event
  FrontEnd.io.event(1) := Execute.io.eventOut

  //// EXECUTE ////

  //Execute.io.scratchOut <> Scratchpad.io.Writeport(0)
  Execute.io.scratchIn(0) <> Scratchpad.io.Readport(0)
  Execute.io.scratchIn(1) <> Scratchpad.io.Readport(1)
  Execute.io.eventIn <> Load.io.event

  //// LOAD //// 

  Load.io.AXIST <> io.AXIST_inData
  //Load.io.scratchOut <> Scratchpad.io.Writeport(1)

  //// STORE //// 

  Store.io.AXIST <> io.AXIST_out
  Store.io.readPort <> Scratchpad.io.Readport(2)
  Store.io.event(0) := Load.io.event
  Store.io.event(1) := Execute.io.eventOut

  //// SCRATCHPAD //// 

  //Scratchpad.io.Writeport <> VecInit(Execute.io.scratchOut ++ Load.io.scratchOut)
  Scratchpad.io.Writeport <> VecInit(Execute.io.scratchOut ++ VecInit(Seq(Load.io.scratchOut)))

  /// DEBUG /// 

  Config.io.axi_s0 <> io.axi_s0
  Config.io.loadDebug <> Load.io.debug
  Config.io.storeDebug <> Store.io.debug
  Config.io.exeDebug <> Execute.io.debug
  Config.io.receiverDebug <> FrontEnd.io.receiverDebug
  Config.io.decodeDebug <> FrontEnd.io.decodeDebug
  Config.io.event(0) := Load.io.event
  Config.io.event(1) := Execute.io.eventOut
  Config.io.robDebug := FrontEnd.io.robDebug

  Config.io.decodeOutLoad.bits := FrontEnd.io.loadStream.bits
  Config.io.decodeOutLoad.valid := FrontEnd.io.loadStream.fire

  Config.io.AXIDebug.data_ready := Load.io.AXIST.tready
  Config.io.AXIDebug.data_valid := io.AXIST_inData.tvalid

  Config.io.AXIDebug.inst_ready := FrontEnd.io.AXIST.tready
  Config.io.AXIDebug.inst_valid := io.AXIST_inInst.tvalid

  Config.io.AXIDebug.out_ready := io.AXIST_out.tready
  Config.io.AXIDebug.out_valid := Store.io.AXIST.tvalid

  Config.io.frontEndDebug := FrontEnd.io.frontEndDebug
}

object ATA8 extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new ATA8(Configuration.default()))
}