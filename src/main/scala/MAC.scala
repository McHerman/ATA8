import chisel3._
import chisel3.experimental._
import chisel3.util._

class MAC() extends Module {
  val io = IO(new Bundle {
    val Right_In = Flipped(new Element_Right)
    val Right_Out = new Element_Right
    val Down_In = Flipped(new Element_Down)
    val Down_Out = new Element_Down 
  })

  // IO

  val Reg = RegInit(0.U(1.W))

  when(!io.Down_In.Weight_Stall){
    Reg := io.Down_In.Weight
  }

  io.Down_Out.Weight := Reg
  io.Down_Out.Weight_Stall := io.Down_In.Weight_Stall
  io.Down_Out.Carry := (io.Right_In.Edge * Reg) + io.Down_In.Carry

  io.Right_Out.Edge := io.Right_In.Edge
}
