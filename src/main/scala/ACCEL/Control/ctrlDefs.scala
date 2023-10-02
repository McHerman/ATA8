package ATA8

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum

class ExecuteInst(implicit c: Configuration) extends Bundle {
  val Op = UInt(1.W) // TODO: magic number
  val mode = UInt(c.modeWidth.W)
  val grainSize = UInt(c.sysWidth.W)
  val ids = Vec(2, new Bundle { val ready = Bool(); val id = UInt(c.tagWidth.W)})
  val idd = UInt(c.tagWidth.W) // Dest Address
  val size = UInt(c.grainSizeWidth.W)
} 

class IssuePackage(implicit c: Configuration) extends Bundle {
  val mode = UInt(c.modeWidth.W)
  val elementOffset = UInt(c.sysWidth.W)
  val ids = Vec(2, UInt(c.tagWidth.W))
  val idd = UInt(c.tagWidth.W) // Dest Address
  val syncId = UInt(c.syncIdWidth.W)          // Tag used to syncronize between different grains
} 

object EventType extends ChiselEnum {
  val CompletionWithValue, Completion, Branch, Jump, Exception = Value
}

class Event(implicit c: Configuration) extends Bundle {
  //val eventType = EventType
  val id = UInt(c.tagWidth.W)
} 