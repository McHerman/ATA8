package ATA8

import chisel3._
import chisel3.util._

class ReadportSimple[T <: Data](private val dataType: T, val addrWidth: Int) extends Bundle {
  val addr = Output(UInt(addrWidth.W))
  val readData = Input(dataType.cloneType)
}

class Readport[T <: Data](private val dataType: T, val addrWidth: Int) extends Bundle {
  val request = Decoupled(new Bundle {
    val addr = UInt(addrWidth.W)
  })
  val response = Flipped(Valid(new Bundle {
    val readData = dataType.cloneType
  }))
}

class Writeport[T <: Data](private val dataType: T, val addrWidth: Int) extends Bundle {
  val addr  = Output(UInt(addrWidth.W))
  val data = Output(dataType.cloneType)
}

class ReadportScratch(implicit c: Configuration) extends Bundle {
  val request = Decoupled(new Bundle {
    val addr = UInt(16.W)
    val burstCnt = UInt(8.W)
    val burstStride = UInt(5.W) // Limited to max size of 128 bit (16 byte) //TODO: change burstsize convenntion so 0 equals 1 byte
    val burstSize = UInt(5.W) // Limited to max size of 128 bit (16 byte) //TODO: change burstsize convenntion so 0 equals 1 byte
  })
  val data = Flipped(Decoupled(new Bundle { //TODO: Change this to comply with the naming scheme of other readports
    val readData = Vec(c.dataBusSize,UInt(8.W))
    val last = Bool()
  }))
}

class WriteportScratch(implicit c: Configuration) extends Bundle { // TODO: Might be a good idea to introduce a "locking" feature where a continued transaction is insured even with intermitten interuptions 
  val request = Decoupled(new Bundle {
    val addr = UInt(16.W) // TODO find non arbitry value for this 
    val burstMode = Bool() // false: sized burst, true: streamed burst (pretty unsafe)
    val burstCnt = UInt(8.W)
    val burstStride = UInt(5.W)
    val burstSize = UInt(5.W)
  })
  val data = Decoupled(new Bundle {
    val writeData = Vec(c.dataBusSize,UInt(8.W))
    val strb = Vec(c.dataBusSize,Bool())
    val last = Bool()
  })
}





