package ATA8

import chisel3._
import chisel3.util._

class Memport[T <: Data](private val dataType: T, val addrWidth: Int) extends Bundle {
  val addr  = Output(UInt(addrWidth.W))
  //val enable  = Output(Bool())
  val wenable = Output(Bool())
  val readData = Input(dataType.cloneType)
  val writeData = Output(dataType.cloneType)
  //val strb = Output(UInt((dataWidth/8).W)) // Add strobe signal here
}

class Memport_V2(val dataWidth: Int,val addrWidth: Int) extends Bundle {
  val addr  = Output(UInt(addrWidth.W))
  val wenable = Output(Bool())
  val readData = Input(UInt(dataWidth.W))
  val writeData = Output(UInt(dataWidth.W))
  val strb = Output(UInt((dataWidth/8).W)) // Add strobe signal here
}

class Memport_V3(val dataWidth: Int,val addrWidth: Int) extends Bundle {
  val addr  = Output(UInt(addrWidth.W))
  val wenable = Output(Bool())
  val readData = Input(Vec(dataWidth/8,UInt(8.W)))
  //val readData = Input(UInt(128.W))
  val writeData = Output(Vec(dataWidth/8,UInt(8.W)))
  val strb = Output(UInt((dataWidth/8).W)) // Add strobe signal here
}

class Point2PointDMA(val AXI_addr_width: Int,val BRAM_addr_width: Int,val id_width: Int) extends Bundle {
  val AXI_addr    = UInt(AXI_addr_width.W)
  val BRAM_addr    = UInt(BRAM_addr_width.W)
  val size    = UInt(3.W)
  val len     = UInt(8.W)

  val write   = Bool()
  val id      = UInt(id_width.W)
}

class ST2PointDMA(val addr_width: Int, val id_width: Int) extends Bundle {
  val addr    = UInt(addr_width.W)
  val size    = UInt(3.W)
  val len     = UInt(16.W)

  val id      = UInt(id_width.W)
}

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

class Readport_V2(val dataWidth: Int,val addrWidth: Int) extends Bundle {
  val request = Decoupled(new Bundle {
    val addr = UInt(addrWidth.W)
  })
  val response = Flipped(Valid(new Bundle {
    val readData = Vec(dataWidth/8,UInt(8.W))
  }))
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





