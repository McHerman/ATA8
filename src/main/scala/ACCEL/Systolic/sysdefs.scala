package ATA8

import chisel3._
import chisel3.util._

class PEY(val dataWidth: Int, val statewidth: Int) extends Bundle {
  val Y = Output(UInt(dataWidth.W))
}

class PEX(val dataWidth: Int) extends Bundle {
  val X = Output(UInt(dataWidth.W))
}
