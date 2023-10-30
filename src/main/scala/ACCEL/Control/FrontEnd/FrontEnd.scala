package ATA8

import chisel3._
import chisel3.experimental._
import chisel3.util._

class FrontEnd(implicit c: Configuration) extends Module {          
  val io = IO(new Bundle {
    //val AXIST = Flipped(new AXIST_2(64,2,1,1,1))
    val AXIData = Flipped(Decoupled(new Bundle{val data = UInt(64.W); val strb = UInt(8.W); val last = Bool()}))
    //val instructionStream = Decoupled(new InstructionPackage)
    val exeStream = Decoupled(new ExecuteInstIssue)
    val loadStream = Decoupled(new LoadInstIssue)
    val storeStream = Decoupled(new StoreInstIssue)
    val event = Vec(2,Flipped(Valid(new Event()))) 
  })

  //val Reciever = Module(new InstReciever)
  //val instQueue = Module(new BufferFIFO(32, new InstructionPackage)) //FIXME: non parameterized constant
  val Decoder = Module(new Decoder(c))
  val ROB = Module(new ROB(c.tagCount,2)(c))

  //Reciever.io.AXIST <> io.AXIST
  //instQueue.io.WriteData <> Reciever.io.instructionStream
  //Decoder.io.instructionStream <> instQueue.io.ReadData

  Decoder.io.instructionStream <> io.AXIData

  Decoder.io.tagFetch <> ROB.io.ReadData
  Decoder.io.tagRegister <> ROB.io.Writeport
  Decoder.io.event := io.event

  ROB.io.event := io.event
  ROB.io.readAddr(0).request.valid := false.B
  ROB.io.readAddr(0).request.bits := DontCare
  ROB.io.readAddr(1).request.valid := false.B
  ROB.io.readAddr(1).request.bits := DontCare

  io.exeStream <> Decoder.io.exeStream
  io.loadStream <> Decoder.io.loadStream
  io.storeStream <> Decoder.io.storeStream

}