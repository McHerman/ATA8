package ATA8

import chisel3._
import chisel3.experimental._
import chisel3.util._

class FrontEnd(implicit c: Configuration) extends Module {          
  val io = IO(new Bundle {
    val AXIST = Flipped(new AXIST_2(64,2,1,1,1))

    val exeStream = Decoupled(new ExecuteInstIssue)
    val loadStream = Decoupled(new LoadInstIssue)
    val storeStream = Decoupled(new StoreInstIssue)
    val event = Vec(2,Flipped(Valid(new Event()))) 
    val receiverDebug = Valid(UInt(64.W))
    val decodeDebug = Valid(new LoadInst)

    val frontEndDebug = Output(new Bundle{val decodeReady = Bool(); val ROBFetchReady = Bool(); val exeOutReady = Bool(); val loadOutReady = Bool(); val storeOutReady = Bool()})
    val robDebug = Output(Vec(c.tagCount,new mapping()))
  })

  val Reciever = Module(new InstReciever)
  //val instQueue = Module(new BufferFIFO(16, new InstructionPackage)) //FIXME: non parameterized constant
  val instQueue = Module(new Queue(new InstructionPackage, 32))
  val Decoder = Module(new Decoder)
  val ROBFetch = Module(new ROBFetch)
  val ROB = Module(new ROB(c.tagCount,2,0)(c))

  io.receiverDebug.valid := Reciever.io.instructionStream.valid
  io.receiverDebug.bits := Reciever.io.instructionStream.bits.instruction
  io.robDebug <> ROB.io.debug

  Reciever.io.AXIST <> io.AXIST

  instQueue.io.enq <> Reciever.io.instructionStream
  Decoder.io.instructionStream <> instQueue.io.deq

  io.decodeDebug.valid := Decoder.io.issueStream.valid
  io.decodeDebug.bits := Decoder.io.issueStream.bits.data(1)

  ROBFetch.io.instructionStream <> Decoder.io.issueStream
  ROBFetch.io.tagFetch <> ROB.io.ReadData
  ROBFetch.io.tagRegister <> ROB.io.Writeport
  ROBFetch.io.event := io.event

  ROB.io.event := io.event

  io.exeStream <> ROBFetch.io.issueStream(0)
  io.loadStream <> ROBFetch.io.issueStream(1)
  io.storeStream <> ROBFetch.io.issueStream(2)

  io.frontEndDebug.decodeReady := Decoder.io.instructionStream.ready
  io.frontEndDebug.ROBFetchReady := ROBFetch.io.instructionStream.ready 
  io.frontEndDebug.exeOutReady := io.exeStream.ready
  io.frontEndDebug.loadOutReady := io.loadStream.ready
  io.frontEndDebug.storeOutReady := io.storeStream.ready

}