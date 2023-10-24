package ATA8

import chisel3._
import chisel3.util._

class DependTrack[T <: HasAddrsField](size: Int, dataType: T, val dependsize: Int)(implicit c: Configuration) extends BufferFIFO(size, dataType){
  val dependio = IO(new Bundle {
    val event = Vec(2,Flipped(Valid(new Event)))
  }) 

  // Existing FIFO logic inherited from BufferFIFO

  val dependVec = Reg(Vec(size, Vec(dependsize,new Depend)))

  dependVec.foreach{case (element) => 

    element.foreach{case (depend) => 
      dependio.event.foreach{case (event) => 
        when(event.valid && event.bits.tag === depend.tag){
          depend.ready := true.B
        }
		  }
    }
  }

  /* when(dependio.depend.valid){
    dependVec(Head) := dependio.depend.bits
  } */

  when(io.WriteData.valid){
    dependVec(Head) := VecInit(io.WriteData.bits.addrs.map(_.depend))
  }

  //dependio.dependReady := VecInit(dependVec(Tail).map(_.ready)).reduceTree((a: Bool, b: Bool) => a && b)

  val readyVec = VecInit(dependVec(Tail).map(_.ready))

  io.ReadData.request.ready := readyVec.reduceTree((a: Bool, b: Bool) => a && b) && !empty
} 

/* package ATA8

import chisel3._
import chisel3.util._

//class DependTrack[T <: HasAddrsField](val size: Int, val dataType: T, val dependsize: Int)(implicit c: Configuration) extends Module {
class DependTrack(val size: Int)(implicit c: Configuration) extends Module {


  val pointerwidth = log2Ceil(size)
  
  val io = IO(new Bundle {
    //val WriteData = Flipped(Decoupled(dataType))  // updated line
    //val ReadData = Flipped(new Readport(dataType,0))  // updated line
    val WriteData = Flipped(Decoupled(new StoreInstIssue))  // updated line
    val ReadData = Flipped(new Readport(new StoreInstIssue,0))  // updated line

    val event = Vec(2,Flipped(Valid(new Event)))
  })

  val Head = RegInit(0.U(pointerwidth.W))
  val Tail = RegInit(0.U(pointerwidth.W))

  val HeadFlip = RegInit(0.U(1.W))
  val TailFlip = RegInit(0.U(1.W))

  val Full = Wire(Bool())
  Full := (Head === Tail) && !(HeadFlip === TailFlip)
  val empty = Wire(Bool())
  empty := (Head === Tail) && (HeadFlip === TailFlip)

  //val Mem = Module(new DualPortRAM(size, dataType))  // updated line
  val Mem = Module(new DualPortRAM(size, new StoreInstIssue))  // updated line

  io.ReadData.response.bits.readData := Mem.io.Read.data 
  io.ReadData.response.valid := false.B

  Mem.io.Write.valid := false.B
  Mem.io.Write.bits := DontCare

  Mem.io.Read := DontCare

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

  io.ReadData.request.ready := !empty

  Mem.io.Read.addr := Tail
  
  when(io.ReadData.request.valid){

    when(Tail === (size.U - 1.U)){
      Tail := 0.U
      Mem.io.Read.addr := 0.U
      TailFlip := ~TailFlip
    }.otherwise{
      Tail := Tail + 1.U
      Mem.io.Read.addr := Tail + 1.U // FIXME: Might be a little dangerous in terms of combinational delays 
    }

    io.ReadData.response.valid := true.B
  }

  io.WriteData.ready := !Full

  val dependVec = Reg(Vec(size, Vec(1,new Depend)))

  dependVec.foreach{case (element) => 

    element.foreach{case (depend) => 
      io.event.foreach{case (event) => 
        when(event.valid && event.bits.tag === depend.tag){
          depend.ready := true.B
        }
		  }
    }
  }

  /* when(dependio.depend.valid){
    dependVec(Head) := dependio.depend.bits
  } */

  when(io.WriteData.valid){
    dependVec(Head) := VecInit(io.WriteData.bits.addrs.map(_.depend))
  }

  //dependio.dependReady := VecInit(dependVec(Tail).map(_.ready)).reduceTree((a: Bool, b: Bool) => a && b)

  val readyVec = VecInit(dependVec(Tail).map(_.ready))

  //io.ReadData.request.ready := readyVec.reduceTree((a: Bool, b: Bool) => a && b) && !empty

}

//TODO: change port names to camelcase
 */