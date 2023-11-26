package ATA8

import chisel3._
import chisel3.util._
  
class SysDMA(implicit c: Configuration) extends Module {  
  val io = IO(new Bundle {
    val in = Flipped(new DMARead)
    val scratchIn = new ReadportScratch
    val writePort = Decoupled(Vec(c.dataBusSize,UInt(8.W))) 
  })

  io.in.request.ready := false.B

  io.scratchIn.request.valid := false.B
  io.scratchIn.request.bits := DontCare

  io.scratchIn.data.ready := false.B

  io.writePort.valid := false.B
  io.writePort.bits := DontCare

  val StateReg = RegInit(0.U(4.W))
  val reg = Reg(io.in.request.bits.cloneType)

  val burstCNT = RegInit(0.U(8.W))

  io.in.response.valid := StateReg =/= 0.U //Indicates to controller that the DMA is invoked and working. Used for determining when read is complete in SysController
  io.in.response.bits.completed := false.B

  switch(StateReg){
    is(0.U){ // Recieves operation
      io.in.request.ready := true.B
      
      when(io.in.request.valid){
        reg := io.in.request.bits
        StateReg := 1.U
      }
    }
    is(1.U){ // Requests Scratchpad transfer
      io.scratchIn.request.bits.addr := reg.addr

      io.scratchIn.request.bits.burstSize := reg.burstSize
      io.scratchIn.request.bits.burstStride := reg.burstStride
      io.scratchIn.request.bits.burstCnt := reg.burstCnt
      
      when(reg.burstSize =/= 0.U){ //FIXME: Incredibly hacky
        when(io.scratchIn.request.ready){
          io.scratchIn.request.valid := true.B
          StateReg := 2.U
        } 
      }.otherwise{
        StateReg := 3.U
      }
    }
    is(2.U){ // Writes data into Systolic array buffers
      when(io.writePort.ready){
        io.scratchIn.data.ready := true.B
        
        when(io.scratchIn.data.valid){
          val mask = HelperFunctions.uintToBoolVec(reg.burstSize, c.dataBusSize) 

          (io.writePort.bits zip io.scratchIn.data.bits.readData zip mask).foreach{case ((port,data),mask) => 
            when(mask){
              port := data
            }.otherwise{
              port := 0.U
            }
          }

          io.writePort.valid := true.B

          when(io.scratchIn.data.bits.last){
            StateReg := 3.U
          }
        }
      }
    }
    is(3.U){ // Waits for Controller to agnoledge finis
      io.in.response.valid := true.B
      io.in.response.bits.completed := true.B // Write completed succesfully 

      when(io.in.response.ready){ 
        StateReg := 0.U
      }
    }
  }
}
