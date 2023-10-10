package ATA8

import chisel3._
import chisel3.util._

class BufferFIFO[T <: Data](val size: Int, val dataType: T) extends Module {
  
  val pointerwidth = log2Ceil(size)
  
  val io = IO(new Bundle {
    val WriteData = Flipped(Decoupled(dataType))  // updated line
    val ReadData = Decoupled(dataType)  // updated line
  })

  val Head = RegInit(0.U(pointerwidth.W))
  val Tail = RegInit(0.U(pointerwidth.W))

  val HeadFlip = RegInit(0.U(1.W))
  val TailFlip = RegInit(0.U(1.W))

  val Readvalid = RegInit(0.U(1.W))
  
  val Mem = Module(new DualPortRAM(size, dataType))  // updated line

  io.ReadData.bits := Mem.io.Read.data 
  io.ReadData.valid := false.B

  Mem.io.Write.valid := false.B
  Mem.io.Write.bits := DontCare

  Mem.io.Read := DontCare

  Readvalid := io.ReadData.ready
  io.ReadData.valid := Readvalid


  when(io.WriteData.valid){
    Mem.io.Write.valid := true.B
    Mem.io.Write.bits.addr := Head
    Mem.io.Write.bits.data := io.WriteData.bits  // updated line
    
    Head := Head + 1.U

    when(Head === (size.U - 1.U)){
      Head := 0.U
      HeadFlip := ~HeadFlip
    }.otherwise{
      Head := Head + 1.U
    }
  }


  when(io.ReadData.ready){
    Mem.io.Read.addr := Tail

    when(Tail === (size.U - 1.U)){
      Tail := 0.U
      TailFlip := ~TailFlip
    }.otherwise{
      Tail := Tail + 1.U
    }

    io.ReadData.valid := true.B
  }

  val Full = Wire(Bool())
  Full := (Head === Tail) && !(HeadFlip === TailFlip)

  io.WriteData.ready := !Full
}

