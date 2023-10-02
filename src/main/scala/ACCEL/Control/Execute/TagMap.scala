package ATA8

import chisel3._
import chisel3.util._



//class ResStation(implicit c: Configuration) extends Module {
class TagMap(implicit c: Configuration) extends Module {
  def vecSearch(reg: Vec[UInt], size: Int, search: UInt): (UInt, Bool) = {
    val tag = WireDefault (0.U)
    val valid = WireDefault (false.B)


    for(i <- 0 until size){
      when(reg(i) === search){
        tag := i.U
        valid := true.B
      }
    }
  
    (tag,valid)
  }
  
  var pointerwidth = log2Ceil(c.grainFIFOSize - 1)

  val io = IO(new Bundle {
    val Writeport = Flipped(Valid(new Writeport(UInt(c.addrWidth.W),c.tagWidth)))
    val ReadData = Vec(c.dmaCount,Flipped(new Readport(UInt(c.addrWidth.W),c.tagWidth)))
    val tagValid = Output(Vec(c.dmaCount,Bool()))
  })



  val Map = Reg(Vec(c.tagCount,UInt(c.addrWidth.W)))

  for(i <- 0 until c.dmaCount){

    val (tag,valid) = vecSearch(Map,c.tagCount,io.ReadData(i).addr)
    io.ReadData(i).data := tag
    io.tagValid(i) := valid
  }

  when(io.Writeport.valid){
    Map(io.Writeport.bits.addr) := io.Writeport.bits.data
  }

}
