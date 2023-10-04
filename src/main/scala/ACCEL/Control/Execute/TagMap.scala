package ATA8

import chisel3._
import chisel3.util._



//class ResStation(implicit c: Configuration) extends Module {
class TagMap(implicit c: Configuration) extends Module {

  def vecSearch(reg: Vec[mapping], search: UInt): (UInt, Bool, Bool) = {
    val tag = WireDefault (0.U)
    val valid = WireDefault (false.B)
    val ready = WireDefault (false.B)

    reg.zipWithIndex.foreach{case (element,i) => 
      when(element.addr === search){
        tag := i.U
        ready := element.ready
        valid := element.valid
      }
    }
  
    (tag,valid,ready)
  }
  

  /* def isFull(head: UInt, tail: UInt, HeadFlip: Bool, TailFlip: Bool)(implicit c: Configuration): (Bool) = {
    val full = WireDefault (false.B)

    when()
  
    (full)
  } */

  class mapping(implicit c: Configuration) extends Bundle {
    val addr = UInt(c.addrWidth.W)
    val ready = Bool()
    val valid = Bool()
  } 


  
  
  var pointerwidth = log2Ceil(c.grainFIFOSize - 1)

  val io = IO(new Bundle {
    val Writeport = Decoupled(new Bundle {val tag = Output(UInt(c.tagWidth.W)); val addr = Input(UInt(c.addrWidth.W))})
    // val Writeport = Vec(c.tagProducers,Flipped(Decoupled(new Bundle {val addr = Output(UInt(c.addrWidth.W)); val tag = Input(UInt(c.tagWidth.W))})))
    val ReadData = Vec(c.tagRecievers,Decoupled(new Bundle {val tag = Output(UInt(c.tagWidth.W)); val ready = Output(Bool()); val addr = Input(UInt(c.addrWidth.W))}))
    val tagDealloc = Flipped(Decoupled(UInt(c.tagWidth.W)))
    val tagValid = Output(Vec(c.dmaCount,Bool())) 
  })

  val Head = RegInit(UInt(c.tagWidth.W))
  val Tail = RegInit(UInt(c.tagWidth.W))

  val HeadFlip = RegInit(UInt(1.W))
  val TailFlip = RegInit(UInt(1.W))

  //val Map = Reg(Vec(c.tagCount,UInt(c.addrWidth.W)))

  val Map = Reg(Vec(c.tagCount,new mapping()))


  io.ReadData.foreach { case (element) => 
    when(element.ready){
      val (tag,valid,ready) = vecSearch(Map,element.bits.addr) 
      element.bits.tag := tag
      element.bits.ready := ready
      element.valid := valid
    }
  }


  /*

  val tempIdx = Wire(Vec(c.tagProducers+1,UInt(4.W)))
  tempIdx(0) := Head

  io.Writeport.zipWithIndex.foreach { case (port, i) =>
    when(port.valid && !full){
      port.ready := true.B
      port.bits.tag := tempIdx(i)
      Map(tempIdx(i)) := port.bits.addr
      tempIdx(i+1) := tempIdx(i) + 1.U
    }.otherwise {
      tempIdx(i+1) := tempIdx(i)
    }
  }

  */

  when(io.Writeport.ready && !full){
    Map(Head).addr := io.Writeport.bits.addr
    Map(Head).ready := false.B
    Map(Head).valid := true.B

    io.Writeport.valid := true.B
    io.Writeport.bits.tag := Head
    
    when(Head === (c.tagCount.U - 1.U)){
      Head := 0.U
      HeadFlip := ~HeadFlip
    }.otherwise{
      Head := Head + 1.U
    }
  }


  when(io.tagDealloc.valid && Tail === io.tagDealloc.bits){
    Map(Tail).valid := false.B

    when(Tail === (c.grainFIFOSize.U - 1.U)){
      Tail := 0.U
      TailFlip := ~TailFlip
    }.otherwise{
      Tail := Tail + 1.U
    }
  }

  val full = Wire(Bool())
  full := (Head === Tail) && !(HeadFlip === TailFlip)

}
