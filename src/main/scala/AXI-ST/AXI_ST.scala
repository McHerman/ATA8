package ATA8

import chisel3._
import chisel3.experimental._
import chisel3.util._

class AXI_ST() extends Module {
  val io = FlatIO(new Bundle {
    val AXIST = Flipped(new AXIST_2(64,2,1,1,1))
    val AXIST_out = new AXIST_2(64,2,1,1,1)
    val LED_A1 = Output(Bool())
    val LED_A2 = Output(Bool())
    val LED_A3 = Output(Bool())
    val LED_A4 = Output(Bool())
    
    val AXI4 = new AXI4(64,32,2).suggestName("")
  })

  val burstAddrReg = RegInit(0.U(32.W))
  val addrTemp = RegInit(0.U(32.W))
  val idReg = RegInit(0.U(2.W))

  val RAM = Module(new RamSingle(64,32,1024))
  //val Memport_dummy = Wire(new Memport(64,32))

  RAM.io.port.bits.addr := 0.U
  RAM.io.port.bits.wenable := false.B
  RAM.io.port.bits.writeData := 0.U
  //RAM.io.port.bits.strb := 0.U

  /*
  Memport_dummy.addr := 0.U
  Memport_dummy.enable := false.B
  Memport_dummy.wenable := false.B
  Memport_dummy.writeData := 0.U
  Memport_dummy.strb := 0.U

  RAM.io.port2 <> Memport_dummy
  */

  val RAM2 = Module(new RamSingle(64,32,1024))  
  //val Memport_dummy2 = Wire(new Memport(64,32))

  RAM2.io.port.bits.addr := 0.U
  RAM2.io.port.bits.wenable := false.B
  RAM2.io.port.bits.writeData := 0.U
  //RAM2.io.port.bits.strb := 0.U

  /*
  Memport_dummy2.addr := 0.U
  Memport_dummy2.enable := false.B
  Memport_dummy2.wenable := false.B
  Memport_dummy2.writeData := 0.U
  Memport_dummy2.strb := 0.U

  RAM2.io.port2 <> Memport_dummy2
  */

  io.AXIST.tready := false.B

  io.AXIST_out := DontCare
  io.AXIST_out.tvalid := false.B

  io.AXI4.AXI4WriteAddress := DontCare
  io.AXI4.AXI4WriteData := DontCare
  io.AXI4.AXI4WriteResp := DontCare
  io.AXI4.AXI4ReadAddress := DontCare
  io.AXI4.AXI4ReadData := DontCare

  io.AXI4.AXI4WriteAddress.awvalid := false.B
  io.AXI4.AXI4WriteData.wvalid := false.B
  io.AXI4.AXI4WriteResp.bready := false.B
  io.AXI4.AXI4ReadAddress.arvalid := false.B
  io.AXI4.AXI4ReadData.rready := false.B

  io.AXI4.AXI4WriteData.wlast := 0.U
  io.AXIST_out.tlast := 0.U
  


  val StateReg = RegInit(0.U(4.W))

  val AW_accepted = RegInit(0.U(1.W))

