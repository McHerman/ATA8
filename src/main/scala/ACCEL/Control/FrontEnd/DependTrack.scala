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


  when(io.WriteData.valid){
    dependVec(Head) := VecInit(io.WriteData.bits.addrs.map(_.depend))
  }

  val readyVec = VecInit(dependVec(Tail).map(_.ready))
  io.ReadData.request.ready := readyVec.reduceTree((a: Bool, b: Bool) => a && b) && !empty
} 
