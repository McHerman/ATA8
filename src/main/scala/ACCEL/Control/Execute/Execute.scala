package ATA8

import chisel3._
import chisel3.experimental._
import chisel3.util._

class Execute(config: Configuration) extends Module {
  implicit val c = config

  val io = IO(new Bundle {
    val instructionStream = Flipped(Decoupled(new ExecuteInstIssue))
    //val tagDealloc = Decoupled(UInt(c.tagWidth.W))
    val scratchOut = new WriteportScratch
    val scratchIn = Vec(2,new ReadportScratch)
    //val event = Vec(2,Flipped(Valid(new Event())))
    val eventIn = Flipped(Valid(new Event()))
    val eventOut = Valid(new Event)
    val debug = new ExeDebug
  })

  //val queue = Module(new BufferFIFO(16,new StoreInstIssue))
  val queue = Module(new DependTrack(16,new ExecuteInstIssue,2)) 

  val SysWrapper = Module(new SysWrapper(c))

  queue.io.WriteData <> io.instructionStream
  //queue.dependio.event <> io.event
  queue.dependio.event(0) := io.eventIn
  queue.dependio.event(1) := SysWrapper.io.event

  SysWrapper.io.scratchOut <> io.scratchOut
  SysWrapper.io.scratchIn <> io.scratchIn

  SysWrapper.io.in <> queue.io.ReadData

  io.eventOut := SysWrapper.io.event

  /// DEBUG /// 

  SysWrapper.io.debug <> io.debug
}

object Execute extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new Execute(Configuration.default()))
}