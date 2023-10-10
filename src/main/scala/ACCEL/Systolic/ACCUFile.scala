package ATA8

import chisel3._
import chisel3.util._


class ACCUFile(implicit c: Configuration) extends Module {
  var addr_width = log2Ceil(c.grainACCUSize)
  val io = IO(new Bundle {
    val In = Input(Vec(c.grainDim, new PEY(c.arithDataWidth,1)))
    val Activate = Input(Bool())
    val Shift = Input(Bool())
    //val Memport = Flipped(Decoupled(new Memport_V3(dataWidth*grainWidth,addr_width)))
    //val Memport = Flipped(Decoupled(new Memport_V3(32,addr_width)))
    val Readport = Flipped(new Readport_V2(c.arithDataWidth*c.grainDim,10))
    val State = Input(UInt(1.W))
  })

  io.Readport.request.ready := true.B
  io.Readport.response.valid := true.B
  io.Readport.response.bits := DontCare


  val moduleArray = Seq.fill(c.grainDim)(Module(new BufferFIFO(c.grainFIFOSize, UInt(c.arithDataWidth.W))))

  val ACCUAct = Reg(Vec(c.grainDim,UInt(1.W)))
  val ActDReg = RegInit(0.U(1.W))

  ActDReg := io.Activate  

  for(i <- 0 until c.grainDim){
    if(i == 0){
      ACCUAct(0) := ActDReg
    }else{
      ACCUAct(i) := ACCUAct(i-1)
    }

    moduleArray(i).io.WriteData.valid := false.B
    moduleArray(i).io.ReadData.ready := false.B

    when(io.Readport.request.valid){
      moduleArray(i).io.ReadData.ready := true.B
    }

    io.Readport.response.bits.readData(i) := moduleArray(i).io.ReadData.bits 

    switch(io.State){
      is(0.U){
        moduleArray(i).io.WriteData.valid := ACCUAct(i)
      }
      is(1.U){
        moduleArray(i).io.WriteData.valid := io.Shift
      }
    }

    moduleArray(i).io.WriteData.bits := io.In(i).Y

  }
}

/*
object ACCUFile extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new ACCUFile(32,512,8))
}
*/
