package ATA8

import chisel3._
import chisel3.util._

class DualPortRAM[T <: Data](val size: Int, val dataType: T) extends Module {
  
  var pointerwidth = log2Ceil(size - 1)
  
  val io = IO(new Bundle {
    val Write = Flipped(Valid(new Writeport(dataType,pointerwidth)))  
    //val Read = Flipped(Valid(new Readport(dataWidth,pointerwidth)))
    val Read = Flipped(new Readport(dataType,pointerwidth))
  })
  
  val mem = SyncReadMem(size, dataType)

  val wrPort = mem(io.Write.bits.addr)
  val rdPort = mem(io.Read.addr)

  when(io.Write.valid){
    wrPort := io.Write.bits.data
  }


  io.Read.data := rdPort

  /* when(io.Read.valid){
    io.Read.data := rdPort
  } */
}
