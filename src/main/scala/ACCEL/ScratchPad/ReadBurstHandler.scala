package ATA8

import chisel3._
import chisel3.util._

class ReadBurstHandler(implicit c: Configuration) extends Module {
  val io = IO(new Bundle {
    val scratchReadport = Flipped(new ReadportScratch)
    val readPort = new Readport(Vec(c.dataBusSize,UInt(c.arithDataWidth.W)),16)
  })

  // Set default values
  io.readPort.request.valid := false.B
  io.readPort.request.bits := DontCare
  io.readPort.response.bits:= DontCare
  io.scratchReadport.request.ready := false.B
  io.scratchReadport.data.valid := false.B
  io.scratchReadport.data.bits := DontCare
  io.scratchReadport.data.bits.last := false.B

  val reg = Reg(io.scratchReadport.request.bits.cloneType)

  val isLocked = RegInit(false.B)

  io.scratchReadport.request.ready := !isLocked

  when(io.scratchReadport.request.fire) {
    isLocked := true.B

    reg.addr := io.scratchReadport.request.bits.addr
    reg.burstSize := io.scratchReadport.request.bits.burstSize
    reg.burstStride := io.scratchReadport.request.bits.burstStride 
    reg.burstCnt := io.scratchReadport.request.bits.burstCnt - 1.U //TODO: not great, fix at invocation
  }

  when(isLocked) {
    when(io.readPort.request.ready) {
      io.readPort.request.valid := true.B
      io.readPort.request.bits.addr := reg.addr

      io.scratchReadport.data.bits.readData := io.readPort.response.bits.readData.asTypeOf(Vec(c.dataBusSize, UInt(8.W)))

      when(io.readPort.request.fire) {
        reg.addr := reg.addr + reg.burstStride
      } 

      when(io.readPort.response.valid){
        reg.burstCnt := reg.burstCnt - 1.U
        io.scratchReadport.data.valid := true.B

        when(reg.burstCnt === 0.U){
          io.scratchReadport.data.bits.last := true.B
        }
      }
    }

    when(reg.burstCnt === 0.U) {
      isLocked := false.B
    }
  }
}