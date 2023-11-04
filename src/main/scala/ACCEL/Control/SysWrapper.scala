package ATA8

import chisel3._
import chisel3.util._
  
class SysWrapper(config: Configuration) extends Module {

  implicit val c = config
  
  val io = IO(new Bundle {
    //val in = Flipped(Decoupled(new ExecuteInstIssue))
    val in = new Readport(new ExecuteInstIssue,0)
    val scratchOut = new WriteportScratch
    val scratchIn = Vec(2,new ReadportScratch)
    val event = Valid(new Event)
    //val debug = new ExeDebug
  })

  val SysController = Module(new SysController())
  val SysGrain = Module(new Grain(c))

  SysController.io.in <> io.in
  SysController.io.scratchIn <> io.scratchIn
  SysController.io.scratchOut <> io.scratchOut

  SysController.io.out <> SysGrain.io.in 
  SysController.io.memport <> SysGrain.io.Memport
  SysController.io.readPort <> SysGrain.io.Readport

  SysController.io.sysCompleted <> SysGrain.io.completed

  io.event := SysController.io.event

  /// DEBUG /// 

  //SysController.io.debug <> io.debug
}

object SysWrapper extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new SysWrapper(Configuration.default()))
}
