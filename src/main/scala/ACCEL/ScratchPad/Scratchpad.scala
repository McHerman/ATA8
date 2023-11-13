package ATA8

import chisel3._
import chisel3.util._

class Scratchpad(writeports: Int)(implicit c: Configuration) extends Module {
  val io = IO(new Bundle {
  	//val Readport = Vec(c.bufferReadPorts, Flipped(new ReadportBuf(c.arithDataWidth,10)))
  	//val Writeport = Vec(c.bufferWritePorts, Flipped(new WriteportBuf(c.arithDataWidth,10)))
  	//val Writeport = Vec(writeports, Flipped(Decoupled(new Writeport(Vec(c.grainDim,UInt(c.arithDataWidth.W)),16))))
    //val Readport = Vec(c.bufferReadPorts, Flipped(new Readport(Vec(c.grainDim,UInt(c.arithDataWidth.W)),16)))
    val Writeport = Vec(writeports, Flipped(Decoupled(new Writeport(new Bundle{val writeData = Vec(c.dataBusSize,UInt(8.W)); val strb = Vec(c.dataBusSize, Bool())},16))))
    val Readport = Vec(c.bufferReadPorts, Flipped(new Readport(Vec(c.dataBusSize,UInt(8.W)),16)))
  })

  /* val ReadDelay = Reg(Vec(c.bufferReadPorts, UInt(1.W)))
  val mem = SyncReadMem(c.scratchpadSize, Vec(c.grainDim,UInt(c.arithDataWidth.W)))

  // ReadPorts

  io.Readport.zipWithIndex.foreach{ case(element,i) =>
    element.request.ready := true.B

    val rdPort = mem(io.Readport(i).request.bits.addr)
    element.response.bits.readData := rdPort

    ReadDelay(i) := element.request.valid 
    element.response.valid := ReadDelay(i)  
  }

  // WritePorts
  
  io.Writeport.zipWithIndex.foreach{ case(element,i) =>
    //element.request.ready := true.B
    //element.data.ready := true.B

    element.ready := true.B

    //val wrPort = mem(element.request.bits.addr)
    val wrPort = mem(element.bits.addr)

    when(element.valid){
      wrPort := element.bits.data
    }    
  }  */

  val mem = SyncReadMem(c.scratchpadSize, UInt(8.W))

  // ReadPorts
  io.Readport.foreach{case (port) =>
    port.request.ready := true.B

    val rdAddr = port.request.bits.addr

    //port.response.bits.readData := (0 until c.dataBusSize).map(i => mem.read(rdAddr + i.U))

    port.response.bits.readData.zipWithIndex.foreach{case (readData,i) =>
      val rdPort = mem(rdAddr + i.U)
      readData := rdPort
    }

    port.response.valid := RegNext(port.request.fire, init = false.B) // Register the valid signal to delay by one cycle
  }

  // WritePorts
  io.Writeport.foreach{case (port) =>
    port.ready := true.B

    /* when(port.fire) {
      val wrAddr = port.bits.addr
      val wrData = port.bits.data.data
      val mask = port.bits.data.strb.asBools

      wrData.zipWithIndex.foreach{case (data,i) => 
        when(mask(i)) {
          mem.write(wrAddr + i.U, data)
        }
      } 
    } */

    val wrAddr = port.bits.addr
    val wrData = port.bits.data.writeData
    val mask = port.bits.data.strb

    wrData.zipWithIndex.foreach{case (data,i) => 

      val wrPort = mem(wrAddr + i.U)

      when(mask(i) && port.fire) {
        wrPort := data
      }
    } 
  }


}