  switch(StateReg){
    is(0.U){ // ACCEPT DATA STREAM

      io.AXIST.tready := true.B

      when (io.AXIST.tvalid) {
        when (io.AXIST.tlast) {
          StateReg := 1.U
          idReg:= io.AXIST.tid

          //RAM.io.port.enable := true.B
          RAM.io.port.valid := true.B
          RAM.io.port.bits.addr := 0.U
        }.otherwise {
          burstAddrReg := burstAddrReg + 1.U // Increase address by 4 bytes for next beat

          RAM.io.port.bits.addr := burstAddrReg
          //RAM.io.port.enable := true.B
          RAM.io.port.valid := true.B
          RAM.io.port.bits.wenable := true.B  // Request next beat
          RAM.io.port.bits.writeData := io.AXIST.tdata
          //RAM.io.port.bits.strb := io.AXIST.tstrb
        }
      }
    }
    is(1.U){ // WRITE ADDRESS

      io.AXI4.AXI4WriteAddress.awvalid := true.B

      io.AXI4.AXI4WriteAddress.awaddr := 0.U
      io.AXI4.AXI4WriteAddress.awid := 0.U
      io.AXI4.AXI4WriteAddress.awlen := burstAddrReg
      io.AXI4.AXI4WriteAddress.awsize := 3.U
      io.AXI4.AXI4WriteAddress.awburst := 1.U
      //io.AXI4.AXI4WriteAddress.awlock := 0.U
      //io.AXI4.AXI4WriteAddress.awcache := 0.U
      //io.AXI4.AXI4WriteAddress.awprot := 0.U

      when(io.AXI4.AXI4WriteAddress.awready){
        StateReg := 2.U

        //RAM.io.port.enable := true.B
        RAM.io.port.valid := true.B
        RAM.io.port.bits.addr := 0.U
        addrTemp := 0.U
      }
    }
    is(2.U){ // WRITE DATA

      //RAM.io.port.enable := true.B
      RAM.io.port.valid := true.B
      RAM.io.port.bits.addr := addrTemp

      io.AXI4.AXI4WriteData.wvalid := true.B

      io.AXI4.AXI4WriteData.wdata := RAM.io.port.bits.readData
      io.AXI4.AXI4WriteData.wstrb := 0xff.U
      io.AXI4.AXI4WriteData.wid := 0.U

      when(io.AXI4.AXI4WriteData.wready){
        when(addrTemp === burstAddrReg){
          io.AXI4.AXI4WriteData.wlast := true.B
          StateReg := 3.U
          addrTemp := 0.U
        }.otherwise{
          RAM.io.port.bits.addr := addrTemp + 1.U
          addrTemp := addrTemp + 1.U
        }
      }
    }
    /*

    is(1.U){ // WRITE ADDRESS

      io.AXI4.AXI4WriteAddress.awvalid := !AW_accepted

      io.AXI4.AXI4WriteAddress.awaddr := 0.U
      io.AXI4.AXI4WriteAddress.awid := 0.U
      io.AXI4.AXI4WriteAddress.awlen := burstAddrReg
      io.AXI4.AXI4WriteAddress.awsize := 3.U
      io.AXI4.AXI4WriteAddress.awburst := 1.U
      //io.AXI4.AXI4WriteAddress.awlock := 0.U
      //io.AXI4.AXI4WriteAddress.awcache := 0.U
      //io.AXI4.AXI4WriteAddress.awprot := 0.U

      when(io.AXI4.AXI4WriteAddress.awready){
        AW_accepted := 1.U
      }

      // Write data

      RAM.io.port.enable := true.B
      RAM.io.port.addr := addrTemp

      io.AXI4.AXI4WriteData.wvalid := true.B

      io.AXI4.AXI4WriteData.wdata := RAM.io.port.readData
      io.AXI4.AXI4WriteData.wstrb := 0xff.U
      io.AXI4.AXI4WriteData.wid := 0.U

      when(io.AXI4.AXI4WriteData.wready){
        when(addrTemp === burstAddrReg){
          io.AXI4.AXI4WriteData.wlast := true.B
          StateReg := 3.U
          addrTemp := 0.U
          AW_accepted := 0.U
        }.otherwise{
          RAM.io.port.araddr := addrTemp + 1.U
          addrTemp := addrTemp + 1.U
        }
      }
    }

    */
    is(3.U){
      io.AXI4.AXI4WriteResp.bready := true.B

      when(io.AXI4.AXI4WriteResp.bvalid){
        StateReg := 4.U
      }
    }
    is(4.U){ // READ ADDRESS 

      io.AXI4.AXI4ReadAddress.arvalid := true.B

      io.AXI4.AXI4ReadAddress.araddr := 0.U
      io.AXI4.AXI4ReadAddress.arid := 0.U
      io.AXI4.AXI4ReadAddress.arlen := burstAddrReg
      io.AXI4.AXI4ReadAddress.arsize := 3.U
      io.AXI4.AXI4ReadAddress.arburst := 1.U
      io.AXI4.AXI4ReadAddress.arlock := 0.U
      io.AXI4.AXI4ReadAddress.arcache := 0.U
      io.AXI4.AXI4ReadAddress.arprot := 0.U

      when(io.AXI4.AXI4ReadAddress.arready){
        StateReg := 5.U
      }
    }
    is(5.U){ // READ DATA
      io.AXI4.AXI4ReadData.rready := true.B

      when (io.AXI4.AXI4ReadData.rvalid) {
        when (io.AXI4.AXI4ReadData.rlast) {
          StateReg := 6.U
          idReg:= io.AXI4.AXI4ReadData.rid
          
          addrTemp := 0.U
          RAM2.io.port.bits.addr := 0.U
          //RAM2.io.port.enable := true.B
          RAM2.io.port.valid := true.B
        }.otherwise {
          addrTemp := addrTemp + 1.U // Increase address by 4 bytes for next beat

          RAM2.io.port.bits.addr := addrTemp
          //RAM2.io.port.enable := true.B
          RAM2.io.port.valid := true.B
          RAM2.io.port.bits.wenable := true.B  // Request next beat
          RAM2.io.port.bits.writeData := io.AXI4.AXI4ReadData.rdata
          //RAM2.io.port.bits.strb := 0xff.U
        }
      }
    }
    is(6.U){ // WRITE DATA 
      RAM2.io.port.bits.addr := addrTemp
      //RAM2.io.port.enable := true.B
      RAM2.io.port.valid := true.B

      io.AXIST_out.tdata := RAM2.io.port.bits.readData
      io.AXIST_out.tid := idReg
      io.AXIST_out.tstrb := 0xff.U
      io.AXIST_out.tkeep := 0xff.U
      
      io.AXIST_out.tvalid := true.B

      when(io.AXIST_out.tready){
        when(addrTemp === burstAddrReg){
          io.AXIST_out.tlast := true.B
          StateReg := 0.U
          addrTemp := 0.U
          burstAddrReg := 0.U

          idReg := 0.U
        }.otherwise{
          RAM2.io.port.bits.addr := addrTemp + 1.U
          addrTemp := addrTemp + 1.U
        }
      }
    }
  }

  io.LED_A1 := ~StateReg(0)
  io.LED_A2 := ~StateReg(1)
  io.LED_A3 := ~StateReg(2)
  io.LED_A4 := ~StateReg(3)

}

object AXI_ST extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new AXI_ST())
}
