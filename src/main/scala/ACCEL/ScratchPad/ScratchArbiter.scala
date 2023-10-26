package ATA8

import chisel3._
import chisel3.util._

class ScratchArbiter(numPorts: Int)(implicit c: Configuration) extends Module {
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

  when(io.outPort.request.ready && !isLocked) {
    isLocked := io.inPorts(roundRobin).request.valid
    io.outPort <> io.inPorts(roundRobin)

    when(isLocked) {
      activePort := roundRobin
    }
  }

  io.inPorts.zipWithIndex.foreach { case (port, index) =>
    port.request.ready := (index.U === roundRobin) && !isLocked
  }

  when(io.outPort.data.fire() && io.outPort.data.bits.last) {
    isLocked := false.B
  }

  when(isLocked) {
    io.outPort <> io.inPorts(activePort)
  }


  /* // Wire up requests
  io.outPort.request.valid := io.inPorts(activePort).request.valid && isLocked
  io.outPort.request.bits := io.inPorts(activePort).request.bits
  
 
  // Wire up data
  io.outPort.data.valid := io.inPorts(activePort).data.valid && isLocked
  io.outPort.data.bits := io.inPorts(activePort).data.bits
  io.inPorts.foreach { port =>
    port.data.ready := (activePort === roundRobin) && !isLocked
  }
 */




}
