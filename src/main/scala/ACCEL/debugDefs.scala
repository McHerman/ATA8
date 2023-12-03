package ATA8

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum

class LoadDebug extends Bundle {
  val state = Output(UInt(4.W))
}

class StoreDebug extends Bundle {
  val state = Output(UInt(4.W))
  val axiReady = Output(Bool())
  val readPortValid = Output(Bool())
}

class ExeDebug extends Bundle {
  val state = Output(UInt(4.W))
}