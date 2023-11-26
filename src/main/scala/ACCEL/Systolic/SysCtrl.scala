package ATA8

import chisel3._
import chisel3.util._


class SysCtrl(implicit c: Configuration) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new SysOP))
    
    val Activate = Output(Bool()) //TODO: change to camelcase
    val Shift = Output(Bool())
    val Enable = Output(Bool())
    val Mode = Output(UInt(1.W))
    val sizes = Output(Vec(c.grainDim, UInt(log2Ceil(c.dataBusSize + 1).W)))
    val activateLoopBack = Input(Bool())

    val completed = Valid(new Bundle{val tag = UInt(c.tagWidth.W)})
  })

  io.in.ready := false.B

  io.Shift := false.B
  io.Activate := false.B
  io.Enable := false.B
  io.Mode := 0.U

  io.completed.valid := false.B
  io.completed.bits := DontCare

  val ShiftCnt = RegInit(0.U(8.W))
  val ActivateCnt = RegInit(0.U(8.W))
  val EnableCnt = RegInit(0.U(8.W))
  val WaitCnt = RegInit(0.U(8.W))

  val StateReg = RegInit(0.U(4.W))

  val inReg = Reg(new SysOP)

  io.sizes := inReg.sizes

  switch(StateReg){ // TODO, Add enumerations  
    is(0.U){
      io.in.ready := true.B

      when(io.in.valid){
        inReg := io.in.bits
  
        switch(io.in.bits.mode){
          is(0.U){
            StateReg := 1.U
          }
          is(1.U){
            StateReg := 5.U
          }
        }
      }
    }
    is(1.U){
      io.Mode := 0.U

      when(ShiftCnt < inReg.size){
        io.Shift := true.B
        ShiftCnt := ShiftCnt + 1.U
      }.otherwise{
        ShiftCnt := 0.U
        StateReg := 2.U
      }
    }
    is(2.U){ // 
      io.Mode := 0.U

      when(ActivateCnt < inReg.size){
        io.Activate := true.B
        ActivateCnt := ActivateCnt + 1.U
      }.otherwise{
        ActivateCnt := 0.U
        StateReg := 4.U
      }
    }
    is(4.U){
      when(ActivateCnt < inReg.size){
        when(io.activateLoopBack){
          ActivateCnt := ActivateCnt + 1.U
        }
      }.otherwise{
        ActivateCnt := 0.U
        StateReg := 0.U

        io.completed.bits.tag := inReg.tag
        io.completed.valid := true.B
      }
    }

    is(5.U){
      io.Mode := 1.U

      when(ActivateCnt < inReg.size){
        io.Activate := true.B
        ActivateCnt := ActivateCnt + 1.U
      }
      
      when(EnableCnt < ((inReg.size * 2.U))){
        io.Enable := true.B
        EnableCnt := EnableCnt + 1.U
      }.otherwise{
        ActivateCnt := 0.U
        EnableCnt := 0.U
        StateReg := 6.U
      }
    }
    is(6.U){
      io.Mode := 1.U

      when(WaitCnt < inReg.size){
        WaitCnt := WaitCnt + 1.U
      }.otherwise{
        WaitCnt := 0.U
        StateReg := 7.U
      }
    }
    is(7.U){ // 
      io.Mode := 1.U

      when(ShiftCnt < inReg.size){
        io.Shift := true.B
        ShiftCnt := ShiftCnt + 1.U
      }.otherwise{
        ShiftCnt := 0.U
        StateReg := 0.U

        io.completed.bits.tag := inReg.tag
        io.completed.valid := true.B
      }
    }
  }

}
