package ATA8

import chisel3._
import chisel3.experimental._
import chisel3.util._

class Execute(config: Configuration) extends Module {
  implicit val c = config

  val io = IO(new Bundle {
    val instructionStream = Flipped(Decoupled(new ExecuteInstIssue))
    //val tagDealloc = Decoupled(UInt(c.tagWidth.W))
    //val scratchOut = new WriteportScratch
    val scratchOut = Vec(c.grainDim,new WriteportScratch)
    val scratchIn = Vec(2,new ReadportScratch)
    //val event = Vec(2,Flipped(Valid(new Event())))
    val eventIn = Flipped(Valid(new Event()))
    val eventOut = Valid(new Event)
    val debug = new ExeDebug
  })

  //val queue = Module(new BufferFIFO(16,new StoreInstIssue))
  val queue = Module(new DependTrack(32,new ExecuteInstIssue,2)) // TODO: add to config

  val SysWrapper = Module(new SysWrapper(c))

  queue.io.WriteData <> io.instructionStream
  //queue.dependio.event <> io.event
  queue.dependio.event(0) := io.eventIn
  queue.dependio.event(1) := SysWrapper.io.event

  //SysWrapper.io.scratchOut <> io.scratchOut
  //SysWrapper.io.scratchIn <> io.scratchIn

  SysWrapper.io.in <> queue.io.ReadData

  /// SCRATCHPAD CONNECTIONS /// 

  if(c.grainDim != 1){
    //val WriteArbiter = Module(new ScratchWriteArbiter(c.grainDim))
    val ReadArbiter = Seq.fill(2)(Module(new ScratchReadArbiter(c.grainDim)))  

    /* WriteArbiter.io.inPorts <> SysWrapper.io.scratchOut
    io.scratchOut <> WriteArbiter.io.outPort */

    io.scratchOut <> SysWrapper.io.scratchOut

    ReadArbiter(0).io.inPorts <> SysWrapper.io.scratchIn(0) //FIXME: A bit verbose
    ReadArbiter(1).io.inPorts <> SysWrapper.io.scratchIn(1)

    io.scratchIn(0) <> ReadArbiter(0).io.outPort
    io.scratchIn(1) <> ReadArbiter(1).io.outPort
  }else{
    //SysWrapper.io.scratchOut(0) <> io.scratchOut

    SysWrapper.io.scratchOut <> io.scratchOut

    SysWrapper.io.scratchIn(0)(0) <> io.scratchIn(0)
    SysWrapper.io.scratchIn(1)(0) <> io.scratchIn(1)
  }
 
  io.eventOut := SysWrapper.io.event

  /// DEBUG /// 

  SysWrapper.io.debug <> io.debug
}

object Execute extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new Execute(Configuration.default()))
}