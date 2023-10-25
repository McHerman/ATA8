package ATA8

import chisel3._
import chisel3.experimental._
import chisel3.util._

class StoreController(implicit c: Configuration) extends Module {          
  def splitInt(input: UInt, totalWidth: Int, subWidth: Int): Seq[UInt] = {
    require(totalWidth % subWidth == 0, "Total width must be a multiple of sub width")
    val numSplits = totalWidth / subWidth
    (0 until numSplits).map(i => input(subWidth*(i+1)-1, subWidth*i))
  }

  val io = IO(new Bundle {
    val instructionStream = new Readport(new StoreInstIssue,0)
    val AXIST = new AXIST_2(64,2,1,1,1) 
    val readport = new ReadportScratch
		//val tagRead = new TagRead
		//val tagDealloc = Decoupled(UInt(c.tagWidth.W))
		//val event = Flipped(Valid(new Event()))
  })

	io.instructionStream.request.valid := false.B
	io.instructionStream.request.bits := DontCare

	io.AXIST := DontCare

	io.AXIST.tvalid := false.B // these definitions take priority, kinda hacky 
	io.AXIST.tlast := false.B

	io.readport.request.valid := false.B
  io.readport.request.bits := DontCare

	io.readport.data.ready := false.B

	/* io.tagRead.request.valid := false.B
	io.tagRead.request.bits := DontCare */

	//io.tagRead.response.ready := false.B

	/* io.tagDealloc.valid := false.B
	io.tagDealloc.bits := DontCare */

	val burstAddrReg = RegInit(0.U(32.W))
  val addrTemp = RegInit(0.U(32.W))
  val idReg = RegInit(0.U(2.W))
	val tagReg = RegInit(0.U(c.tagWidth.W))

  val reg = Reg(new StoreInstIssue)

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
			io.readport.request.valid := true.B
			io.readport.request.bits.addr := reg.addrs(0).addr
			io.readport.request.bits.burst := reg.size

			when (io.AXIST.tready) {
				io.readport.data.ready := true.B

				when(io.readport.data.valid){
					when(burstAddrReg < (reg.size-1.U)){
						//io.AXIST.tdata := io.readport.data.bits.readData
						io.AXIST.tdata := io.readport.data.bits.readData.reverse.reduce((a, b) => Cat(a, b))
						io.AXIST.tkeep := "h11".U
						io.AXIST.tstrb := "h11".U

						burstAddrReg := burstAddrReg + 1.U
					}.otherwise{
						//io.AXIST.tdata := io.readport.data.bits.readData
						io.AXIST.tdata := io.readport.data.bits.readData.reverse.reduce((a, b) => Cat(a, b))
						io.AXIST.tkeep := "h11".U
						io.AXIST.tstrb := "h11".U

						io.AXIST.tlast := true.B
						//StateReg := 4.U
            StateReg := 0.U
					}			
				}
      }
		}
		/* is(2.U){
      io.tagDealloc.valid := true.B
			io.tagDealloc.bits := tagReg
			StateReg := 0.U
		} */
  }
}