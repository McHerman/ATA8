package ATA8

import chisel3._
import chisel3.util._
import chisel3.experimental.Analog
import chisel3.util.HasBlackBoxInline

class DSPMultiplier(width: Int) extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val a = Input(UInt(width.W))
    val b = Input(UInt(width.W))
    val result = Output(UInt(width.W)) // Output width is twice the input width
  })

  setInline("DSPMultiplier.v",
    s"""|(* use_dsp48 = "yes" *)
        |module DSPMultiplier #(
        |    parameter WIDTH = $width
        |)(
        |    input [WIDTH-1:0] a,
        |    input [WIDTH-1:0] b,
        |    output [WIDTH-1:0] result
        |);
        |    assign result = a * b; // Perform multiplication
        |endmodule
        |""".stripMargin)
}
// PE Module
class PE(implicit c: Configuration) extends Module {
  val io = IO(new Bundle {
    val x = Flipped(new PEX(c.arithDataWidth))
    val xOut = new PEX(c.arithDataWidth)
    val y = Flipped(new PEY(c.arithDataWidth,1))
    val yOut = new PEY(c.arithDataWidth,1)
    val ctrl = Input(new Bundle{val state = UInt(1.W); val shift = Bool()})
  })

  //val multiplier = Module(new Multiplier(c.arithDataWidth))
  val multiplier = Module(new DSPMultiplier(c.arithDataWidth))

  //io.yOut.PEState := io.y.PEState
  io.yOut.Y := DontCare

  val DataReg = RegInit(0.U(c.arithDataWidth.W))
  val XReg = RegInit(0.U(c.arithDataWidth.W))
  val YReg = RegInit(0.U(c.arithDataWidth.W))

  XReg := io.x.X
  io.xOut.X := XReg

  multiplier.io.a := 0.U
  multiplier.io.b := 0.U
  //multiplier.io.enable := 0.U

  //switch(io.y.PEState.State) {
  switch(io.ctrl.state) {
    is(0.U) {
      io.yOut.Y := DataReg
      multiplier.io.a := io.x.X
      multiplier.io.b := YReg

      DataReg := multiplier.io.result + io.y.Y


      /* when(io.y.PEState.EN) {
        //multiplier.io.enable := true.B
        DataReg := multiplier.io.result + io.y.Y
      }.otherwise {
        //multiplier.io.enable := false.B
        when(io.y.PEState.Shift) {
          YReg := io.y.Y
        }
        io.yOut.Y := YReg
      } */

      //when(io.y.PEState.Shift) {
      when(io.ctrl.shift){
        YReg := io.y.Y
        io.yOut.Y := YReg
      }
    }
    is(1.U) {
      multiplier.io.a := io.x.X
      multiplier.io.b := io.y.Y

      DataReg := multiplier.io.result + DataReg

      YReg := io.y.Y
      io.yOut.Y := YReg

      //when(io.y.PEState.Shift) {
      when(io.ctrl.shift){
        DataReg := io.y.Y
        io.yOut.Y := DataReg
      }
        

      /* when(io.y.PEState.EN) {
        //multiplier.io.enable := true.B
        DataReg := multiplier.io.result + DataReg
        YReg := io.y.Y
        io.yOut.Y := YReg
      }.otherwise {
        //multiplier.io.enable := false.B
        when(io.y.PEState.Shift) {
          DataReg := io.y.Y
        }
        io.yOut.Y := DataReg
      } */
    }
  }
}


