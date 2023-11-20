package ATA8

import chisel3._
import chisel3.util._

class ACCUFile(val hasDelay: Boolean)(implicit c: Configuration) extends Module {
  var addr_width = log2Ceil(c.grainACCUSize)
  val io = IO(new Bundle {
    val In = Input(Vec(c.dataBusSize, new PEY(c.arithDataWidth,1)))
    val Activate = Input(Bool())
    val ActivateOut = Output(Bool())
    val Shift = Input(Bool())
    //val Memport = Flipped(Decoupled(new Memport_V3(dataWidth*grainWidth,addr_width)))
    //val Memport = Flipped(Decoupled(new Memport_V3(32,addr_width)))
    //val Readport = Flipped(new Readport_V2(c.arithDataWidth*c.dataBusSize,10))
    val Readport = Flipped(new Readport(Vec(c.dataBusSize,UInt(8.W)),0))

    val State = Input(UInt(1.W))
  })

  io.Readport.request.ready := true.B
  io.Readport.response.valid := true.B
  io.Readport.response.bits := DontCare

  val moduleArray = Seq.fill(c.dataBusSize)(Module(new BufferFIFO(c.grainFIFOSize, UInt(c.arithDataWidth.W))))

  val ACCUAct = Reg(Vec(c.dataBusSize,UInt(1.W)))

  val activateIn = Wire(Bool())
  val ActDReg = RegInit(false.B)

  if(hasDelay){
    ActDReg := io.Activate
    activateIn := ActDReg
  }else{
    activateIn := io.Activate
  }

  /* io.Readport.request <>  

  val dataVec = VecInit(moduleArray.map(_.io.ReadData.response.readData)) */

  for(i <- 0 until c.dataBusSize){
    if(i == 0){
      ACCUAct(0) := activateIn
    }else{
      ACCUAct(i) := ACCUAct(i-1)
    }

    moduleArray(i).io.WriteData.valid := false.B
    moduleArray(i).io.ReadData.request.valid := false.B
    moduleArray(i).io.ReadData.request.bits := DontCare

    when(io.Readport.request.valid){
      moduleArray(i).io.ReadData.request.valid := true.B
    }

    io.Readport.response.bits.readData(i) := moduleArray(i).io.ReadData.response.bits.readData // FIXME: All this shit sucks

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

  //io.ActivateOut := ACCUAct(c.dataBusSize)
  io.ActivateOut := ACCUAct.last

}
