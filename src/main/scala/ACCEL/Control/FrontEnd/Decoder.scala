package ATA8

import chisel3._
import chisel3.util._
import chisel3.util.MixedVec._

class ExeInstDecode(implicit c: Configuration) extends Bundle {
  val addrd = UInt(16.W) // Dest Address
  val addrs2 = UInt(16.W)
  val addrs1 = UInt(16.W)
  val size = UInt(8.W)
  val mode = UInt(1.W)
  val op2 = UInt(1.W)
  val op = UInt(2.W) // TODO: magic number
  val func = UInt(4.W)
}

class LoadInstDecode(implicit c: Configuration) extends Bundle {
  val addr = UInt(32.W)
  val size = UInt(8.W)
  val fill = UInt(18.W)
  val mode = UInt(1.W)  
  val op = UInt(1.W) // TODO: magic number  
  val func = UInt(4.W)
} 

class StoreInstDecode(implicit c: Configuration) extends Bundle {
  val addr = UInt(32.W)
  val size = UInt(8.W)
  val fill = UInt(19.W)
  val op = UInt(1.W) // TODO: magic number
  val func = UInt(4.W)
} 

class Decoder(implicit c: Configuration) extends Module {
//class Decoder(config: Configuration) extends Module {

  val io = IO(new Bundle {
    val instructionStream = Flipped(Decoupled(new InstructionPackage))
    //val instructionStream = new Readport(new InstructionPackage,0) 
    //val issueStream = Decoupled(new Bundle{val op = UInt(2.W); val data = MixedVec(new ExeInstDecode, new LoadInstDecode, new StoreInstDecode)})
    val issueStream = Decoupled(new Bundle{val op = UInt(4.W); val data = MixedVec(new ExecuteInst, new LoadInst, new StoreInst)})    
  })
  
  //io.instructionStream.request.valid := false.B
  //io.instructionStream.request.bits := DontCare

  io.instructionStream.ready := false.B

  io.issueStream.valid := false.B

  val inReg = Reg(new InstructionPackage)
  //val storeFile = Reg(new Bundle{val op = UInt(4.W); val data = MixedVec(Seq(new ExeInstDecode, new LoadInstDecode, new StoreInstDecode))})
  
  val stall = WireDefault(false.B)

  when(!stall){
    /* when(io.instructionStream.request.ready){
      io.instructionStream.request.valid := true.B
      when(io.instructionStream.response.valid){
        inReg := io.instructionStream.response.bits.readData
      }
    }.otherwise{
      inReg.instruction := 0.U
    } */

    io.instructionStream.ready := true.B

    when(io.instructionStream.valid){
      inReg := io.instructionStream.bits
    }.otherwise{
      inReg.instruction := 0.U
    }


  }

  val temp = Wire(MixedVec(new ExeInstDecode, new LoadInstDecode, new StoreInstDecode))

  io.issueStream.bits.op := inReg.instruction(3,0)
  /* io.issueStream.bits.data.foreach{decode => 
    decode := inReg.instruction.asTypeOf(decode.cloneType)
  } */

  temp.foreach{decode => 
    decode := inReg.instruction.asTypeOf(decode.cloneType)
  }

  /* io.issueStream.bits.data.zipWithIndex.foreach{case (decode,i) => 
    decode.op := temp(i).op
    decode.size := temp(i).size
  } */

  io.issueStream.bits.data(0).asInstanceOf[ExecuteInst].addrs(0).addr := temp(0).asInstanceOf[ExeInstDecode].addrs1
  io.issueStream.bits.data(0).asInstanceOf[ExecuteInst].addrs(1).addr := temp(0).asInstanceOf[ExeInstDecode].addrs2
  io.issueStream.bits.data(0).asInstanceOf[ExecuteInst].addrd(0).addr := temp(0).asInstanceOf[ExeInstDecode].addrd
  io.issueStream.bits.data(0).asInstanceOf[ExecuteInst].op := temp(0).asInstanceOf[ExeInstDecode].op2
  io.issueStream.bits.data(0).asInstanceOf[ExecuteInst].size := temp(0).asInstanceOf[ExeInstDecode].size
  io.issueStream.bits.data(0).asInstanceOf[ExecuteInst].mode := temp(0).asInstanceOf[ExeInstDecode].mode
  //io.issueStream.bits.data(0).asInstanceOf[ExecuteInst].grainSize := temp(0).asInstanceOf[ExeInstDecode].grainSize


  io.issueStream.bits.data(1).asInstanceOf[LoadInst].addrd(0).addr := temp(1).asInstanceOf[LoadInstDecode].addr
  io.issueStream.bits.data(1).asInstanceOf[LoadInst].op := temp(1).asInstanceOf[LoadInstDecode].op
  io.issueStream.bits.data(1).asInstanceOf[LoadInst].size := temp(1).asInstanceOf[LoadInstDecode].size
  io.issueStream.bits.data(1).asInstanceOf[LoadInst].mode := temp(1).asInstanceOf[LoadInstDecode].mode


  io.issueStream.bits.data(2).asInstanceOf[StoreInst].addrs(0).addr := temp(2).asInstanceOf[StoreInstDecode].addr
  io.issueStream.bits.data(2).asInstanceOf[StoreInst].op := temp(2).asInstanceOf[StoreInstDecode].op
  io.issueStream.bits.data(2).asInstanceOf[StoreInst].size := temp(2).asInstanceOf[StoreInstDecode].size
  //io.issueStream.bits.data(2).asInstanceOf[StoreInst].mode := temp(2).asInstanceOf[StoreInstDecode].mode

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
