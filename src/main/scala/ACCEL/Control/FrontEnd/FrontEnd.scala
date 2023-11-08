package ATA8

import chisel3._
import chisel3.experimental._
import chisel3.util._

class FrontEnd(implicit c: Configuration) extends Module {          
  val io = IO(new Bundle {
    val AXIST = Flipped(new AXIST_2(64,2,1,1,1))
    //val instructionStream = Decoupled(new InstructionPackage)
    val exeStream = Decoupled(new ExecuteInstIssue)
    val loadStream = Decoupled(new LoadInstIssue)
    val storeStream = Decoupled(new StoreInstIssue)
    val event = Vec(2,Flipped(Valid(new Event()))) 
    val debug = Valid(UInt(64.W))
    val robDebug = Output(Vec(c.tagCount,new mapping()))
  })

  val Reciever = Module(new InstReciever)
  val instQueue = Module(new BufferFIFO(16, new InstructionPackage)) //FIXME: non parameterized constant
  val Decoder = Module(new Decoder)
  val ROBFetch = Module(new ROBFetch)
  val ROB = Module(new ROB(c.tagCount,2)(c))

  io.debug.valid := Reciever.io.instructionStream.valid
  io.debug.bits := Reciever.io.instructionStream.bits.instruction
  io.robDebug <> ROB.io.debug

  Reciever.io.AXIST <> io.AXIST
  instQueue.io.WriteData <> Reciever.io.instructionStream
  
  Decoder.io.instructionStream <> instQueue.io.ReadData
  //Decoder.io.tagFetch <> ROB.io.ReadData
  //Decoder.io.tagRegister <> ROB.io.Writeport
  //Decoder.io.event := io.event

  ROBFetch.io.instructionStream <> Decoder.io.issueStream
  ROBFetch.io.tagFetch <> ROB.io.ReadData
  ROBFetch.io.tagRegister <> ROB.io.Writeport
  ROBFetch.io.event := io.event

  ROB.io.event := io.event
  ROB.io.readAddr(0).request.valid := false.B
  ROB.io.readAddr(0).request.bits := DontCare
  ROB.io.readAddr(1).request.valid := false.B
  ROB.io.readAddr(1).request.bits := DontCare

  //io.exeStream <> Decoder.io.exeStream
  //io.loadStream <> Decoder.io.loadStream
  //io.storeStream <> Decoder.io.storeStream

  io.exeStream <> ROBFetch.io.issueStream(0)
  io.loadStream <> ROBFetch.io.issueStream(1)
  io.storeStream <> ROBFetch.io.issueStream(2)

}