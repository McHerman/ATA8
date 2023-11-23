package ATA8

import chisel3._
import chisel3.util._

class ScratchReadArbiter(numPorts: Int)(implicit c: Configuration) extends Module {
  val io = IO(new Bundle {
    val inPorts = Vec(numPorts, Flipped(new ReadportScratch))
    val outPort = new ReadportScratch
  })

  // Initialize all ports
  io.inPorts.foreach { port =>
    port.request.ready := false.B
    port.data.valid := false.B
    port.data.bits := DontCare
    port.data.bits.last := false.B
  }
  
  io.outPort.request.valid := false.B
  io.outPort.request.bits := DontCare
  io.outPort.data.ready := false.B

  val activePort = RegInit(0.U(log2Ceil(numPorts).W))
  val isLocked = RegInit(false.B)
  val roundRobin = RegInit(0.U(log2Ceil(numPorts).W))

  // Round-robin increment
  when(!isLocked) {
    //roundRobin := Mux(roundRobin === (numPorts.U - 1.U), 0.U, roundRobin + 1.U)
    roundRobin := roundRobin + 1.U
  }

  // Arbitration logic
  io.inPorts.zipWithIndex.foreach { case (port, index) =>
    when((index.U === roundRobin) && !isLocked) {
      //port.request <> io.outPort.request
      port <> io.outPort
    }
  }

  // Locking mechanism
  /* when(io.outPort.request.fire() && !isLocked) {
    when(io.inPorts(roundRobin).request.valid) {
      isLocked := true.B
      activePort := roundRobin
    }
  } */

  when(io.outPort.request.fire && !isLocked) {
    isLocked := true.B
    activePort := roundRobin
  }

  // Data transfer
  when(isLocked) {
    //io.inPorts(activePort).data <> io.outPort.data
    io.inPorts(activePort) <> io.outPort
  }

  // Unlocking mechanism
  when(isLocked && io.outPort.data.fire() && io.outPort.data.bits.last) {
    isLocked := false.B
  }
}
