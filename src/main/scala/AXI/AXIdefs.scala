package ATA8

import chisel3._
import chisel3.util._

// Bundle for Write Address channel

/*
class AXI4WriteAddress(val addrWidth: Int, val idWidth: Int) extends Bundle {
  val awaddr   = UInt(addrWidth.W)
  val awid     = UInt(idWidth.W)
  val awlen    = UInt(8.W)
  val awsize   = UInt(3.W)
  val awburst  = UInt(2.W)
  val awlock   = Bool()
  val awcache  = UInt(4.W)
  val awprot   = UInt(3.W)
}

// Bundle for Write Data channel
class AXI4WriteData(val dataWidth: Int) extends Bundle {
  val wdata   = UInt(dataWidth.W)
  val wstrb   = UInt((dataWidth / 8).W)
  val wlast   = Bool()
  //val wvalid  = Bool()
  //val wready  = Bool()
}

// Bundle for Write Response channel
class AXI4WriteResp(val idWidth: Int) extends Bundle {
  val bid     = UInt(idWidth.W)
  val bresp   = UInt(2.W)
  //val bvalid  = Bool()
  //val bready  = Bool()
}

// Bundle for Read Address channel
class AXI4ReadAddress(val addrWidth: Int, val idWidth: Int) extends Bundle {
  val araddr  = UInt(addrWidth.W)
  val arid    = UInt(idWidth.W)
  val arlen   = UInt(8.W)
  val arsize  = UInt(3.W)
  val arburst = UInt(2.W)
  val arlock  = Bool()
  val arcache = UInt(4.W)
  val arprot  = UInt(3.W)
}

// Bundle for Read Data channel
class AXI4ReadData(val dataWidth: Int, val idWidth: Int) extends Bundle {
  val rdata   = UInt(dataWidth.W)
  val rresp   = UInt(2.W)
  val rlast   = Bool()
  val rid     = UInt(idWidth.W)
}

*/


// Extend the Bundle to add default initialization 


class AXI4WriteAddress(val addrWidth: Int, val idWidth: Int) extends Bundle {
  val awaddr   = Output(UInt(addrWidth.W))
  val awid     = Output(UInt(idWidth.W))
  val awlen    = Output(UInt(8.W))
  val awsize   = Output(UInt(3.W))
  val awburst  = Output(UInt(2.W))
  val awlock   = Output(Bool())
  val awcache  = Output(UInt(4.W))
  val awprot   = Output(UInt(3.W))

  val awvalid  = Output(Bool())
  val awready  = Input(Bool())

  /*
  def initializeToZero() = {
    awaddr   := 0.U
    awid     := 0.U
    awlen    := 0.U
    awsize   := 0.U
    awburst  := 0.U 
    awlock   := 0.U
    awcache  := 0.U
    awprot   := 0.U

    awvalid  := false.B
    awready  := false.B
  }
  */
}

// Bundle for Write Data channel
class AXI4WriteData(val dataWidth: Int, val idWidth: Int) extends Bundle {
  val wid     = Output(UInt(idWidth.W))
  val wdata   = Output(UInt(dataWidth.W))
  val wstrb   = Output(UInt((dataWidth / 8).W))
  val wlast   = Output(Bool())
  
  val wvalid  = Output(Bool())
  val wready  = Input(Bool())

  /*
  def initializeToZero() = {
    wdata    := 0.U
    wstrb    := 0.U
    wlast    := 0.U
    
    wvalid  := false.B
    wready  := false.B
  }
  */
}

// Bundle for Write Response channel
class AXI4WriteResp(val idWidth: Int) extends Bundle {
  val bid     = Output(UInt(idWidth.W))
  val bresp   = Output(UInt(2.W))
  
  val bvalid  = Output(Bool())
  val bready  = Input(Bool())

  /*
  def initializeToZero() = {
    bid      := 0.U
    bresp    := 0.U
    
    bvalid  := false.B
    bready  := false.B
  }
  */
}

// Bundle for Read Address channel
class AXI4ReadAddress(val addrWidth: Int, val idWidth: Int) extends Bundle {
  val araddr  = Output(UInt(addrWidth.W))
  val arid    = Output(UInt(idWidth.W))
  val arlen   = Output(UInt(8.W))
  val arsize  = Output(UInt(3.W))
  val arburst = Output(UInt(2.W))
  val arlock  = Output(Bool())
  val arcache = Output(UInt(4.W))
  val arprot  = Output(UInt(3.W))

  val arvalid  = Output(Bool())
  val arready  = Input(Bool())

  /*
  def initializeToZero() = {
    araddr     := 0.U
    arid       := 0.U
    arlen      := 0.U
    arsize     := 0.U
    arburst    := 0.U
    arlock     := false.B
    arcache    := 0.U
    arprot     := 0.U
    
    arvalid  := false.B
    arready  := false.B
  }
  */
}

// Bundle for Read Data channel
class AXI4ReadData(val dataWidth: Int, val idWidth: Int) extends Bundle {
  val rdata   = Output(UInt(dataWidth.W))
  val rresp   = Output(UInt(2.W))
  val rlast   = Output(Bool())
  val rid     = Output(UInt(idWidth.W))

  val rvalid  = Output(Bool())
  val rready  = Input(Bool())

  /*
  def initializeToZero() = {
    rdata       := 0.U 
    rresp       := 0.U
    rlast       := 0.U
    rid         := 0.U

    rvalid      := false.B
    rready      := false.B
  }
  */
}


class AXI4(val dataWidth: Int, val addrWidth: Int, val idWidth: Int) extends Bundle {
  val AXI4WriteAddress = new AXI4WriteAddress(addrWidth,idWidth).suggestName("")
  val AXI4WriteData = new AXI4WriteData(dataWidth,idWidth).suggestName("")
  val AXI4WriteResp = Flipped(new AXI4WriteResp(idWidth)).suggestName("")
  val AXI4ReadAddress = new AXI4ReadAddress(addrWidth,idWidth).suggestName("")
  val AXI4ReadData = Flipped(new AXI4ReadData(dataWidth,idWidth)).suggestName("")
}

class AXIST(val dataWidth: Int, val idWidth: Int, val destWidth: Int, val userWidth: Int, val respWidth: Int) extends Bundle {
  val tdata   = UInt(dataWidth.W).suggestName("tdata")
  val tstrb     = UInt((dataWidth/8).W).suggestName("tstrb")
  val tkeep    = UInt((dataWidth/8).W).suggestName("tkeep")
  val tlast   = Bool().suggestName("tlast")
  val tid  = UInt(idWidth.W).suggestName("tid")
  val tdest   = UInt(destWidth.W).suggestName("tdest")
  val tuser  = UInt(userWidth.W).suggestName("tuser")
  val tresp = UInt(respWidth.W).suggestName("tresp")
}


class AXIST_2(val dataWidth: Int, val idWidth: Int, val destWidth: Int, val userWidth: Int, val respWidth: Int) extends Bundle {
  val tdata   = Output(UInt(dataWidth.W).suggestName("tdata"))
  val tstrb     = Output(UInt((dataWidth/8).W).suggestName("tstrb"))
  val tkeep    = Output(UInt((dataWidth/8).W).suggestName("tkeep"))
  val tlast   = Output(Bool().suggestName("tlast"))
  val tid  = Output(UInt(idWidth.W).suggestName("tid"))
  val tdest   = Output(UInt(destWidth.W).suggestName("tdest"))
  val tuser  = Output(UInt(userWidth.W).suggestName("tuser"))
  val tresp = Output(UInt(respWidth.W).suggestName("tresp"))

  val tvalid = Output(Bool())
  val tready = Input(Bool())
}

