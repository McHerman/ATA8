package ATA8

import chisel3._
import chisel3.util._
  
class SysDMA(implicit c: Configuration) extends Module {  
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new Bundle{val addr = UInt(16.W); val size = UInt(8.W)}))
    val scratchIn = new ReadportScratch
    val memport = Decoupled(new Memport(Vec(c.grainDim,UInt(c.arithDataWidth.W)),10)) //FIXME: replace with updated memport
    val completed = Output(Bool())
    val completeAgnoledge = Input(Bool())
  })

  io.in.ready := false.B

  io.completed := false.B

  io.scratchIn.request.valid := false.B
  io.scratchIn.request.bits := DontCare

  io.scratchIn.data.ready := false.B

  io.memport.valid := false.B
  io.memport.bits := DontCare

  val StateReg = RegInit(0.U(4.W))
  val reg = Reg(new Bundle{val addr = UInt(16.W); val size = UInt(8.W)})

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
      io.scratchIn.request.bits.addr := reg.addr
      //io.scratchIn.request.bits.burst := reg.size
      io.scratchIn.request.bits.burstSize := reg.size
      io.scratchIn.request.bits.burstCnt := reg.size
      
      when(io.scratchIn.request.ready){
        io.scratchIn.request.valid := true.B
        StateReg := 2.U
      } 
    }
    is(2.U){
      when(io.memport.ready){
        io.scratchIn.data.ready := true.B
        
        when(io.scratchIn.data.valid){
          io.memport.bits.addr := reg.addr + burstCNT
          io.memport.bits.writeData := io.scratchIn.data.bits.readData
          io.memport.bits.wenable := true.B

          io.memport.valid := true.B

          when(burstCNT < (reg.size - 1.U)){
            burstCNT := burstCNT + 1.U 
          }.otherwise{
            burstCNT := 0.U 
            StateReg := 3.U
          }
        }
      }
    }
    is(3.U){ // FIXME: Change this
      io.completed := true.B

      when(io.completeAgnoledge){
        StateReg := 0.U
      }
    }
  }
}
