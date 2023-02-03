import chisel3._
import chisel3.experimental._
import chisel3.util._

class PE(Size: Int) extends Module {
  val io = IO(new Bundle {
    val Right_In = Flipped(new Element_Right)
    val Right_Out = new Element_Right
    val Down_In = Flipped(new Element_Down)
    val Down_Out = new Element_Down 
  })

  // IO

  val Mem1 = SyncReadMem(1024, UInt(8.W))
  val Mem2 = SyncReadMem(1024, UInt(8.W))

  val Right = Wire(Vec(Size + 1, Vec(Size + 1, UInt(8.W)))) 
  val Down = Wire(Vec(Size + 1, Vec(Size + 1, UInt(8.W)))) 

  for(n <- 0 until Size){
    for(k <- 0 until Size){
      val MAC = Module(new MAC())
      MAC.io.Right_In := Right(n)(k)
      MAC.io.Down_In := Down(n)(k)

      Right(n+1)(k+1) := MAC.io.Right_Out
      Down(n+1)(k+1) := MAC.io.Down_Out
    }
  }
}
