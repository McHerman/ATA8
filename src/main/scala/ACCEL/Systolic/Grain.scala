package ATA8

import chisel3._
import chisel3.util._
  
class Grain(config: Configuration) extends Module {

  implicit val c = config
  
  val io = IO(new Bundle {
    //val State = Input(UInt(1.W))
    //val Size = Input(UInt(8.W))
    val in = Flipped(Decoupled(new SysOP)) 
    //val Memport = Flipped(Decoupled(new Memport_V3(c.arithDataWidth*c.grainDim,10))) // Add actual memport
    val Memport = Vec(2,Flipped(Decoupled(new Memport_V3(c.arithDataWidth*c.grainDim,10)))) // TODO: Change to more streamlined memport
    val Readport = new Readport(Vec(c.grainDim,UInt(c.arithDataWidth.W)),10)
    val completed = Valid(new Bundle{val id = UInt(4.W)})
  })

  val XFile = Module(new XFile())
  val YFile = Module(new YFile())
  val ACCUFile = Module(new ACCUFile())
  val SysCtrl = Module(new SysCtrl())

  SysCtrl.io.in <> io.in
  io.completed := SysCtrl.io.completed

  XFile.io.Memport <> io.Memport(0)
  YFile.io.Memport <> io.Memport(1)

  ACCUFile.io.Readport <> io.Readport
  ACCUFile.io.Readport.request.valid := false.B
  ACCUFile.io.Readport.request.bits := DontCare

  XFile.io.Activate := SysCtrl.io.Activate
  YFile.io.Activate := SysCtrl.io.Activate
  YFile.io.Enable := SysCtrl.io.Enable
  
  ACCUFile.io.Activate := XFile.io.ActivateOut

  YFile.io.State :=  SysCtrl.io.Mode // FIXME: kinda poopy, add another bit
  ACCUFile.io.State :=  SysCtrl.io.Mode
  
  YFile.io.Shift := SysCtrl.io.Shift
  ACCUFile.io.Shift := SysCtrl.io.Shift

  val peArray = Seq.fill(c.grainDim, c.grainDim)(Module(new PE())) // Create a 2D array of PE modules

  for(i <- 0 until c.grainDim){
    for(k <- 0 until c.grainDim){
      if(i != c.grainDim-1){
        peArray(i)(k).io.X_OUT <> peArray(i+1)(k).io.X_IN
      }

      if(k != c.grainDim-1){
        peArray(i)(k).io.Y_OUT <> peArray(i)(k+1).io.Y_IN
      }else{ 
        ACCUFile.io.In(i) <> peArray(i)(k).io.Y_OUT
      }


    }  
  }

  for(i <- 0 until c.grainDim){
    peArray(0)(i).io.X_IN <> XFile.io.Out(i)
  }

  for(i <- 0 until c.grainDim){
    peArray(i)(0).io.Y_IN <> YFile.io.Out(i)
    //ACCUFile.io.In(i) <> peArray(i)(grainWidth-1).io.Y_OUT
  }
  

}

object Grain extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new Grain(Configuration.default()))
}
