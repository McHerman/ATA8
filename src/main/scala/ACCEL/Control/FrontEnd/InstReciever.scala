package ATA8

import chisel3._
import chisel3.experimental._
import chisel3.util._

class InstReciever(implicit c: Configuration) extends Module {          
  val io = IO(new Bundle {
    val AXIST = Flipped(new AXIST_2(64,2,1,1,1))
    val instructionStream = Decoupled(new InstructionPackage)
  })

	io.instructionStream.valid := false.B
	io.instructionStream.bits := DontCare

	io.AXIST.tready := false.B

	when(io.instructionStream.ready){
    io.AXIST.tready := true.B
    when(io.AXIST.tvalid){
      io.instructionStream.valid := true.B
      io.instructionStream.bits.instruction := io.AXIST.tdata
    }
	}
}