package ATA8

import chisel3._
import chisel3.util._

class ScratchWriteArbiter(numPorts: Int)(implicit c: Configuration) extends Module {
  val io = IO(new Bundle {
    val inPorts = Vec(numPorts, Flipped(new WriteportScratch))
    val outPort = new WriteportScratch
  })

  io.inPorts.foreach{element => element.request.ready := false.B; element.data.ready := false.B}
  io.outPort.request.valid := false.B
  io.outPort.request.bits := DontCare
  io.outPort.data.valid := false.B
  io.outPort.data.bits := DontCare
  io.outPort.data.bits.last := false.B

  val activePort = RegInit(0.U(log2Ceil(numPorts).W))
  val isLocked = RegInit(false.B)
  val roundRobin = RegInit(0.U(log2Ceil(numPorts).W))

  when(!isLocked) {
    roundRobin := roundRobin + 1.U
  }

  io.inPorts.zipWithIndex.foreach { case (port, index) =>
    when((index.U === roundRobin) && !isLocked){
      port <> io.outPort
    }
  }

  when(io.outPort.request.fire && !isLocked) {
    isLocked := true.B
    activePort := roundRobin
  } 

  when(isLocked) {
    io.outPort <> io.inPorts(activePort)
  }

  when(isLocked && io.outPort.data.fire && io.inPorts(activePort).data.bits.last) {
    isLocked := false.B
  }
}
