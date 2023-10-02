package ATA8

import chisel3._
import chisel3.util._

class BufferFIFO(implicit c: Configuration) extends Module {
  
  var pointerwidth = log2Ceil(c.grainFIFOSize - 1)

  val io = IO(new Bundle {
    val WriteData = Flipped(Valid(UInt(c.arithDataWidth.W)))
    val ReadData = Decoupled(UInt(c.arithDataWidth.W))
    val Full = Output(Bool())
  })

  val Head = RegInit(0.U(pointerwidth.W))
  val Tail = RegInit(0.U(pointerwidth.W))

  val HeadFlip = RegInit(0.U(1.W))
  val TailFlip = RegInit(0.U(1.W))

  val Readvalid = RegInit(0.U(1.W))
  
  //val Mem = DualPortRAM(c.grainFIFOSize,c.arithDataWidth)

  val Mem = Module(new DualPortRAM(c.grainFIFOSize,UInt(c.arithDataWidth.W)))

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
  }

  io.Full := (Head === Tail) && (HeadFlip.asUInt ^ TailFlip.asUInt).asBool 

}

/*
object BufferFIFO extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new BufferFIFO(1024,8))
}
*/
