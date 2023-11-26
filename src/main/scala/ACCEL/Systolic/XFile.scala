package ATA8

import chisel3._
import chisel3.util._


class XFile(implicit c: Configuration) extends Module {
  var addr_width = log2Ceil(c.grainFIFOSize)
  val io = IO(new Bundle {
    val Out = Output(Vec(c.dataBusSize, new PEX(c.arithDataWidth)))
    val Activate = Input(Bool())
    val ActivateOut = Output(Bool())

    val Memport = Flipped(Decoupled(Vec(c.dataBusSize,UInt(8.W)))) //TODO: change name 
    val size = Input(UInt(log2Ceil(c.dataBusSize + 1).W))
  })

  val moduleArray = Seq.fill(c.dataBusSize)(Module(new BufferFIFO(c.grainFIFOSize, UInt(8.W))))
  val XACT = Reg(Vec(c.dataBusSize,UInt(1.W)))
  
  for(i <- 0 until c.dataBusSize){
    if(i == 0){
      XACT(0) := io.Activate
    }else{
      XACT(i) := XACT(i-1)
    }

    moduleArray(i).io.WriteData.valid := io.Memport.valid
    moduleArray(i).io.WriteData.bits := io.Memport.bits(i)
    
    moduleArray(i).io.ReadData.request.valid := false.B
    moduleArray(i).io.ReadData.request.bits := DontCare

    when(io.size =/= 0.U){
      moduleArray(i).io.ReadData.request.valid := XACT(i)
    }

    when(moduleArray(i).io.ReadData.request.valid){
      io.Out(i).X := moduleArray(i).io.ReadData.response.bits.readData
    }.otherwise{
      io.Out(i).X := 0.U
    }
  }

  io.ActivateOut := XACT.last
  io.Memport.ready := VecInit(moduleArray.map(_.io.WriteData.ready)).reduceTree(_ && _)
  
}

