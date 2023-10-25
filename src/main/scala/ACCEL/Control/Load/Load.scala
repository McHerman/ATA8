package ATA8

import chisel3._
import chisel3.experimental._
import chisel3.util._

class Load(config: Configuration) extends Module {
  implicit val c = config

  val io = IO(new Bundle {
    val instructionStream = Flipped(Decoupled(new LoadInstIssue))
    val event = Valid(new Event())
    //val tagDealloc = Flipped(Decoupled(UInt(c.tagWidth.W)))
    val AXIST = Flipped(new AXIST_2(64,2,1,1,1))
    val scratchOut = new WriteportScratch
    //val readAddr = Vec(2 /*FIXME: magic fucking number*/ ,Flipped(new Readport(UInt(c.addrWidth.W), c.tagWidth)))
  })

  val queue = Module(new BufferFIFO(16,new LoadInstIssue))
  val LoadController = Module(new LoadController)

  queue.io.WriteData <> io.instructionStream
  
  LoadController.io.instructionStream <> queue.io.ReadData
  LoadController.io.AXIST <> io.AXIST

  io.scratchOut <> LoadController.io.writeport
  io.event <> LoadController.io.event

}

object Load extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new Load(Configuration.default()))
}