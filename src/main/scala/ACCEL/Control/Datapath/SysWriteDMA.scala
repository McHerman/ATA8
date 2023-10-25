package ATA8

import chisel3._
import chisel3.util._
  
class SysWriteDMA(implicit c: Configuration) extends Module {  
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new Bundle{val addr = UInt(16.W); val size = UInt(8.W); val tag = UInt(c.tagWidth.W)}))
    val scratchOut = new WriteportScratch
    val readPort = new Readport(Vec(c.grainDim,UInt(c.arithDataWidth.W)),10)
    //val completed = Output(Bool())
    val completed = Valid(UInt(c.tagWidth.W))
  })

  io.in.ready := false.B

  io.completed.valid := false.B
  io.completed.bits := DontCare

  io.scratchOut.request.valid := false.B
  io.scratchOut.request.bits := DontCare

  io.scratchOut.data.valid := false.B
  io.scratchOut.data.bits := DontCare
  io.scratchOut.data.bits.last := false.B

  io.readPort.request.valid := false.B
  io.readPort.request.bits := DontCare

  val StateReg = RegInit(0.U(4.W))
  val reg = Reg(new Bundle{val addr = UInt(16.W); val size = UInt(8.W); val tag = UInt(c.tagWidth.W)})

  val burstCNT = RegInit(0.U(8.W))

  switch(StateReg){
    is(0.U){
      io.in.ready := true.B
      
      when(io.in.valid){
        reg := io.in.bits
        StateReg := 1.U
      }
    }
    is(1.U){
      io.scratchOut.request.bits.addr := reg.addr
      io.scratchOut.request.bits.burst := reg.size
      
      when(io.scratchOut.request.ready){
        io.scratchOut.request.valid := true.B
        StateReg := 2.U
      } 
    }
    is(2.U){
      io.readPort.request.bits.addr := reg.addr + burstCNT

      when(io.scratchOut.data.ready){
        io.readPort.request.valid := true.B

        io.scratchOut.data.bits.writeData := io.readPort.response.bits.readData
        //io.scratchOut.data.bits.strb := "hff".U.asBools
        io.scratchOut.data.bits.strb.foreach(element => element := true.B)


        when(io.readPort.response.valid){
          io.scratchOut.data.valid := true.B

          when(burstCNT < (reg.size - 1.U)){ //FIXME: Might have to change size width
            burstCNT := burstCNT + 1.U
          }.otherwise{
            io.scratchOut.data.bits.last := true.B
            burstCNT := 0.U
            StateReg := 3.U
          }
        }
      }
    }
    is(3.U){
      io.completed.valid := true.B
      io.completed.bits := reg.tag

      StateReg := 0.U
    }
  }
}
