package ATA8

import chisel3._
import chisel3.util._

// Multiplier Module
class Multiplier(width: Int) extends Module {
  val io = IO(new Bundle {
    val a = Input(UInt(width.W))
    val b = Input(UInt(width.W))
    val enable = Input(Bool())
    val result = Output(UInt(width.W))
  })

  io.result := 0.U
  when(io.enable) {
    io.result := io.a * io.b
  }
}

// PE Module
class PE(implicit c: Configuration) extends Module {
  val io = IO(new Bundle {
    val X_IN = Flipped(new PEX(c.arithDataWidth))
    val X_OUT = new PEX(c.arithDataWidth)
    val Y_IN = Flipped(new PEY(c.arithDataWidth,1))
    val Y_OUT = new PEY(c.arithDataWidth,1)
  })

  val multiplier = Module(new Multiplier(c.arithDataWidth))

  io.Y_OUT.PEState := io.Y_IN.PEState
  io.Y_OUT.Y := DontCare

  val DataReg = RegInit(0.U(c.arithDataWidth.W))
  val XReg = RegInit(0.U(c.arithDataWidth.W))
  val YReg = RegInit(0.U(c.arithDataWidth.W))

  XReg := io.X_IN.X
  io.X_OUT.X := XReg

  multiplier.io.a := 0.U
  multiplier.io.b := 0.U
  multiplier.io.enable := 0.U

  switch(io.Y_IN.PEState.State) {
    is(0.U) {
      io.Y_OUT.Y := DataReg
      multiplier.io.a := io.X_IN.X
      multiplier.io.b := YReg
      when(io.Y_IN.PEState.EN) {
        multiplier.io.enable := true.B
        DataReg := multiplier.io.result + io.Y_IN.Y
      }.otherwise {
        multiplier.io.enable := false.B
        when(io.Y_IN.PEState.Shift) {
          YReg := io.Y_IN.Y
        }
        io.Y_OUT.Y := YReg
      }
    }
    is(1.U) {
      multiplier.io.a := io.X_IN.X
      multiplier.io.b := io.Y_IN.Y
      when(io.Y_IN.PEState.EN) {
        multiplier.io.enable := true.B
        DataReg := multiplier.io.result + DataReg
        YReg := io.Y_IN.Y
        io.Y_OUT.Y := YReg
      }.otherwise {
        multiplier.io.enable := false.B
        when(io.Y_IN.PEState.Shift) {
          DataReg := io.Y_IN.Y
        }
        io.Y_OUT.Y := DataReg
      }
    }
  }
}


/*
class PE(implicit c: Configuration) extends Module {
  val io = IO(new Bundle {
    val X_IN = Flipped(new PEX(c.arithDataWidth))
    val X_OUT = new PEX(c.arithDataWidth)
    val Y_IN = Flipped(new PEY(c.arithDataWidth,1))
    val Y_OUT = new PEY(c.arithDataWidth,1)
  })

  io.Y_OUT.PEState := io.Y_IN.PEState
  io.Y_OUT.Y := DontCare

  val DataReg = RegInit(0.U(c.arithDataWidth.W))
  val XReg = RegInit(0.U(c.arithDataWidth.W))
  val YReg = RegInit(0.U(c.arithDataWidth.W))

  XReg := io.X_IN.X
  io.X_OUT.X := XReg

  switch(io.Y_IN.PEState.State){
    is(0.U){

      io.Y_OUT.Y := DataReg

      when(io.Y_IN.PEState.EN){
        //DataReg := ((io.X_IN.X * YReg) >> c.arithDataWidth/2) + io.Y_IN.Y
        DataReg := ((io.X_IN.X.asUInt(16.W) * YReg)) + io.Y_IN.Y
      }.otherwise{
        when(io.Y_IN.PEState.Shift){
          YReg := io.Y_IN.Y
        }

        io.Y_OUT.Y := YReg
      }

    }
    is(1.U){
      when(io.Y_IN.PEState.EN){
        //DataReg := ((io.X_IN.X * io.Y_IN.Y) >> c.arithDataWidth/2) + DataReg
        DataReg := (io.X_IN.X * io.Y_IN.Y) + DataReg

        YReg := io.Y_IN.Y
        io.Y_OUT.Y := YReg
      }.otherwise{
        when(io.Y_IN.PEState.Shift){
          DataReg := io.Y_IN.Y
        }

        io.Y_OUT.Y := DataReg
      }
    }
  }
}
*/
