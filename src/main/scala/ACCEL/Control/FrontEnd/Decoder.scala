package ATA8

import chisel3._
import chisel3.util._
import chisel3.util.MixedVec._

class Decoder(implicit c: Configuration) extends Module {
//class Decoder(config: Configuration) extends Module {

  val io = IO(new Bundle {
    //val instructionStream = Flipped(Decoupled(new InstructionPackage))
    val instructionStream = new Readport(new InstructionPackage,0)
    
    val issueStream = Decoupled(new Bundle{val op = UInt(2.W); val data = MixedVec(new ExeInstDecode, new LoadInstDecode, new StoreInstDecode)})
  })
  
  io.instructionStream.request.valid := false.B
  io.instructionStream.request.bits := DontCare

  io.issueStream.valid := false.B

  val inReg = Reg(new InstructionPackage)
  //val storeFile = Reg(new Bundle{val op = UInt(4.W); val data = MixedVec(Seq(new ExeInstDecode, new LoadInstDecode, new StoreInstDecode))})
  
  val stall = WireDefault(false.B)

  when(!stall){
    when(io.instructionStream.request.ready){
      io.instructionStream.request.valid := true.B
      when(io.instructionStream.response.valid){
        inReg := io.instructionStream.response.bits.readData
      }
    }.otherwise{
      inReg.instruction := 0.U
    }
  }

  io.issueStream.bits.op := inReg.instruction(3,0)
  io.issueStream.bits.data.foreach{decode => 
    decode := inReg.instruction.asTypeOf(decode.cloneType)
  }

  when(inReg.instruction(3,0) =/= 0.U){
    when(io.issueStream.ready){
      io.issueStream.valid := true.B
    }.otherwise{
      stall := true.B
    }
  }




  /* when(!stall){
    storeFile.op := inReg.instruction(3,0)
    storeFile.data.foreach{decode => 
      decode := inReg.instruction.asTypeOf(decode.cloneType)
    }
  }

  io.issueStream := storeFile

  when(storeFile.op =/= 0.U){
    when(io.issueStream.ready){
      io.issueStream.valid := true.B
    }.otherwise{
      stall := true.B
    }
  } */
}