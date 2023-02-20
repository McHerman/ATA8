import chisel3._
import chisel3.experimental._
import chisel3.util._


class Element_Right extends Bundle{
  val Edge = Output(UInt(8.W))
} 

class Element_Down extends Bundle{
  val Carry = Output(UInt(8.W))
  val Weight = Output(UInt(8.W))
  val State = Input(Bool())
  val Stall = Input(Bool())
} 


/*

class MemPort(VectorRegisterLength: Int) extends Bundle{
  val Address = Output(UInt(24.W))
  val WriteData = Output(Vec(VectorRegisterLength,UInt(24.W)))
  val Enable = Output(Bool())
  val Len = Output(UInt(6.W))
  val WriteEn = Output(Bool())

  val ReadData = Input(Vec(VectorRegisterLength,UInt(24.W)))
  val Completed = Input(Bool())
  val ReadValid = Input(Bool())
  val Ready = Input(Bool())
}

*/

class ATA8(/*, xml: scala.xml.Elem */) extends Module {
  val io = IO(new Bundle {
    //val Sub_IO = new CAP_IO
    val In = Input(UInt(24.W))
    val Out = Output(UInt(24.W))
  })

  // IO

  io.Out := 0.U
  
}

// generate Verilog
object DSP extends App {

  //val Config = args(0)

  val Config = "config/APA24ex.xml"

  /*val xml = XML.loadFile(Config)*/
  (new chisel3.stage.ChiselStage).emitVerilog(new ATA8(/*, xml */))
}

