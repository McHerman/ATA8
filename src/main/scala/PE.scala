import chisel3._
import chisel3.experimental._
import chisel3.util._
import scala.math

class PE(Size: Int, MemSize: Int) extends Module {
  val io = IO(new Bundle {
    val EdgeIn = Input(Vec(Size,UInt(8.W)))
    val EdgeOut = Input(Vec(Size,UInt(8.W)))

    val Memwrite = Input(Bool())
    val MemAdress = Input(UInt(10.W))
    val WriteData = Input(UInt(16.W))
  })

  // IO

  var Bitsize = log2Floor(Size)

  val EdgeMem = SyncReadMem(MemSize, UInt(8.W))
  val WheightMem = SyncReadMem(MemSize, UInt(8.W))
  val AccuMem = SyncReadMem(MemSize, UInt(8.W))

  val Weight_File = Reg(Vec(Size,UInt(Bitsize.W)))
  val Edge_File = Reg(Vec(Size,UInt(Bitsize.W)))
  val Out_File = Reg(Vec(Size,UInt(Bitsize.W)))

  val OpCounter = RegInit(0.U(4.W))

  val SubCounter = RegInit(0.U(Bitsize.W))
  val WheightOffsetReg = RegInit(0.U(24.W))
  val EdgeOffsetReg = RegInit(0.U(24.W))

  val StallReg = RegInit(0.U(1.W))
  val StateReg = RegInit(0.U(1.W))

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
  
  for(n <- 0 until Size){
    Down(n)(0).Stall := 1.U
    Down(n)(0).Carry := 0.U
    Down(n)(0).State := 0.U
    Down(n)(0).Weight := 0.U
  }

  switch(OpCounter){
    is(0.U){
      when(!StallReg){
        when(StateReg){
          OpCounter := 4.U
        }.elsewhen{
          OpCounter := 1.U
        }
      }
    }
    is(1.U){ 
      Weight_File(SubCounter) := WheightMem(SubCounter + WheightOffsetReg)
      Edge_File(SubCounter) := WheightMem(SubCounter + EdgeOffsetReg)    

      when(SubCounter < Size.U){
        SubCounter := SubCounter + 1.U
        OpCounter := 2.U
      }
    }
    is(2.U){
      for(n <- 0 until Size){
        Down(n)(0).Stall := 0.U
        Down(n)(0).Carry := 0.U
        Down(n)(0).State := 0.U
        Down(n)(0).Weight := Weight_File(n)
      }

      Right_In(0) := Edge_File
    }
    is(3.U){  

      
    }
  }





  }







}
