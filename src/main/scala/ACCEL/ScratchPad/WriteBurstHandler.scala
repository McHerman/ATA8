package ATA8

import chisel3._
import chisel3.util._

class WriteBurstHandler(implicit c: Configuration) extends Module {
  val io = IO(new Bundle {
    val scratchWriteport = Flipped(new WriteportScratch)
    //val writePort = Decoupled(new Writeport(Vec(c.grainDim,UInt(c.arithDataWidth.W)),16))
    val writePort = Decoupled(new Writeport(new Bundle{val writeData = Vec(c.dataBusSize,UInt(8.W)); val strb = Vec(c.dataBusSize, Bool())},16))
  })

  // Set default values
  io.writePort.valid := false.B
  io.writePort.bits := DontCare
  io.scratchWriteport.request.ready := false.B
  io.scratchWriteport.data.ready := false.B

  /* val burstCounter = RegInit(0.U(8.W)) //TODO: cleanup
  val burstSize = RegInit(0.U(8.W))
  val activeAddr = RegInit(0.U(16.W)) */
  val isLocked = RegInit(false.B)
  //val burstMode = RegInit(false.B)

  val reg = Reg(io.scratchWriteport.request.bits.cloneType)

  io.scratchWriteport.request.ready := !isLocked

  when(io.scratchWriteport.request.fire()) {
    isLocked := true.B

    reg.addr := io.scratchWriteport.request.bits.addr
    reg.burstSize := io.scratchWriteport.request.bits.burstSize
    reg.burstStride := io.scratchWriteport.request.bits.burstStride
    reg.burstCnt := io.scratchWriteport.request.bits.burstCnt - 1.U
    reg.burstMode := io.scratchWriteport.request.bits.burstMode
  }

  when(isLocked) {
    when(io.writePort.ready) {
      io.scratchWriteport.data.ready := true.B
      io.writePort.valid := io.scratchWriteport.data.valid
      io.writePort.bits.addr := reg.addr
      io.writePort.bits.data.writeData := io.scratchWriteport.data.bits.writeData
      io.writePort.bits.data.strb := io.scratchWriteport.data.bits.strb

      when(io.writePort.fire) {
        reg.addr := reg.addr + reg.burstStride

        when(!reg.burstMode){
          reg.burstCnt := reg.burstCnt - 1.U
        }
      }
    }

    // in streaming mode, the burst when exit out then the master asserts last

    when(!reg.burstMode){
      when(reg.burstCnt === 0.U && io.scratchWriteport.data.bits.last) { //FIXME: Not brilliant, fix
        isLocked := false.B
      }
    }.otherwise{
      when(io.scratchWriteport.data.bits.last) {
        isLocked := false.B
      }
    }
  }
}