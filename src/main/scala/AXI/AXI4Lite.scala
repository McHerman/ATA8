/*
 * Copyright (c) 2019-2021 Antmicro <www.antmicro.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package ATA8

import chisel3._

class AXI4LAW(val addrWidth: Int) extends Bundle {
  val awaddr = Output(UInt(addrWidth.W))
  val awprot = Output(UInt(AXI4Lite.protWidth.W))
  val awvalid = Output(Bool())
  val awready = Input(Bool())
  val awid = Output(UInt(AXI4Lite.idWidth.W))

  awaddr.suggestName("awaddr")
  awprot.suggestName("awprot")
  awvalid.suggestName("awvalid")
  awready.suggestName("awready")
  awid.suggestName("awid")
}

object AXI4LAW {
  def apply(addr: UInt, valid: UInt): AXI4LAW = {
    val aw = Wire(new AXI4LAW(addr.getWidth))
    aw.awprot := 0.U
    aw.awaddr := addr
    aw.awvalid := valid
    aw.awid := 0.U
    aw
  }
}

class AXI4LW(val dataWidth: Int) extends Bundle {
  val wdata = Output(UInt(dataWidth.W))
  val wstrb = Output(UInt((dataWidth / 8).W))
  val wvalid = Output(Bool())
  val wready = Input(Bool())
  val wid = Output(UInt(AXI4Lite.idWidth.W))

  wdata.suggestName("wdata")
  wstrb.suggestName("wstrb")
  wvalid.suggestName("wvalid")
  wready.suggestName("wready")
  wid.suggestName("wid")
}

object AXI4LW {
  def apply(data: UInt, strb: UInt, valid: UInt): AXI4LW = {
    val w = Wire(new AXI4LW(data.getWidth))
    w.wdata := data
    w.wstrb := strb
    w.wvalid := valid
    w.wid := 0.U
    w
  }
}

class AXI4LB extends Bundle {
  val bresp = Input(UInt(AXI4Lite.respWidth.W))
  val bvalid = Input(Bool())
  val bready = Output(Bool())
  val bid = Input(UInt(AXI4Lite.idWidth.W))

  bresp.suggestName("bresp")
  bvalid.suggestName("bvalid")
  bready.suggestName("bready")
  bid.suggestName("bid")
}

object AXI4LB {
  def apply(ready: UInt): AXI4LB = {
    val b = Wire(new AXI4LB())
    b.bready := ready
    b
  }
}

class AXI4LAR(val addrWidth: Int) extends Bundle {
  val araddr = Output(UInt(addrWidth.W))
  val arprot = Output(UInt(AXI4Lite.protWidth.W))
  val arvalid = Output(Bool())
  val arready = Input(Bool())
  val arid = Output(UInt(AXI4Lite.idWidth.W))

  araddr.suggestName("araddr")
  arprot.suggestName("arprot")
  arvalid.suggestName("arvalid")
  arready.suggestName("arready")
  arid.suggestName("arid")
}

object AXI4LAR {
  def apply(addr: UInt, valid: UInt): AXI4LAR = {
    val ar = Wire(new AXI4LAR(addr.getWidth))
    ar.arprot := 0.U
    ar.araddr := addr
    ar.arvalid := valid
    ar.arid := 0.U
    ar
  }

  def tieOff(addrWidth: Int): AXI4LAR = {
    val ar = Wire(new AXI4LAR(addrWidth))
    ar.arprot := 0.U
    ar.araddr := 0.U
    ar.arvalid := 0.U
    ar.arid := 0.U
    ar
  }
}

class AXI4LR(val dataWidth: Int) extends Bundle {
  val rdata = Input(UInt(dataWidth.W))
  val rresp = Input(UInt(AXI4Lite.respWidth.W))
  val rvalid = Input(Bool())
  val rready = Output(Bool())
  val rid = Input(UInt(AXI4Lite.idWidth.W))

  rdata.suggestName("rdata")
  rresp.suggestName("rresp")
  rvalid.suggestName("rvalid")
  rready.suggestName("rready")
  rid.suggestName("rid")
}

object AXI4LR {
  def apply(dataWidth: Int, ready: UInt): AXI4LR = {
    val r = Wire(new AXI4LR(dataWidth))
    r.rready := ready
    r
  }

  def tieOff(dataWidth: Int): AXI4LR = {
    val r = Wire(new AXI4LR(dataWidth))
    r.rready := 0.U
    r
  }
}

class AXI4Lite(val addrWidth: Int, val dataWidth: Int) extends Bundle {
  val aw = new AXI4LAW(addrWidth)
  val w = new AXI4LW(dataWidth)
  val b = new AXI4LB()
  val ar = new AXI4LAR(addrWidth)
  val r = new AXI4LR(dataWidth)
}

object AXI4Lite {
  val protWidth = 3
  val respWidth = 2
  val idWidth = 12
}


class CustomAXI4Lite(val addrWidth: Int, val dataWidth: Int) extends Bundle {
  val awaddr = Output(UInt(addrWidth.W))
  val awprot = Output(UInt(AXI4Lite.protWidth.W))
  val awvalid = Output(Bool())
  val awready = Input(Bool())
  val awid = Output(UInt(AXI4Lite.idWidth.W))

  val wdata = Output(UInt(dataWidth.W))
  val wstrb = Output(UInt((dataWidth / 8).W))
  val wvalid = Output(Bool())
  val wready = Input(Bool())
  val wid = Output(UInt(AXI4Lite.idWidth.W))

  val bresp = Input(UInt(AXI4Lite.respWidth.W))
  val bvalid = Input(Bool())
  val bready = Output(Bool())
  val bid = Input(UInt(AXI4Lite.idWidth.W))

  val araddr = Output(UInt(addrWidth.W))
  val arprot = Output(UInt(AXI4Lite.protWidth.W))
  val arvalid = Output(Bool())
  val arready = Input(Bool())
  val arid = Output(UInt(AXI4Lite.idWidth.W))

  val rdata = Input(UInt(dataWidth.W))
  val rresp = Input(UInt(AXI4Lite.respWidth.W))
  val rvalid = Input(Bool())
  val rready = Output(Bool())
  val rid = Input(UInt(AXI4Lite.idWidth.W))
}
