package ATA8

import chisel3._
import chisel3.util._

class ScratchpadWrapper(implicit c: Configuration) extends Module {
  val io = IO(new Bundle {
  	//val Writeport = Vec(c.bufferWritePorts, Flipped(new WriteportScratch())) //FIXME: HACKY AF, fix 
  	val Writeport = Vec(1 + c.grainDim, Flipped(new WriteportScratch())) //FIXME: HACKY AF, fix 
    val Readport = Vec(c.bufferReadPorts, Flipped(new ReadportScratch()))
  })


  //val WriteArbiter = Module(new ScratchWriteArbiter(c.bufferWritePorts))
  val WriteArbiter = Module(new ScratchWriteArbiter(1 + c.grainDim))
  val Scratchpad = Module(new Scratchpad(1)) 

  val ReadBurstHandlerVec = Seq.fill(c.bufferReadPorts)(Module(new ReadBurstHandler))
  val WriteBurstHandlerVec = Seq.fill(1)(Module(new WriteBurstHandler))

  WriteArbiter.io.inPorts <> io.Writeport
  //Arbiter.io.outPort <> Scratchpad.io.Writeport(0)

  //Arbiter.io.outPort.zipWithIndex.foreach{case (port,i) => port <> WriteBurstHandlerVec(i).io.scratchWriteport; Scratchpad.io.writePort(i) <> WriteBurstHandlerVec(i).io.writePort}
  
  WriteArbiter.io.outPort <> WriteBurstHandlerVec(0).io.scratchWriteport
  Scratchpad.io.Writeport(0) <> WriteBurstHandlerVec(0).io.writePort
  
  io.Readport.zipWithIndex.foreach{case (port,i) => port <> ReadBurstHandlerVec(i).io.scratchReadport; Scratchpad.io.Readport(i) <> ReadBurstHandlerVec(i).io.readPort} 

  //Scratchpad.io.Readport <> io.Readport

}