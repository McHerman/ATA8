package ATA8

import chisel3._
import chisel3.util._

class ScratchpadWrapper(implicit c: Configuration) extends Module {
  val io = IO(new Bundle {
  	//val Readport = Vec(c.bufferReadPorts, Flipped(new ReadportBuf(c.arithDataWidth,10)))
  	//val Writeport = Vec(c.bufferWritePorts, Flipped(new WriteportBuf(c.arithDataWidth,10)))
  	val Writeport = Vec(c.bufferWritePorts, Flipped(new WriteportScratch()))
    val Readport = Vec(c.bufferReadPorts, Flipped(new ReadportScratch()))
  })

  val Arbiter = Module(new ScratchArbiter(c.bufferWritePorts))
  val Scratchpad = Module(new Scratchpad(1)) 

  val ReadBurstHandlerVec = Seq.fill(c.bufferReadPorts)(Module(new ReadBurstHandler))
  val WriteBurstHandlerVec = Seq.fill(1)(Module(new WriteBurstHandler))

  Arbiter.io.inPorts <> io.Writeport
  //Arbiter.io.outPort <> Scratchpad.io.Writeport(0)

  //Arbiter.io.outPort.zipWithIndex.foreach{case (port,i) => port <> WriteBurstHandlerVec(i).io.scratchWriteport; Scratchpad.io.writePort(i) <> WriteBurstHandlerVec(i).io.writePort}
  
  Arbiter.io.outPort <> WriteBurstHandlerVec(0).io.scratchWriteport
  Scratchpad.io.Writeport(0) <> WriteBurstHandlerVec(0).io.writePort
  
  io.Readport.zipWithIndex.foreach{case (port,i) => port <> ReadBurstHandlerVec(i).io.scratchReadport; Scratchpad.io.Readport(i) <> ReadBurstHandlerVec(i).io.readPort} 

  //Scratchpad.io.Readport <> io.Readport

}