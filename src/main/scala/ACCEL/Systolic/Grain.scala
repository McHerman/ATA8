package ATA8

import chisel3._
import chisel3.util._
  
class Grain(config: Configuration) extends Module {

  implicit val c = config
  
  val io = IO(new Bundle {
    //val State = Input(UInt(1.W))
    //val Size = Input(UInt(8.W))
    val in = Flipped(Decoupled(new SysOP)) 
    val Memport = Flipped(Decoupled(new Memport_V3(c.arithDataWidth*c.grainDim,10))) // Add actual memport
    val Readport = Flipped(new Readport_V2(c.arithDataWidth*c.grainDim,10))
    val Trigger = Input(Bool())
  })

  val XFile = Module(new XFile())
  val YFile = Module(new YFile())
  val ACCUFile = Module(new ACCUFile())
  val SysCtrl = Module(new SysCtrl())

  SysCtrl.io.in <> io.in

  XFile.io.Memport.valid := false.B
  XFile.io.Memport.bits := DontCare

  YFile.io.Memport.valid := false.B
  YFile.io.Memport.bits := DontCare

  /* ACCUFile.io.Memport.valid := false.B
  ACCUFile.io.Memport.bits := DontCare
 */

  ACCUFile.io.Readport.request.valid := false.B
  ACCUFile.io.Readport.request.bits := DontCare


  io.Memport.ready := true.B

  when(io.Memport.valid){ // Fix this shit
    when(io.Memport.bits.addr === 1.U){
      XFile.io.Memport <> io.Memport
    }.elsewhen(io.Memport.bits.addr === 2.U){
      YFile.io.Memport <> io.Memport
    }
  }

  ACCUFile.io.Readport <> io.Readport


  /*
  XFile.io.MemPort <> io.Memport
  YFile.io.MemPort <> io.Memport
  ACCUFile.io.MemPort <> io.Memport
  */

  //XFile.io.Activate := false.B
  //YFile.io.Activate := false.B
  
  XFile.io.Activate := SysCtrl.io.Activate
  YFile.io.Activate := SysCtrl.io.Activate
  YFile.io.Enable := SysCtrl.io.Enable
  
  ACCUFile.io.Activate := XFile.io.ActivateOut

  //YFile.io.State := io.State
  //ACCUFile.io.State := io.State 

  YFile.io.State := io.in.bits.mode
  ACCUFile.io.State := io.in.bits.mode

  //YFile.io.Shift := 0.U
  //ACCUFile.io.Shift := 0.U
  
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
