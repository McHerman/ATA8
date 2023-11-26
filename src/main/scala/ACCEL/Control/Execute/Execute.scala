package ATA8

import chisel3._
import chisel3.experimental._
import chisel3.util._

class Execute(implicit c: Configuration) extends Module {
  val io = IO(new Bundle {
    val instructionStream = Flipped(Decoupled(new ExecuteInstIssue))

    val scratchOut = Vec(c.grainDim,new WriteportScratch)
    val scratchIn = Vec(2,new ReadportScratch)

    val eventIn = Flipped(Valid(new Event()))
    val eventOut = Valid(new Event)
    val debug = new ExeDebug
  })

  val queue = Module(new DependTrack(32,new ExecuteInstIssue,2)) // TODO: add to config

  val SysWrapper = Module(new SysWrapper)

  queue.io.WriteData <> io.instructionStream

  queue.dependio.event(0) := io.eventIn
  queue.dependio.event(1) := SysWrapper.io.event

  SysWrapper.io.in <> queue.io.ReadData

  /// SCRATCHPAD CONNECTIONS /// 

  if(c.grainDim != 1){ // Adds arbiters when system is larger that 1 x 1
    val ReadArbiter = Seq.fill(2)(Module(new ScratchReadArbiter(c.grainDim)))  

    io.scratchOut <> SysWrapper.io.scratchOut

    ReadArbiter(0).io.inPorts <> SysWrapper.io.scratchIn(0) //FIXME: A bit verbose
    ReadArbiter(1).io.inPorts <> SysWrapper.io.scratchIn(1)

    io.scratchIn(0) <> ReadArbiter(0).io.outPort
    io.scratchIn(1) <> ReadArbiter(1).io.outPort
  }else{
    SysWrapper.io.scratchOut <> io.scratchOut

    SysWrapper.io.scratchIn(0)(0) <> io.scratchIn(0)
    SysWrapper.io.scratchIn(1)(0) <> io.scratchIn(1)
  }
 
  io.eventOut := SysWrapper.io.event

  /// DEBUG /// 

  SysWrapper.io.debug <> io.debug
}
