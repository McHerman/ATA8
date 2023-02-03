import chisel3._
import chisel3.experimental._
import chisel3.util._

class CAP_IO extends Bundle{
  val In = Input(UInt(24.W))
  val Out = Output(UInt(24.W))
}

/*

class Element_In extends Bundle{
  val Carry_In = Input(UInt(8.W))
  val Edge_In = Input(UInt(8.W))
  val Edge_Out = Output(UInt(8.W))
  val Weight_In = Input(UInt(8.W))
  val Weight_Out = Output(UInt(8.W))
  val Weight_Stall_In = Input(Bool())
  val Weight_Stall_Out = Input(Bool())
}

*/

/*

class Element_Out extends Bundle{
  val Carry = Output(UInt(8.W))
  val Edge = Output(UInt(8.W))
  val Weight = Output(UInt(8.W))
  val Weight_Stall = Input(Bool())
}
*/

class Element_Right extends Bundle{
  val Edge = Output(UInt(8.W))
} 

class Element_Down extends Bundle{
  val Carry = Output(UInt(8.W))
  val Weight = Output(UInt(8.W))
  val Weight_Stall = Input(Bool())
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

  /*  

  val Program = (xml \\ "Core" \ "Program").text 

  var Lanes = (xml \\ "Core" \\ "Vector" \\ "VALU" \\ "@lanes").text.toInt
  var VectorRegisters = (xml \\ "Core" \\ "Vector" \\ "Registers" \\ "@number").text.toInt
  var VectorRegisterLength = (xml \\ "Core" \\ "Vector" \\ "Registers" \\ "@length").text.toInt

  var HasCache = (xml \\ "Core" \\ "Memory" \\ "Cache" \\ "@hasCache").text.toBoolean
  var CacheSize = (xml \\ "Core" \\ "Memory" \\ "Cache" \\ "Size" \\"@bit").text.toInt

  var Memsize = (xml \\ "Core" \\ "Memory" \\ "BRAM" \\ "@size").text.toInt
  var SPIRAM_Offset = (xml \\ "Core" \\ "Memory" \\ "DRAM" \\ "@offset").text.toInt 

  */

  val io = IO(new Bundle {
    //val Sub_IO = new CAP_IO
    val In = Input(UInt(24.W))
    val Out = Output(UInt(24.W))
  })

  /*
  val SPI_Out = IO(new Bundle{
    val SCLK = Output(Bool())
    val CE = Output(Bool())
    val SO = Input(Vec(4,Bool()))
    val SI = Output(Vec(4,Bool()))
    val Drive = Output(Bool())
  })
  */

  // IO

  io.Out := 0.U

  // Interconnections

  //SPI_Out <> SPI.SPI
  
}

// generate Verilog
object DSP extends App {

  //val Config = args(0)

  val Config = "config/APA24ex.xml"

  /*val xml = XML.loadFile(Config)*/
  (new chisel3.stage.ChiselStage).emitVerilog(new ATA8(/*, xml */))
}

