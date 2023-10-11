package ATA8

import chisel3._
import chisel3.util._


class XFile(implicit c: Configuration) extends Module {
  var addr_width = log2Ceil(c.grainFIFOSize)
  val io = IO(new Bundle {
    val Out = Output(Vec(c.grainDim, new PEX(c.arithDataWidth)))
    val Activate = Input(Bool())
    val ActivateOut = Output(Bool())
    val Memport = Flipped(Decoupled(new Memport_V3(c.arithDataWidth*c.grainDim,addr_width)))
  })

  io.Memport.ready := true.B
  io.Memport.bits.readData := DontCare

  val moduleArray = Seq.fill(c.grainDim)(Module(new BufferFIFO(c.grainFIFOSize, UInt(c.arithDataWidth.W))))

  val XACT = Reg(Vec(c.grainDim,UInt(1.W)))
  
  for(i <- 0 until c.grainDim){
    if(i == 0){
      XACT(0) := io.Activate
    }else{
      XACT(i) := XACT(i-1)
    }

    moduleArray(i).io.WriteData.valid := false.B
    moduleArray(i).io.WriteData.bits := 0.U

    when(io.Memport.valid){
      when(io.Memport.bits.wenable){
        moduleArray(i).io.WriteData.valid := true.B
        moduleArray(i).io.WriteData.bits := io.Memport.bits.writeData(i)
      }
    }
    
    moduleArray(i).io.ReadData.request.valid := XACT(i)
    moduleArray(i).io.ReadData.request.bits := DontCare

    when(moduleArray(i).io.ReadData.request.valid){
      io.Out(i).X := moduleArray(i).io.ReadData.response.bits.readData
    }.otherwise{
      io.Out(i).X := 0.U
    }
  }

  io.ActivateOut := XACT(c.grainDim-1)

  
  
}

/*
object XFile extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new XFile(32,512,8))
}
*/
