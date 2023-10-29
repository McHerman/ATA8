package ATA8

import chisel3._
import chisel3.util._

class Scratchpad(writeports: Int)(implicit c: Configuration) extends Module {
  val io = IO(new Bundle {
  	//val Readport = Vec(c.bufferReadPorts, Flipped(new ReadportBuf(c.arithDataWidth,10)))
  	//val Writeport = Vec(c.bufferWritePorts, Flipped(new WriteportBuf(c.arithDataWidth,10)))
  	val Writeport = Vec(writeports, Flipped(Decoupled(new Writeport(Vec(c.grainDim,UInt(c.arithDataWidth.W)),16))))
    val Readport = Vec(c.bufferReadPorts, Flipped(new Readport(Vec(c.grainDim,UInt(c.arithDataWidth.W)),16)))
  })

  val ReadDelay = Reg(Vec(c.bufferReadPorts, UInt(1.W)))
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
  } 


}