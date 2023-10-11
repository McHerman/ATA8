package ATA8

import chisel3._
import chisel3.experimental._
import chisel3.util._

class LoadController(implicit c: Configuration) extends Module {          
  def splitInt(input: UInt, totalWidth: Int, subWidth: Int): Seq[UInt] = {
    require(totalWidth % subWidth == 0, "Total width must be a multiple of sub width")
    val numSplits = totalWidth / subWidth
    (0 until numSplits).map(i => input(subWidth*(i+1)-1, subWidth*i))
  }

  val io = IO(new Bundle {
    //val instructionStream = Flipped(Decoupled(new LoadInst))
    val instructionStream = new Readport(new LoadInst,0)
    val AXIST = Flipped(new AXIST_2(64,2,1,1,1))
    val writeport = new WriteportScratch
    val tagRegister = new TagWrite
  })

	io.instructionStream.request.valid := false.B
	io.instructionStream.request.bits := DontCare

	io.writeport.request.valid := false.B
	io.writeport.data.valid := false.B

  io.writeport.request.bits := DontCare
  io.writeport.data.bits := DontCare

  io.tagRegister.addr.valid := false.B
  io.tagRegister.addr.bits := DontCare

	io.AXIST.tready := false.B

	val burstAddrReg = RegInit(0.U(32.W))
  val addrTemp = RegInit(0.U(32.W))
  val idReg = RegInit(0.U(2.W))

  val reg = Reg(new LoadInst)

  val StateReg = RegInit(0.U(4.W))

  switch(StateReg){
		is(0.U){
			io.instructionStream.request.valid := true.B

			when(io.instructionStream.response.valid){
				reg := io.instructionStream.response.bits.readData
        StateReg := 1.U
			}
		}
		is(1.U){
			io.AXIST.tready := true.B

      when (io.AXIST.tvalid) {
        when (io.AXIST.tlast) {
          StateReg := 2.U
          idReg:= io.AXIST.tid
        }.otherwise {
          burstAddrReg := burstAddrReg + 1.U // Increase address by 4 bytes for next beat
        }

        io.writeport.request.bits.addr := reg.addr + burstAddrReg
					
				when(io.writeport.request.ready){ // Assuming that the line remains open.
					io.writeport.request.valid := true.B
					when(io.writeport.data.ready){
            io.writeport.data.bits.writeData := VecInit(splitInt(io.AXIST.tdata,64,8))
            
            io.writeport.data.bits.strb := io.AXIST.tstrb.asBools
            io.writeport.data.valid := true.B
					}
				}
      }
		}
		is(2.U){
      when(io.tagRegister.addr.ready){
        io.tagRegister.addr.bits.addr := reg.addr
        io.tagRegister.addr.bits.ready := true.B

        io.tagRegister.addr.valid := true.B 

        when(io.tagRegister.tag.valid){
          StateReg := 0.U
        }
      }
		}
  }
}