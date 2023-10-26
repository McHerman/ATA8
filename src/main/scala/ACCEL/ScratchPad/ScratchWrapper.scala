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

  Arbiter.io.inPorts <> io.Writeport
  Arbiter.io.outPort <> Scratchpad.io.Writeport(0)

  Scratchpad.io.Readport <> io.Readport

}