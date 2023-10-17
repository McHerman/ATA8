package ATA8

import chisel3._
import chisel3.util._



//class ResStation(implicit c: Configuration) extends Module {
class ROB(tagCount: Int, readports: Int, val offset: Int)(implicit c: Configuration) extends Module {

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

  def full(head: UInt, tail: UInt, headFlip: Bool, tailFlip: Bool): (Bool) = {
    val full = WireDefault (false.B)
  
    when(!(headFlip === tailFlip) && head >= tail){ // build wrapping
      full := true.B
    }
  
    (full)
  }
  

  class mapping(implicit c: Configuration) extends Bundle {
    val addr = UInt(c.addrWidth.W)
    val ready = Bool()
    val valid = Bool()
  } 

  var pointerwidth = log2Ceil(c.grainFIFOSize - 1)

  val io = IO(new Bundle {
    val Writeport = Flipped(new TagWrite())
    // val Writeport = Vec(c.tagProducers,Flipped(Decoupled(new Bundle {val addr = Output(UInt(c.addrWidth.W)); val tag = Input(UInt(c.tagWidth.W))})))
    val ReadData = Vec(readports,Flipped(new TagRead())) // Two request from ExeDecoder, one from StoreController
    val tagDealloc = Flipped(Decoupled(UInt(c.tagWidth.W)))
    val event = Vec(3,Flipped(Valid(new Event())))
    val readAddr = Vec(2/* FIXME: magic fucking number*/,Flipped(new Readport(UInt(c.addrWidth.W), c.tagWidth)))
  })

  val Head = RegInit(0.U(c.tagWidth.W))
  val Tail = RegInit(0.U(c.tagWidth.W))
  val HeadFlip = RegInit(0.U(1.W))
  val TailFlip = RegInit(0.U(1.W))

  val full = Wire(Bool())
  full := (Head === Tail) && !(HeadFlip === TailFlip)
  val empty = Wire(Bool())
  empty := (Head === Tail) && (HeadFlip === TailFlip)

  io.ReadData.foreach{case (element) => element.response.valid := false.B; element.response.bits := DontCare}
  io.readAddr.foreach{case (element) => element.request.ready := false.B; element.response.valid := false.B; element.response.bits := DontCare}

  io.tagDealloc.ready := !empty

	io.Writeport.tag.valid := false.B
	io.Writeport.tag.bits := DontCare

  val Map = Reg(Vec(tagCount,new mapping()))

  io.ReadData.foreach { case (element) => 
    when(element.request.valid){
      val (tag,valid,ready) = vecSearch(Map,element.request.bits.addr) 
      element.response.bits.tag := tag + offset.U// TODO: Add offset

			io.event.foreach{case (event) => 
				when(event.valid && event.bits.tag === tag){
					element.response.bits.ready := true.B
				}.otherwise{
					element.response.bits.ready := ready
				}
			}

      element.response.valid := valid
    }
  }

  io.readAddr.foreach {case (element) => // For use in SysCtrl, checks for stat
    element.request.ready := true.B
    when(element.request.valid){
      element.response.bits.readData := Map(element.request.bits.addr).addr - offset.U// TODO: subtract offset
      element.response.valid := Map(element.request.bits.addr).valid - offset.U // TODO: subtract offset
    }
  }

  //io.Writeport.addr.ready := !full

	when(io.Writeport.addr.valid && !full){
    Map(Head).addr := io.Writeport.addr.bits.addr
    Map(Head).ready := false.B
    Map(Head).valid := true.B

    io.Writeport.tag.valid := true.B
    io.Writeport.tag.bits := Head + offset.U // TODO: Add offset
    
    when(Head === (tagCount.U - 1.U)){
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

  io.event.foreach{case (element) => 
    when(element.valid){
      when(Map(element.bits.tag).valid){ 
        Map(element.bits.tag).ready := true.B
      } 
    }
  }
}
