package ATA8

import chisel3._
import chisel3.util._


class RamSingle(val dataWidth: Int ,val addrWidth: Int, val memSize: Int) extends Module {
  val io = IO(new Bundle {
    val port = Flipped(Decoupled(new Memport(dataWidth,addrWidth)))
  })

  io.port.ready := true.B

  io.port.bits.readData := DontCare

  //val mem = SyncReadMem(memSize, Vec(dataWidth/8, UInt(8.W)))

  val mem = SyncReadMem(memSize, UInt(dataWidth.W))

  when (io.port.valid) {
    val rdwrPort = mem(io.port.bits.araddr)
    
    when (io.port.bits.wenable){ 
      //rdwrPort := io.port.writeData.asTypeOf(Vec(dataWidth/8, UInt(8.W)))
      rdwrPort := io.port.bits.writeData    
    }.otherwise{
      //io.port.readData := rdwrPort.asUInt
      io.port.bits.readData := rdwrPort
    }
  }
}
