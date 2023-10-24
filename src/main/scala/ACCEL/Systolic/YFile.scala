package ATA8

import chisel3._
import chisel3.util._


class YFile(implicit c: Configuration) extends Module {
  var addr_width = log2Ceil(c.grainFIFOSize)
  val io = IO(new Bundle {
    val Out = Output(Vec(c.grainDim, new PEY(c.arithDataWidth,1)))
    val Activate = Input(Bool())
    val Enable = Input(Bool())
    val Shift = Input(Bool())
    val Memport = Flipped(Decoupled(new Memport(Vec(c.grainDim,UInt(c.arithDataWidth.W)), addr_width)))
    val State = Input(UInt(1.W))
  })

  io.Memport.ready := true.B
  io.Memport.bits.readData := DontCare
 

  val moduleArray = Seq.fill(c.grainDim)(Module(new BufferFIFO(c.grainFIFOSize, UInt(c.arithDataWidth.W))))

  val YACT = Reg(Vec(c.grainDim,UInt(1.W)))
  val YEn = Reg(Vec(c.grainDim,UInt(1.W)))

  val EnDelayReg = RegInit(0.U(1.W))

  //EnDelayReg := io.Enable
  
  for(i <- 0 until c.grainDim){
    if(i == 0){
      YACT(0) := io.Activate
      //YEn(0) := EnDelayReg
      YEn(0) := io.Enable
    }else{
      YACT(i) := YACT(i-1)
      YEn(i) := YEn(i-1)
    }

    io.Out(i).PEState.Shift := false.B
    io.Out(i).PEState.EN := YEn(i)

    io.Out(i).PEState.State := io.State

    when(moduleArray(i).io.ReadData.response.valid){
      io.Out(i).Y := moduleArray(i).io.ReadData.response.bits.readData
    }.otherwise{
      io.Out(i).Y := 0.U
    }

    moduleArray(i).io.WriteData.valid := false.B
    moduleArray(i).io.WriteData.bits := 0.U

    when(io.Memport.valid){
      when(io.Memport.bits.wenable){
        moduleArray(i).io.WriteData.valid := true.B
        moduleArray(i).io.WriteData.bits := io.Memport.bits.writeData(i)
      }
    }

    moduleArray(i).io.ReadData.request.valid := false.B
    moduleArray(i).io.ReadData.request.bits := DontCare

    val shiftdelay = RegInit(0.U(1.W))

    switch(io.State){
      is(0.U){
        /* shiftdelay := io.Shift
        moduleArray(i).io.ReadData.request.valid := io.Shift
        io.Out(i).PEState.Shift := shiftdelay */

        moduleArray(i).io.ReadData.request.valid := io.Shift
        io.Out(i).PEState.Shift := io.Shift
      }
      is(1.U){
        moduleArray(i).io.ReadData.request.valid := YACT(i)
        io.Out(i).PEState.Shift := io.Shift
      }
    }
  }
}
