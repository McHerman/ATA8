package ATA8

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum

class LoadDebug extends Bundle {
  val state = UInt(4.W)
}

class StoreDebug extends Bundle {
  val state = UInt(4.W)
}

class ExeDebug extends Bundle {
  val state = UInt(4.W)
}