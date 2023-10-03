package ATA8

import chisel3._
import chisel3.util._

class Decoder(implicit c: Configuration) extends Module {

  val io = IO(new Bundle {
    val instructionStream = Flipped(Decoupled(new InstructionPackage))
    val issueStream = Decoupled(new ExecuteInst)
    val idFetch = Vec(2,Flipped(Decoupled(new Bundle {val tag = Output(UInt(c.tagWidth.W)); val addr = Input(UInt(c.addrWidth.W))})))
    val idRegister = Decoupled(new Bundle {val addr = Output(UInt(c.addrWidth.W)); val tag = Input(UInt(c.tagWidth.W))})
  })

  val registerFile = Reg(new InstructionPackage)

  val instruction = registerFile.instruction

  val size = instruction(8, 15)
  val addrd = instruction(16, 31)
  val addrs = VecInit(instruction(32, 47), instruction(48, 63))

  io.idFetch.zipWithIndex.foreach { case (element,i) => element.ready := true.B; element.bits.addr := addrs(i) }
  io.idRegister.bits.addr := addrd

  val writeCompleted = RegInit(UInt(1.W))
  val iddtag = RegInit(UInt(c.tagWidth.W))

  when(!writeCompleted){
    io.idRegister.ready := true.B

    when(io.io.idRegister.valid){
      iddtag := io.idRegister.bits.tag
      writeCompleted := true.B
    }
  }


  when(io.idFetch(0).valid && io.idFetch(1).valid){
    when(io.io.idRegister.valid){
      // Issue 
    }.elsewhen(writeCompleted){
      // Issue from register
    }


  } 

}