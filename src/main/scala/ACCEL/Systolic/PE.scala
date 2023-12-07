package ATA8

import chisel3._
import chisel3.util._
import chisel3.util.HasBlackBoxInline

class DSPMultiplier(width: Int) extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val a = Input(UInt(width.W))
    val b = Input(UInt(width.W))
    val result = Output(UInt((2 * width).W)) // Output width is twice the input width
  })

  // The purpose of this module is to ensure that Vivado instantiates DSP's 

  setInline("DSPMultiplier.v",
    s"""|(* use_dsp48 = "yes" *)
        |module DSPMultiplier #(
        |    parameter WIDTH = $width
        |)(
        |    input [WIDTH-1:0] a,
        |    input [WIDTH-1:0] b,
        |    output [2*WIDTH-1:0] result // Adjusted output width
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

  val multiplier = Module(new DSPMultiplier(c.arithDataWidth))

  io.yOut.Y := DontCare

  val DataReg = RegInit(0.U(c.arithDataWidth.W))
  val XReg = RegInit(0.U(c.arithDataWidth.W))
  val YReg = RegInit(0.U(c.arithDataWidth.W))

  XReg := io.x.X
  io.xOut.X := XReg

  multiplier.io.a := 0.U
  multiplier.io.b := 0.U

  switch(io.ctrl.state) {
    is(0.U) {
      when(io.ctrl.shift){
        YReg := io.y.Y
        io.yOut.Y := YReg
      }.otherwise{
        io.yOut.Y := DataReg
        multiplier.io.a := io.x.X
        multiplier.io.b := YReg

        if(c.fixedpoint){
          DataReg := multiplier.io.result(15,8) + io.y.Y
        }else{
          DataReg := multiplier.io.result(7,0) + io.y.Y
        }
      }
    }
    is(1.U) {
      when(io.ctrl.shift){
        DataReg := io.y.Y
        io.yOut.Y := DataReg
      }.otherwise{
        multiplier.io.a := io.x.X
        multiplier.io.b := io.y.Y

        if(c.fixedpoint){
          DataReg := multiplier.io.result(15,8) + io.y.Y
        }else{
          DataReg := multiplier.io.result(7,0) + io.y.Y
        }

        YReg := io.y.Y
        io.yOut.Y := YReg
      }
    }
  }
}


