package ATA8

import chisel3._
import chisel3.util._

//class ExeDecode(implicit c: Configuration) extends Module {
class ExeDecode(config: Configuration) extends Module {
  implicit val c = config

  val io = IO(new Bundle {
    val instructionStream = Flipped(Decoupled(new InstructionPackage))
    val issueStream = Decoupled(new ExecuteInst)
    val tagFetch = Vec(2, new TagRead())
    val tagFetchfromLoad = Vec(2, new TagRead())
    val tagRegister = new TagWrite()
  })
 
  io.tagFetch.foreach{case (element) => element.request.valid := false.B; element.request.bits := DontCare}
  io.tagRegister.addr.valid := false.B
  io.tagRegister.addr.bits := DontCare

  io.instructionStream.ready := false.B
  
  io.issueStream.valid := false.B
  io.issueStream.bits := DontCare

  val registerFile = Reg(new ExecuteInst())

  val nop = Wire(Bool())
  //nop := !io.instructionStream.bits.instruction(0) // if bit 0 is false, we dont operate
  nop := !io.instructionStream.valid // if bit 0 is false, we dont operate


  val nopReg = Wire(Bool())
  nopReg := !registerFile.op

  //val tagFetched = Wire(Vec(2,Bool()))

  //tagFetched(0) := false.B
  //tagFetched(1) := false.B

  val tagFetched = Reg(Vec(2,Bool()))    

  //val inst = io.instructionStream.bits.instruction.asTypeOf(new penis)


  val addrd = io.instructionStream.bits.instruction(31, 16)
  val addrs = VecInit(io.instructionStream.bits.instruction(47, 32), io.instructionStream.bits.instruction(63, 48))

  io.tagFetch.zipWithIndex.foreach { case (element,i) => element.request.valid := !nop; element.request.bits.addr := addrs(i) }
  io.tagFetchfromLoad.zipWithIndex.foreach { case (element,i) => element.request.valid := !nop; element.request.bits.addr := addrs(i)}

  when(io.tagFetch(0).response.valid){
    tagFetched(0) := true.B
    registerFile.ids(0).ready := io.tagFetch(0).response.bits.ready
    registerFile.ids(0).tag  := io.tagFetch(0).response.bits.tag
  }.elsewhen(io.tagFetchfromLoad(0).response.valid){
    tagFetched(0) := true.B
    registerFile.ids(0).ready := io.tagFetchfromLoad(0).response.bits.ready
    registerFile.ids(0).tag  := io.tagFetchfromLoad(0).response.bits.tag
  }.otherwise{
    tagFetched(0) := false.B
  }

  when(io.tagFetch(1).response.valid){
    tagFetched(1) := true.B
    registerFile.ids(1).ready := io.tagFetch(1).response.bits.ready
    registerFile.ids(1).tag  := io.tagFetch(1).response.bits.tag
  }.elsewhen(io.tagFetchfromLoad(1).response.valid){
    tagFetched(1) := true.B
    registerFile.ids(1).ready := io.tagFetchfromLoad(1).response.bits.ready
    registerFile.ids(1).tag  := io.tagFetchfromLoad(1).response.bits.tag
  }.otherwise{
    tagFetched(1) := false.B
  }

  //  val writeCompleted = RegInit(0.U(1.W))
  
  val writeCompleted = RegInit(0.U(1.W))

  val stall = Wire(Bool())
  stall := false.B

  when(!stall && io.tagRegister.addr.ready && !nop){
    io.tagRegister.addr.bits.addr := addrd
    io.tagRegister.addr.bits.ready := false.B
    io.tagRegister.addr.valid := true.B 

    when(io.tagRegister.tag.valid){
      registerFile.idd := io.tagRegister.tag.bits
      writeCompleted := true.B
    }.otherwise{
      writeCompleted := false.B
    }
  }

  val pipeReady = Wire(Bool())
  pipeReady := false.B

  io.issueStream.bits := registerFile

  when(!nopReg && writeCompleted.asBool && tagFetched(0) && tagFetched(1)){
    io.issueStream.valid := true.B
    pipeReady := true.B
  }.elsewhen(nopReg){
    pipeReady := true.B
  }.otherwise{
    when(writeCompleted.asBool){
      stall := true.B
    }
  }

  when(pipeReady){
    io.instructionStream.ready := true.B

    when(io.instructionStream.valid){
      registerFile.op := io.instructionStream.bits.instruction(0)
      registerFile.mode := io.instructionStream.bits.instruction(1)
      registerFile.grainSize := io.instructionStream.bits.instruction(7,4) 
      registerFile.size := io.instructionStream.bits.instruction(15, 8)
    }.otherwise{
      registerFile := DontCare
      registerFile.op := 0.U
    }
  }

}


object ExeDecode extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new ExeDecode(Configuration.default()))
}
