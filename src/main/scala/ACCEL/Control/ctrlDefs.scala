package ATA8

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum

class InstructionPackage extends Bundle {
  val instruction = UInt(64.W)
}

trait HasAddrsField extends Bundle {
  val addrs: Vec[Bundle {
    val addr: UInt
    val depend: Depend
  }]
}

class Depend(implicit c: Configuration) extends Bundle {
  val ready = Bool()
  val tag = UInt(c.tagWidth.W)
}

abstract class InstBase(addrsSize: Int, addrdSize: Int)(implicit c: Configuration) extends Bundle{
  val op = UInt(1.W)
  val size = UInt(8.W)
  val addrs = Vec(addrsSize, new Bundle{val addr = UInt(16.W)})
  val addrd = Vec(addrdSize, new Bundle{val addr = UInt(16.W)})
}

class ExecuteInst(implicit c: Configuration) extends InstBase(2,1){
  val mode = UInt(1.W)
  //val grainSize = UInt(4.W)
}

class LoadInst(implicit c: Configuration) extends InstBase(0,1){
  val mode = UInt(1.W)
}

class StoreInst(implicit c: Configuration) extends InstBase(1,0){
  //val mode = UInt(1.W)
}


abstract class InstIssueBase(addrsSize: Int, addrdSize: Int)(implicit c: Configuration) extends Bundle with HasAddrsField{
  val op = UInt(1.W)
  val size = UInt(8.W)
  val addrs = Vec(addrsSize, new Bundle{val addr = UInt(16.W); val depend = new Depend})
  val addrd = Vec(addrdSize, new Bundle{val addr = UInt(16.W); val tag = UInt(c.tagWidth.W)})
}


class ExecuteInstIssue(implicit c: Configuration) extends InstIssueBase(2,1){
  val mode = UInt(1.W)
  val grainSize = UInt(4.W)
}

class LoadInstIssue(implicit c: Configuration) extends InstIssueBase(0,1){
  val mode = UInt(1.W)
}

class StoreInstIssue(implicit c: Configuration) extends InstIssueBase(1,0){
  val mode = UInt(1.W)
}

class Event(implicit c: Configuration) extends Bundle {
  //val eventType = EventType
  val tag = UInt(c.tagWidth.W)
} 

class TagWrite(implicit c: Configuration) extends Bundle {
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

class ROBWrite(implicit c: Configuration) extends Bundle {
  val addr = Decoupled(new Bundle{val addr = UInt(16.W); val ready = Bool()})
  val tag = Flipped(Valid(UInt(c.tagWidth.W))) 
} 

class ROBRead(implicit c: Configuration) extends Bundle {
  val request = Valid(new Bundle {
    val addr = UInt(16.W) //FIXME: Magic number
  })
  val response = Flipped(Valid(new Bundle {
    val tag = UInt(c.tagWidth.W)
    val ready = Bool()
  }))
} 

class SysOP(implicit c: Configuration) extends Bundle {
  val mode = UInt(1.W)
  val size = UInt(8.W)
  val tag = UInt(c.tagWidth.W)
  val sizes = Vec(c.grainDim, UInt(log2Ceil(c.dataBusSize + 1).W))
} 

class DMARead(implicit c: Configuration) extends Bundle {
  val request = Decoupled(new Bundle{
    val addr = UInt(16.W)
    val burstStride = UInt(8.W)
    val burstCnt = UInt(8.W)
    val burstSize = UInt(log2Ceil(c.dataBusSize + 1).W)
  })
  val response = Flipped(Decoupled(new Bundle{
    val completed = Bool() // true = passed
  }))
}

class DMAWrite(implicit c: Configuration) extends Bundle {
  val request = Decoupled(new Bundle{
    val addr = UInt(16.W)
    val burstStride = UInt(8.W)
    val burstCnt = UInt(8.W)
    val burstSize = UInt(log2Ceil(c.dataBusSize + 1).W)
    val tag = UInt(c.tagWidth.W)
  })
  val response = Flipped(Decoupled(new Bundle{
    val completed = Bool() // true = passed
    val tag = UInt(c.tagWidth.W)
  }))
}