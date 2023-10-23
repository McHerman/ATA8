package ATA8

import chisel3._
import chisel3.experimental._
import chisel3.util._



class AXI4DMA() extends Module {
  val io = FlatIO(new Bundle {
    val AXI4 = new AXI4(64,32,2).suggestName("")
    val Memport = Decoupled(new Memport(UInt(64.W),64))
    val Descrip = Flipped(Decoupled(new Point2PointDMA(32,32,1)))
  })

  val StateReg = RegInit(0.U(4.W))
  val DescripReg = Reg(new Point2PointDMA(32,32,1))

  val addrTemp = RegInit(0.U(8.W) )


  io.Descrip.ready := false.B


  io.Memport.valid := false.B
  io.Memport.bits := DontCare



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


  switch(StateReg){
    is(0.U){ // IDLE

      io.Descrip.ready := true.B

      when(io.Descrip.valid){
        when(io.Descrip.bits.write){
          StateReg := 1.U
        }.otherwise{
          StateReg := 3.U
        }

        DescripReg := io.Descrip.bits
      }

    }
    is(1.U){ // WRITE ADDRESS

      io.AXI4.AXI4WriteAddress.awvalid := true.B

      io.AXI4.AXI4WriteAddress.awaddr := DescripReg.AXI_addr
      io.AXI4.AXI4WriteAddress.awid := DescripReg.id
      io.AXI4.AXI4WriteAddress.awlen := DescripReg.len
      io.AXI4.AXI4WriteAddress.awsize := DescripReg.size
      io.AXI4.AXI4WriteAddress.awburst := 1.U //TODO add stuff here 
      //io.AXI4.AXI4WriteAddress.awlock := 0.U
      //io.AXI4.AXI4WriteAddress.awcache := 0.U
      //io.AXI4.AXI4WriteAddress.awprot := 0.U

      when(io.AXI4.AXI4WriteAddress.awready && io.Memport.ready){
        StateReg := 2.U

        io.Memport.valid := true.B
        io.Memport.bits.addr := DescripReg.BRAM_addr
        addrTemp := 0.U
      }
    }
    is(2.U){ // WRITE DATA

      io.Memport.valid := true.B
      io.Memport.bits.addr := DescripReg.BRAM_addr + addrTemp

      io.AXI4.AXI4WriteData.wvalid := true.B

      io.AXI4.AXI4WriteData.wdata := io.Memport.bits.readData
      io.AXI4.AXI4WriteData.wstrb := 0xff.U
      io.AXI4.AXI4WriteData.wid := 0.U

      when(io.AXI4.AXI4WriteData.wready){
        when(addrTemp === DescripReg.len){
          io.AXI4.AXI4WriteData.wlast := true.B
          StateReg := 0.U // Return to idle 
          addrTemp := 0.U
        }.otherwise{
          io.Memport.bits.addr := DescripReg.BRAM_addr + addrTemp + 1.U
          addrTemp := addrTemp + 1.U
        }
      }
    }
    is(3.U){ // READ ADDRESS 

      io.AXI4.AXI4ReadAddress.arvalid := true.B

      io.AXI4.AXI4ReadAddress.araddr := DescripReg.AXI_addr
      io.AXI4.AXI4ReadAddress.arid := DescripReg.id
      io.AXI4.AXI4ReadAddress.arlen := DescripReg.len
      io.AXI4.AXI4ReadAddress.arsize := DescripReg.size
      io.AXI4.AXI4ReadAddress.arburst := 1.U
      io.AXI4.AXI4ReadAddress.arlock := 0.U
      io.AXI4.AXI4ReadAddress.arcache := 0.U
      io.AXI4.AXI4ReadAddress.arprot := 0.U

      when(io.AXI4.AXI4ReadAddress.arready){
        StateReg := 4.U
      }
    }
    is(4.U){ // READ DATA

      when(io.Memport.ready){
        io.AXI4.AXI4ReadData.rready := true.B

        /*
        when (io.AXI4.AXI4ReadData.rvalid) {
          when (io.AXI4.AXI4ReadData.rlast) {
            StateReg := 0.U

            addrTemp := 0.U
            io.Memport.bits.addr := 0.U
            io.Memport.valid := true.B
          }.otherwise {
            addrTemp := addrTemp + 1.U // Increase address by 4 bytes for next beat

            io.Memport.bits.addr := addrTemp
            io.Memport.valid := true.B
            io.Memport.bits.wenable := true.B  // Request next beat
            io.Memport.bits.writeData := io.AXI4.AXI4ReadData.rdata
            io.Memport.bits.strb := 0xff.U
          }
        }
        */

        when (io.AXI4.AXI4ReadData.rvalid) {

          io.Memport.bits.addr := addrTemp
          io.Memport.valid := true.B
          io.Memport.bits.wenable := true.B  // Request next beat
          io.Memport.bits.writeData := io.AXI4.AXI4ReadData.rdata
          //io.Memport.bits.strb := 0xff.U

          when (io.AXI4.AXI4ReadData.rlast) {
            StateReg := 0.U
          }.otherwise {
            addrTemp := addrTemp + 1.U // Increase address by 4 bytes for next beat
          }
        }


      }
    }
  }
}

object AXI4DMA extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new AXI4DMA())
}
