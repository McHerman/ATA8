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
    //val AXIST = new AXIST_2(64,2,1,1,1) 
    val AXIData = Decoupled(new Bundle{val data = UInt(64.W); val keep = UInt(8.W); val strb = UInt(8.W); val last = Bool()})
    val readport = new ReadportScratch
		//val tagRead = new TagRead
		//val tagDealloc = Decoupled(UInt(c.tagWidth.W))
		//val event = Flipped(Valid(new Event()))
  })

	io.instructionStream.request.valid := false.B
	io.instructionStream.request.bits := DontCare

	io.AXIData.bits := DontCare

	io.AXIData.valid := false.B // these definitions take priority, kinda hacky 
	io.AXIData.bits.last := false.B

	io.readport.request.valid := false.B
  io.readport.request.bits := DontCare

	io.readport.data.ready := false.B

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

			//when (io.AXIST.tready) {
			when (io.AXIData.ready) {
				io.readport.data.ready := true.B

				when(io.readport.data.valid){
          
          /* io.AXIST.tdata := io.readport.data.bits.readData.reverse.reduce((a, b) => Cat(a, b))
					io.AXIST.tkeep := "h11".U
					io.AXIST.tstrb := "h11".U
          io.AXIST.tvalid := true.B */

          io.AXIData.bits.data := io.readport.data.bits.readData.reverse.reduce((a, b) => Cat(a, b))
					io.AXIData.bits.keep := "h11".U
					io.AXIData.bits.strb := "h11".U
          io.AXIData.valid := true.B

					when(burstAddrReg < (reg.size-1.U)){
						burstAddrReg := burstAddrReg + 1.U
					}.elsewhen(io.readport.data.bits.last){
						//io.AXIST.tlast := true.B
            io.AXIData.bits.last := true.B
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