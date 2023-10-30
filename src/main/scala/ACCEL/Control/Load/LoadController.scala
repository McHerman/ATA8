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
    val instructionStream = new Readport(new LoadInstIssue,0)
    //val AXIST = Flipped(new AXIST_2(64,2,1,1,1))
    val AXIData = Flipped(Decoupled(new Bundle{val data = UInt(64.W); val strb = UInt(8.W); val last = Bool()}))
    val writeport = new WriteportScratch
    val event = Valid(new Event)
  })

	io.instructionStream.request.valid := false.B
	io.instructionStream.request.bits := DontCare

	io.writeport.request.valid := false.B
	io.writeport.data.valid := false.B

  io.writeport.request.bits := DontCare
  io.writeport.data.bits := DontCare
  io.writeport.data.bits.last := false.B

	//io.AXIST.tready := false.B
  io.AXIData.ready := false.B

  io.event.valid := false.B
  io.event.bits := DontCare

	val burstAddrReg = RegInit(0.U(32.W))
  val addrTemp = RegInit(0.U(32.W))
  val idReg = RegInit(0.U(2.W))

  val reg = Reg(new LoadInstIssue)

  val StateReg = RegInit(0.U(4.W))

  switch(StateReg){
		is(0.U){
      when(io.instructionStream.request.ready){
        io.instructionStream.request.valid := true.B

        when(io.instructionStream.response.valid){
          reg := io.instructionStream.response.bits.readData
          StateReg := 1.U
        }
      }
		}
		is(1.U){
			/* io.AXIST.tready := true.B

      when (io.AXIST.tvalid) {
        when (io.AXIST.tlast) {
          StateReg := 2.U
          idReg:= io.AXIST.tid
        }.otherwise {
          burstAddrReg := burstAddrReg + 1.U // Increase address by 4 bytes for next beat
        }

        io.writeport.request.bits.addr := reg.addr.addr + burstAddrReg
					
				when(io.writeport.request.ready){ // Assuming that the line remains open.
					io.writeport.request.valid := true.B
					when(io.writeport.data.ready){
            io.writeport.data.bits.writeData := VecInit(splitInt(io.AXIST.tdata,64,8))
            
            io.writeport.data.bits.strb := io.AXIST.tstrb.asBools
            io.writeport.data.valid := true.B
					}
				}
      } */


      when(io.writeport.request.ready){ // Assuming that the line remains open.
				io.writeport.request.valid := true.B
        io.writeport.request.bits.addr := reg.addr.addr
				io.writeport.request.bits.burst := reg.size

        StateReg := 2.U
			}
		}
    is(2.U){
      when(io.writeport.data.ready){
        //io.AXIST.tready := true.B
        io.AXIData.ready := true.B

        //when(io.AXIST.tvalid){
        when(io.AXIData.valid){
          //io.writeport.data.bits.writeData := VecInit(splitInt(io.AXIST.tdata,64,c.arithDataWidth)) // big fucking problem here 
          io.writeport.data.bits.writeData := VecInit(splitInt(io.AXIData.bits.data,64,c.arithDataWidth)) // big fucking problem here 
          //io.writeport.data.bits.strb := io.AXIST.tstrb.asBools
          io.writeport.data.bits.strb := io.AXIData.bits.strb.asBools
          io.writeport.data.valid := true.B

          when(burstAddrReg < (reg.size - 1.U)){
            burstAddrReg := burstAddrReg + 1.U
          //}.elsewhen(io.AXIST.tlast){
          }.elsewhen(io.AXIData.bits.last){
            StateReg := 3.U
            burstAddrReg := 0.U
            io.writeport.data.bits.last := true.B
          }.otherwise{
            //return an error of some kind 
            StateReg := 0.U
          }
        }
			}
		}
		is(3.U){
      /* when(io.tagRegister.addr.ready){
        io.tagRegister.addr.bits.addr := reg.addr
        io.tagRegister.addr.bits.ready := true.B

        io.tagRegister.addr.valid := true.B 

        when(io.tagRegister.tag.valid){
          StateReg := 0.U
        }
      } */

      io.event.valid := true.B
      io.event.bits.tag := reg.addr.tag

      StateReg := 0.U
		}
  }
}