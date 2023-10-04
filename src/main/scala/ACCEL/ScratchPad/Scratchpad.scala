package ATA8

import chisel3._
import chisel3.util._

class ScratchPad(config: Configuration) extends Module {
  implicit val c = config
  
  val io = IO(new Bundle {
  	//val Readport = Vec(c.bufferReadPorts, Flipped(new ReadportBuf(c.arithDataWidth,10)))
  	//val Writeport = Vec(c.bufferWritePorts, Flipped(new WriteportBuf(c.arithDataWidth,10)))
  	val Writeport = Vec(c.bufferWritePorts, Flipped(new WriteportScratch()))
    val Readport = Vec(c.bufferReadPorts, Flipped(new ReadportScratch()))
  })

  val ReadDelay = Reg(Vec(c.bufferReadPorts, UInt(1.W)))
  val mem = SyncReadMem(c.bufferSize, Vec(c.grainDim,UInt(c.arithDataWidth.W)))

  // ReadPorts

  io.Readport.zipWithIndex.foreach{ case(element,i) =>
    element.request.ready := true.B

    val rdPort = mem(io.Readport(i).request.bits.addr)
    element.data.bits.readData := rdPort

    ReadDelay(i) := element.request.valid 
    element.data.valid := ReadDelay(i)  
  }

  // WritePorts

  io.Writeport.zipWithIndex.foreach{ case(element,i) =>
    element.request.ready := true.B
    element.data.ready := true.B

    val wrPort = mem(element.request.bits.addr)

    when(element.data.valid && element.request.valid){
      wrPort := element.data.bits.writeData
    }    
  }
}