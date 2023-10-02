package ATA8

import chisel3._
import chisel3.util._


class SysCtrl() extends Module {
  val io = IO(new Bundle {
    val State = Input(UInt(1.W))
    val Cnt = Input(UInt(8.W))
    val Activate = Output(Bool())
    val Shift = Output(Bool())
    val Trigger = Input(Bool())
    val Enable = Output(Bool())
  })

  io.Shift := false.B
  io.Activate := false.B
  io.Enable := false.B

  val FinalCnt = RegInit(0.U(8.W))
  val ShiftCnt = RegInit(0.U(8.W))
  val ActivateCnt = RegInit(0.U(8.W))
  val EnableCnt = RegInit(0.U(8.W))
  val WaitCnt = RegInit(0.U(8.W))

  val StateReg = RegInit(0.U(4.W))

  switch(StateReg){ // TODO, Add enumerations  
    is(0.U){
      when(io.Trigger){
        FinalCnt := io.Cnt
  
        switch(io.State){
          is(0.U){
            StateReg := 1.U
          }
          is(1.U){
            StateReg := 4.U
          }
        }
      }
    }
    is(1.U){
      when(ShiftCnt < FinalCnt){
        io.Shift := true.B
        ShiftCnt := ShiftCnt + 1.U
      }.otherwise{
        ShiftCnt := 0.U
        StateReg := 2.U
      }
    }
    is(2.U){
      when(ActivateCnt < FinalCnt){
        io.Activate := true.B
        ActivateCnt := ActivateCnt + 1.U
      }
      
      
      when(EnableCnt < (FinalCnt << 1)){
        io.Enable := true.B
        EnableCnt := EnableCnt + 1.U
      }.otherwise{
        ActivateCnt := 0.U
        EnableCnt := 0.U
        StateReg := 0.U
      }
    }
    is(4.U){
      when(ActivateCnt < FinalCnt){
        io.Activate := true.B
        ActivateCnt := ActivateCnt + 1.U
      }
      
      
      when(EnableCnt < ((FinalCnt << 1) - 1.U)){
        io.Enable := true.B
        EnableCnt := EnableCnt + 1.U
      }.otherwise{
        ActivateCnt := 0.U
        EnableCnt := 0.U
        StateReg := 5.U
      }
    }
    is(5.U){
      when(WaitCnt < FinalCnt){
        WaitCnt := WaitCnt + 1.U
      }.otherwise{
        WaitCnt := 0.U
        StateReg := 6.U
      }
    }
    is(6.U){
      when(ShiftCnt < FinalCnt){
        io.Shift := true.B
        ShiftCnt := ShiftCnt + 1.U
      }.otherwise{
        ShiftCnt := 0.U
        StateReg := 0.U
      }
    }
  }

}

/*
object SysCtrl extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new SysCtrl())
}
*/
