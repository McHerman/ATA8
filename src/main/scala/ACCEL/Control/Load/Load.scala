package ATA8

import chisel3._
import chisel3.experimental._
import chisel3.util._

class Load(config: Configuration) extends Module {
  implicit val c = config

  val io = IO(new Bundle {
    val instructionStream = Flipped(Decoupled(new InstructionPackage))
    val event = Flipped(Valid(new Event()))
    val tagFetch = Vec(2,Flipped(new TagRead())) // Incoming fetch 
    val tagDealloc = Flipped(Decoupled(UInt(c.tagWidth.W)))
    val AXIST = Flipped(new AXIST_2(64,2,1,1,1))
    val writeport = new WriteportScratch
    val readAddr = Vec(2 /*FIXME: magic fucking number*/ ,Flipped(new Readport(UInt(c.addrWidth.W), c.tagWidth)))
  })

  val queue = Module(new BufferFIFO(16,new LoadInst))
  val LoadController = Module(new LoadController)
  val TagMap = Module(new TagMap(c.loadTagCount,2,c.exTagCount))

  val inst = io.instructionStream.bits.instruction.asTypeOf(new LoadInst)

  queue.io.WriteData.bits <> inst
  queue.io.WriteData.valid := io.instructionStream.valid
  io.instructionStream.ready := queue.io.WriteData.ready
  LoadController.io.instructionStream <> queue.io.ReadData
  LoadController.io.AXIST <> io.AXIST

  io.writeport <> LoadController.io.writeport
  
  TagMap.io.ReadData <> io.tagFetch
  TagMap.io.tagDealloc <> io.tagDealloc
  TagMap.io.Writeport <> LoadController.io.tagRegister
  TagMap.io.event <> io.event
  TagMap.io.readAddr <> io.readAddr

}

object Load extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new Load(Configuration.default()))
}