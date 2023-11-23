package ATA8

import chisel3._
import chisel3.util._


object HelperFunctions{
  def uintToBoolVec(uint: UInt, n: Int): Vec[Bool] = {
    VecInit((0 until n).map(i => uint > i.U))
  }  
}

class SysWriteDMA(implicit c: Configuration) extends Module {  
  val io = IO(new Bundle {
    val in = Flipped(new DMAWrite)
    val scratchOut = new WriteportScratch
    val readPort = new Readport(Vec(c.dataBusSize,UInt(8.W)),10)
  })

  io.in.request.ready := false.B

  io.scratchOut.request.valid := false.B
  io.scratchOut.request.bits := DontCare

  io.scratchOut.data.valid := false.B
  io.scratchOut.data.bits := DontCare
  io.scratchOut.data.bits.last := false.B

  io.readPort.request.valid := false.B
  io.readPort.request.bits := DontCare

  val StateReg = RegInit(0.U(4.W))
  val reg = Reg(io.in.request.bits.cloneType)

  val burstCNT = RegInit(0.U(8.W))

  io.in.response.valid := StateReg =/= 0.U //Indicates to controller that the DMA is invoked and working 
  io.in.response.bits.completed := false.B
  io.in.response.bits.tag := 0.U

  switch(StateReg){
    is(0.U){
      io.in.request.ready := true.B
      
      when(io.in.request.valid){
        reg := io.in.request.bits
        StateReg := 1.U
      }
    }
    is(1.U){
      io.scratchOut.request.bits.addr := reg.addr
      //io.scratchOut.request.bits.burst := reg.size
      io.scratchOut.request.bits.burstMode := false.B //TODO: make this a string
      
      io.scratchOut.request.bits.burstCnt := reg.burstCnt
      io.scratchOut.request.bits.burstSize := reg.burstSize
      io.scratchOut.request.bits.burstStride := reg.burstStride 

      when(reg.burstSize =/= 0.U){ // FIXME: incredibly hacky. not a good solution 
        when(io.scratchOut.request.ready){
          io.scratchOut.request.valid := true.B
          StateReg := 2.U
        } 
      }.otherwise{
        StateReg := 3.U
      }
    }
    is(2.U){
      io.readPort.request.bits.addr := reg.addr + burstCNT

      when(io.scratchOut.data.ready){
        io.readPort.request.valid := true.B

        io.scratchOut.data.bits.writeData := io.readPort.response.bits.readData
        io.scratchOut.data.bits.strb := HelperFunctions.uintToBoolVec(reg.burstSize, c.dataBusSize) 

        when(io.readPort.response.valid){
          io.scratchOut.data.valid := true.B

          when(burstCNT < (reg.burstCnt - 1.U)){ //FIXME: Might have to change size width
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
      io.in.response.bits.completed := true.B // Write complete successfully 
      io.in.response.bits.tag := reg.tag

      when(io.in.response.ready){
        StateReg := 0.U
      }
    }
  }
}
