package ATA8

import chisel3._
import chisel3.util._

class ResStation(implicit c: Configuration) extends Module {
//class ResStation(config: Configuration) extends Module { // Todo, change this to the bufferFIFO primitive
  //implicit val c = config
  
  var pointerwidth = log2Ceil(c.grainFIFOSize - 1)

  val io = IO(new Bundle {
    /* val WriteData = Flipped(Decoupled(new IssuePackage()))
    val ReadData = Decoupled(new IssuePackage()) */

    val WriteData = Flipped(Decoupled(new ExecuteInst())) //TODO: Change this back
    val ReadData = Decoupled(new ExecuteInst())
  })

  //io.WriteData.ready := false.B

  val Head = RegInit(0.U(pointerwidth.W))
  val Tail = RegInit(0.U(pointerwidth.W))

  val HeadFlip = RegInit(0.U(1.W))
  val TailFlip = RegInit(0.U(1.W))

  val Readvalid = RegInit(0.U(1.W))
  
  //val Mem = Module(new DualPortRAM(c.grainFIFOSize, new IssuePackage()))
  val Mem = Module(new DualPortRAM(c.grainFIFOSize, new ExecuteInst()))

	
  //io.ReadData.bits := Mem.io.Read.bits.data 
  io.ReadData.bits := Mem.io.Read.data 
  io.ReadData.valid := false.B

  Mem.io.Write.valid := false.B
  Mem.io.Write.bits := DontCare

  //Mem.io.Read.valid := false.B
  //Mem.io.Read.bits := DontCare

  Mem.io.Read := DontCare

  Readvalid := io.ReadData.ready
  io.ReadData.valid := Readvalid


  when(io.WriteData.valid){
    Mem.io.Write.valid := true.B
    Mem.io.Write.bits.addr := Head
    Mem.io.Write.bits.data := io.WriteData.bits
    
    Head := Head + 1.U

    when(Head === (c.grainFIFOSize.U - 1.U)){
      Head := 0.U
      HeadFlip := ~HeadFlip
    }.otherwise{
      Head := Head + 1.U
    }
  }


  when(io.ReadData.ready){
    //Mem.io.Read.valid := true.B
    //Mem.io.Write.bits.addr := Tail
    Mem.io.Read.addr := Tail

    when(Tail === (c.grainFIFOSize.U - 1.U)){
      Tail := 0.U
      TailFlip := ~TailFlip
    }.otherwise{
      Tail := Tail + 1.U
    }

    io.ReadData.valid := true.B
  }

  //io.Full := (Head === Tail) && (HeadFlip.asUInt ^ TailFlip.asUInt).asBool 

  val Full = Wire(Bool())

  Full := (Head === Tail) && !(HeadFlip === TailFlip)

  io.WriteData.ready := !Full


  /*
  io.Fill := 0.U

  when(!(HeadFlip.asUInt ^ TailFlip.asUInt).asBool){ // When they are the same value
    io.Fill := Head - Tail
  }.otherwise{
    io.Fill := c.grainFIFOSize.U - (Tail - Head)
  }
  */



}


/* object ResStation extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new ResStation(Configuration.default()))
} */
