package ATA8

import chisel3._
import chisel3.util._

class Buffer(config: Configuration) extends Module {
  implicit val c = config
  
  val io = IO(new Bundle {
  	//val Readport = Vec(c.bufferReadPorts, Flipped(new ReadportBuf(c.arithDataWidth,10)))
  	//val Writeport = Vec(c.bufferWritePorts, Flipped(new WriteportBuf(c.arithDataWidth,10)))
  	val Writeport = Vec(c.bufferWritePorts, Flipped(new WriteportBuf()))
    val Readport = Vec(c.bufferReadPorts, Flipped(new ReadportBuf()))
  })

  //val ReadportCnt = Reg(Vec(c.bufferReadPorts,UInt(8.W)))
  val ReadDelay = Reg(Vec(c.bufferReadPorts, UInt(1.W)))
  //val WriteportCnt = Reg(Vec(c.bufferWritePorts,UInt(8.W)))

  //val mem = Vec(c.grainDim,SyncReadMem(c.bufferSize, UInt(c.arithDataWidth.W)))

  val mem = Seq.fill(c.grainDim)(SyncReadMem(c.bufferSize, UInt(c.arithDataWidth.W))) // Create a 2D array of PE modules



  

  // ReadPorts

  for(i <- 0 until c.bufferReadPorts){

    io.Readport(i).request.ready := true.B

    for(k <- 0 until c.grainDim){
      val rdPort = mem(k)(io.Readport(i).request.bits.addr)

      io.Readport(i).data.bits.readData(k) := rdPort
    }

    ReadDelay(i) := io.Readport(i).request.valid 
    io.Readport(i).data.valid := ReadDelay(i)    
  }



  // WritePorts

  for(i <- 0 until c.bufferWritePorts){

    io.Writeport(i).request.ready := true.B
    io.Writeport(i).data.ready := true.B


    for(k <- 0 until c.grainDim){
      val wrPort = mem(k)(io.Writeport(i).request.bits.addr)

      when(io.Writeport(i).data.valid && io.Writeport(i).request.valid && io.Writeport(i).data.bits.strb(k)){
        wrPort :=  io.Writeport(i).data.bits.writeData(k)
      }    
    }

  }




}

object Buffer extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new Buffer(Configuration.default()))
}
