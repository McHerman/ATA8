package ATA8

import chisel3._
import chisel3.util._

class PEY(val dataWidth: Int, val statewidth: Int) extends Bundle {
  val Y = Output(UInt(dataWidth.W))
  val PEState = new PEState(statewidth) 
}

class PEState(val statewidth: Int) extends Bundle {  
  val Shift = Output(Bool()) // IN WS, this will trigger a weight shiftin, IN OS this will trigger an output shiftout  
  val EN = Output(Bool()) // TRIGGERS THE PE TO SHIFT IN DATA FOR VARIOUS OPERATIONS
  val State = Output(UInt(statewidth.W)) // LOW WS, HIGH OS 
}

class PEX(val dataWidth: Int) extends Bundle {
  val X = Output(UInt(dataWidth.W))
}
