package ATA8

import chisel3._
import chisel3.util._

class ReadBurstHandler(implicit c: Configuration) extends Module {
  val io = IO(new Bundle {
    val scratchReadport = Flipped(new ReadportScratch)
    val readPort = new Readport(Vec(c.grainDim,UInt(c.arithDataWidth.W)),16)
  })

  // Set default values
  io.readPort.request.valid := false.B
  io.readPort.request.bits := DontCare
  io.readPort.response.bits:= DontCare
  io.scratchReadport.request.ready := false.B
  io.scratchReadport.data.valid := false.B
  io.scratchReadport.data.bits := DontCare
  io.scratchReadport.data.bits.last := false.B


  val burstCounter = RegInit(0.U(8.W))
  val burstSize = RegInit(0.U(8.W))
  val activeAddr = RegInit(0.U(16.W))
  val isLocked = RegInit(false.B)

  io.scratchReadport.request.ready := !isLocked

  when(io.scratchReadport.request.fire()) {
    isLocked := true.B
    activeAddr := io.scratchReadport.request.bits.addr
    burstSize := io.scratchReadport.request.bits.burst
    burstCounter := io.scratchReadport.request.bits.burst
  }

  when(isLocked) {
    when(io.readPort.request.ready && burstCounter =/= 0.U) {
      io.readPort.request.valid := true.B
      io.readPort.request.bits.addr := activeAddr
      //io.scratchReadport.data.valid := io.readPort.response.valid
      io.scratchReadport.data.bits.readData := io.readPort.response.bits.readData.asTypeOf(Vec(c.grainDim, UInt(c.arithDataWidth.W)))

      when(io.readPort.request.fire()) {
        activeAddr := activeAddr + burstSize
      } 

      when(io.readPort.response.valid){
        burstCounter := burstCounter - 1.U
        io.scratchReadport.data.valid := true.B

        when(burstCounter === 1.U){
          io.scratchReadport.data.bits.last := true.B
        }
      }
    }

    when(burstCounter === 0.U) {
      isLocked := false.B
    }
  }
}