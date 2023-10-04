package ATA8

import chisel3._
import chisel3.util._

class Decoder(implicit c: Configuration) extends Module {

  val io = IO(new Bundle {
    val instructionStream = Flipped(Decoupled(new InstructionPackage))
    val issueStream = Decoupled(new ExecuteInst)
    val idFetch = Vec(2,Flipped(Decoupled(new Bundle {val tag = Output(UInt(c.tagWidth.W)); val ready = Output(Bool()); val addr = Input(UInt(c.addrWidth.W))})))
    val idRegister = Decoupled(new Bundle {val addr = Output(UInt(c.addrWidth.W)); val tag = Input(UInt(c.tagWidth.W))})
  })

  val registerFile = Reg(new InstructionPackage)

  when(pipeReady || readyReg.asBool){
    io.instructionStream.ready := true.B
    when(io.instructionStream.valid){
      registerFile := io.instructionStream.bits
    }
  }


  val instruction = registerFile.instruction

  val op = instruction(0,3)
  val grainSize = instruction(4,7)
  val size = instruction(8, 15)
  val addrd = instruction(16, 31)
  val addrs = VecInit(instruction(32, 47), instruction(48, 63))

  io.issueStream.bits.op := op(0)
  io.issueStream.bits.mode := op(1)
  io.issueStream.bits.grainSize := grainSize
  io.issueStream.bits.size := size

  io.idFetch.zipWithIndex.foreach { case (element,i) => element.ready := true.B; element.bits.addr := addrs(i) }
  io.idRegister.bits.addr := addrd

  val writeCompleted = RegInit(UInt(1.W))
  val iddtag = RegInit(UInt(c.tagWidth.W))

  when(!writeCompleted){
    io.idRegister.ready := true.B

    when(io.idRegister.valid){
      iddtag := io.idRegister.bits.tag
      writeCompleted := true.B
    }
  }

  val pipeReady = WireDefault (false.B)
  val readyReg = RegInit(0.U(1.W))

  when(io.idFetch(0).valid && io.idFetch(1).valid){
    io.issueStream.bits.ids.zipWithIndex.foreach{ case (element,i) => element.ready := io.idFetch(i).ready; element.id := io.idFetch(i).bits.tag} 

    when(io.issueStream.ready){
      when(io.idRegister.valid){
        io.issueStream.bits.idd := io.idRegister.bits.tag
        io.issueStream.valid := true.B
        pipeReady := true.B
        readyReg := true.B
      }.elsewhen(writeCompleted.asBool){
        io.issueStream.bits.idd := iddtag
        io.issueStream.valid := true.B
        pipeReady := true.B
        readyReg := true.B
      }        
    }
  } 
}