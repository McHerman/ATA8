/*
 * Copyright (c) 2019-2021 Antmicro <www.antmicro.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package ATA8

import chisel3._
import chisel3.util._

class AXI4LiteCSR(val addrWidth:Int, val busWidth:Int) extends Module{
  val io = IO(new Bundle{
    //val ctl = Flipped(new AXI4Lite(addrWidth, busWidth))
    val ctl = Flipped(new CustomAXI4Lite(addrWidth, busWidth))
    val bus = new CSRBusBundle(addrWidth, busWidth)
  })

  val sIdle :: sReadAddr :: sReadData :: sWriteAddr :: sWriteData :: sWriteResp :: Nil = Enum(6)
  val state = RegInit(sIdle)

  val awready = RegInit(false.B)
  val wready = RegInit(false.B)
  val bvalid = RegInit(false.B)
  val bresp = WireInit(0.U(AXI4Lite.respWidth.W))

  val arready = RegInit(false.B)
  val rvalid = RegInit(false.B)
  val rresp = WireInit(0.U(AXI4Lite.respWidth.W))

  val addr = RegInit(0.U(addrWidth.W))

  val read = RegInit(false.B)
  val write = RegInit(false.B)
  val dataOut = RegInit(0.U(busWidth.W))

  val transaction_id = RegInit(0.U(AXI4Lite.idWidth.W))

  io.ctl.rdata := io.bus.dataIn
  io.ctl.rid := transaction_id
  io.bus.dataOut := dataOut

  io.ctl.awready := awready
  io.ctl.wready := wready
  io.ctl.bvalid := bvalid
  io.ctl.bresp := bresp
  io.ctl.bid := transaction_id

  io.ctl.arready := arready
  io.ctl.rvalid := rvalid
  io.ctl.rresp := rresp

  io.bus.read := read
  io.bus.write := write
  io.bus.addr := addr

  switch(state){
    is(sIdle){
      rvalid := false.B
      bvalid := false.B
      read := false.B
      write := false.B
      transaction_id := 0.U
      when(io.ctl.awvalid){
        state := sWriteAddr
        transaction_id := io.ctl.awid
      }.elsewhen(io.ctl.arvalid){
        state := sReadAddr
        transaction_id := io.ctl.arid
      }
    }
    is(sReadAddr){
      arready := true.B
      when(io.ctl.arvalid && arready){
        state := sReadData
        addr := io.ctl.araddr(addrWidth - 1, 2)
        read := true.B
        arready := false.B
      }
    }
    is(sReadData){
      rvalid := true.B
      when(io.ctl.rready && rvalid){
        state := sIdle
        rvalid := false.B
      }
    }
    is(sWriteAddr){
      awready := true.B
      when(io.ctl.awvalid && awready){
        addr := io.ctl.awaddr(addrWidth - 1, 2)
        state := sWriteData
        awready := false.B
      }
    }
    is(sWriteData){
      wready := true.B
      when(io.ctl.wvalid && wready){
        state := sWriteResp
        dataOut := io.ctl.wdata
        write := true.B
        wready := false.B
      }
    }
    is(sWriteResp){
      wready := false.B
      bvalid := true.B
      when(io.ctl.bready && bvalid){
        state := sIdle
        bvalid := false.B
      }
    }
  }
}
