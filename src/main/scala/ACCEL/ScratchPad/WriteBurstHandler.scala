package ATA8

import chisel3._
import chisel3.util._

class WriteBurstHandler(implicit c: Configuration) extends Module {
  val io = IO(new Bundle {
    val scratchWriteport = Flipped(new WriteportScratch)
    val writePort = Decoupled(new Writeport(Vec(c.grainDim,UInt(c.arithDataWidth.W)),16))
  })

  // Set default values
  io.writePort.valid := false.B
  io.writePort.bits := DontCare
  io.scratchWriteport.request.ready := false.B
  io.scratchWriteport.data.ready := false.B

  val burstCounter = RegInit(0.U(8.W))
  val activeAddr = RegInit(0.U(16.W))
  val isLocked = RegInit(false.B)

  io.scratchWriteport.request.ready := !isLocked

  when(io.scratchWriteport.request.fire()) {
    isLocked := true.B
    activeAddr := io.scratchWriteport.request.bits.addr
    burstCounter := io.scratchWriteport.request.bits.burst
    //io.scratchWriteport.request.ready := true.B
  }

  when(isLocked) {
    when(io.writePort.ready && burstCounter =/= 0.U) {
      io.scratchWriteport.data.ready := io.writePort.ready
      io.writePort.valid := io.scratchWriteport.data.valid
      io.writePort.bits.addr := activeAddr
      io.writePort.bits.data := io.scratchWriteport.data.bits.writeData
      io.scratchWriteport.data.ready := true.B

      when(io.writePort.fire()) {
        activeAddr := activeAddr + 1.U
        burstCounter := burstCounter - 1.U
      }
    }

    when(burstCounter === 0.U) {
      isLocked := false.B
    }
  }
}