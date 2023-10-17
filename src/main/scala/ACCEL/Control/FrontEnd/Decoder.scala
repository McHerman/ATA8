package ATA8

import chisel3._
import chisel3.util._

//class ExeDecode(implicit c: Configuration) extends Module {
class Decoder(config: Configuration) extends Module {
  implicit val c = config

  val io = IO(new Bundle {
    val instructionStream = Flipped(Decoupled(new InstructionPackage))
    val exeStream = Decoupled(new ExecuteInstIssue)
    val loadStream = Decoupled(new LoadInstIssue)
    val storeStream = Decoupled(new StoreInstIssue)
    val tagFetch = Vec(2, new TagRead())
    val tagRegister = new TagWrite()
  })
  
  io.exeStream.valid := false.B
  io.exeStream.bits := DontCare

	io.loadStream.valid := false.B
  io.loadStream.bits := DontCare

	io.storeStream.valid := false.B
  io.storeStream.bits := DontCare


  io.tagFetch.foreach{case (element) => 
    element.request.valid := false.B
    element.request.bits := DontCare
  }

  io.tagRegister.addr.valid := false.B
  io.tagRegister.addr.bits := DontCare

	val selReg = RegInit(0.U(4.W))
	val selRegDelay = RegInit(0.U(4.W))
  
  val exeFile = Reg(new ExeInstDecode())
  val loadFile = Reg(new LoadInstDecode())
  val storeFile = Reg(new StoreInstDecode())

  val exeIssueReg = Reg(new ExecuteInstIssue())
  val loadIssueReg = Reg(new LoadInstIssue())
  val storeIssueReg = Reg(new StoreInstIssue())

  val stall = WireDefault(false.B)

  io.instructionStream.ready := !stall

	when(io.instructionStream.valid && !stall){
		switch(io.instructionStream.bits.instruction(3,0)){
			is(1.U){
				exeFile := io.instructionStream.bits.instruction.asTypeOf(new ExeInstDecode)
				selReg := 1.U
			}
			is(2.U){
				loadFile := io.instructionStream.bits.instruction.asTypeOf(new LoadInstDecode)
				selReg := 2.U
			}
			is(3.U){
				storeFile := io.instructionStream.bits.instruction.asTypeOf(new StoreInstDecode)
				selReg := 3.U
			}
		}
	}


  val exeValid = RegInit(false.B)
  val loadValid = RegInit(false.B)
  val storeValid = RegInit(false.B)

  exeValid := false.B
  loadValid := false.B
  storeValid := false.B

  switch(selReg){
		is(1.U){ // Execute

      when(io.exeStream.ready){
        val addrs = VecInit(exeFile.addrs1, exeFile.addrs2)
        io.tagFetch.zipWithIndex.foreach { case (element,i) => element.request.valid := true.B; element.request.bits.addr := addrs(i); 
          when(element.response.valid){
            exeIssueReg.addrs(i).addr := addrs(i)
            exeIssueReg.addrs(i).tag := element.response.bits.tag
            exeIssueReg.addrs(i).ready := element.response.bits.ready
          }.otherwise{
            exeIssueReg.addrs(i).addr := addrs(i)
            exeIssueReg.addrs(i).tag := 0.U
            exeIssueReg.addrs(i).ready := true.B
          }
        }

        when(io.tagRegister.addr.ready){
          io.tagRegister.addr.bits.addr := exeFile.addrd
          io.tagRegister.addr.bits.ready := false.B
          io.tagRegister.addr.valid := true.B 

          when(io.tagRegister.tag.valid){
            exeIssueReg.addrd.tag := io.tagRegister.tag.bits
            exeIssueReg.addrd.addr := exeFile.addrd
            exeValid := true.B
          }.otherwise{
            stall := true.B
          }
        }.otherwise{
          stall := true.B
        }
      }.otherwise{
        stall := true.B

      }
		} 
		is(2.U){ // Load
      when(io.loadStream.ready){
        when(io.tagRegister.addr.ready){
          io.tagRegister.addr.bits.addr := loadFile.addr
          io.tagRegister.addr.bits.ready := false.B
          io.tagRegister.addr.valid := true.B 

          when(io.tagRegister.tag.valid){
            loadIssueReg.addr.tag := io.tagRegister.tag.bits
            loadIssueReg.addr.addr := loadFile.addr
            loadIssueReg.addr.ready := true.B 
            loadValid := true.B
          }.otherwise{
            stall := true.B
          }
        }.otherwise{
          stall := true.B
        }
      }.otherwise{
        stall := true.B
      }
		}
		is(3.U){ // Store 
      when(io.storeStream.ready){
        io.tagFetch(0).request.valid := true.B
        io.tagFetch(0).request.bits.addr := storeFile.addr

        when(io.tagFetch(0).response.valid){
          storeIssueReg.addr.addr := storeFile.addr
          storeIssueReg.addr.tag := io.tagFetch(0).response.bits.tag
          storeIssueReg.addr.ready := io.tagFetch(0).response.bits.ready
        }.otherwise{
          storeIssueReg.addr.addr := storeFile.addr
          storeIssueReg.addr.tag := 0.U
          storeIssueReg.addr.ready := true.B
        }

        storeValid := true.B
      }.otherwise{
        stall := true.B
      }
		}
	}

  when(io.exeStream.ready && exeValid){
    io.exeStream.valid := true.B
    io.exeStream.bits := exeIssueReg

    exeValid := false.B
  }.elsewhen(exeValid){
    exeValid := true.B
  }

  when(io.loadStream.ready && loadValid){
    io.loadStream.valid := true.B
    io.loadStream.bits := loadIssueReg

    loadValid := false.B
  }.elsewhen(loadValid){
    loadValid := true.B
  }


  when(io.storeStream.ready && storeValid){
    io.storeStream.valid := true.B
    io.storeStream.bits := storeIssueReg

    storeValid := false.B
  }.elsewhen(storeValid){
    storeValid := true.B
  }



}


object Decoder extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new Decoder(Configuration.default()))
}
