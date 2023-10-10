package ATA8

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum

class InstructionPackage extends Bundle {
  val instruction = UInt(64.W)
}


class penis(implicit c: Configuration) extends Bundle {
  val op = UInt(2.W) // TODO: magic number
  val op2 = UInt(1.W)
  val mode = UInt(1.W)
  //val grainSize = UInt(c.sysWidth.W)
  val fill = UInt(4.W)

  val size = UInt(8.W)
  val addrs1 = UInt(16.W)
  val addrs2 = UInt(16.W)
  val addrd = UInt(16.W) // Dest Address
  //val size = UInt(c.grainSizeWidth.W
} 

class ExecuteInst(implicit c: Configuration) extends Bundle {
  val op = UInt(1.W) // TODO: magic number
  val mode = UInt(1.W)
  //val grainSize = UInt(c.sysWidth.W)
  val grainSize = UInt(4.W)
  val ids = Vec(2, new Bundle {val ready = Bool(); val tag = UInt(c.tagWidth.W)})
  val idd = UInt(c.tagWidth.W) // Dest Address
  //val size = UInt(c.grainSizeWidth.W)
  val size = UInt(8.W)
} 

class LoadInst(implicit c: Configuration) extends Bundle {
  val op = UInt(1.W) // TODO: magic number
  val mode = UInt(1.W)
  val fill = UInt(22.W)

  val size = UInt(8.W)
  val addr = UInt(32.W)
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
  val tag = UInt(c.tagWidth.W)
} 



class TagWrite(implicit c: Configuration) extends Bundle {
  //val tag = Output(UInt(c.tagWidth.W))
  //val addr = Input(UInt(c.addrWidth.W))

  //val addr = Decoupled(UInt(16.W)) //FIXME: Magic number
  val addr = Decoupled(new Bundle{val addr = UInt(16.W); val ready = Bool()})
  val tag = Flipped(Valid(UInt(c.tagWidth.W))) 
} 

class TagRead(implicit c: Configuration) extends Bundle {
  val request = Valid(new Bundle {
    val addr = UInt(16.W) //FIXME: Magic number
  })
  val response = Flipped(Valid(new Bundle {
    val tag = UInt(c.tagWidth.W)
    val ready = Bool()
  }))
} 