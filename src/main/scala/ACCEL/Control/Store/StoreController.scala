package ATA8

import chisel3._
import chisel3.experimental._
import chisel3.util._
//import chisel3.util.experimental.BoringUtils


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

    val debug = new StoreDebug
  })

	io.instructionStream.request.valid := false.B
	io.instructionStream.request.bits := DontCare

	io.AXIST := DontCare

	io.AXIST.tvalid := false.B // these definitions take priority, kinda hacky 
	io.AXIST.tlast := false.B

	io.readport.request.valid := false.B
  //io.readport.request.bits := DontCare
  
  io.readport.request.bits.addr := 0.U
  io.readport.request.bits.burstStride := 0.U
  io.readport.request.bits.burstSize := 0.U
  io.readport.request.bits.burstCnt := 0.U


	io.readport.data.ready := false.B

	val burstAddrReg = RegInit(0.U(32.W))
  val addrTemp = RegInit(0.U(32.W))
  val idReg = RegInit(0.U(2.W))
	val tagReg = RegInit(0.U(c.tagWidth.W))

  val reg = Reg(new StoreInstIssue)

  val StateReg = RegInit(0.U(4.W))

  val transferReg = Reg(new Bundle{val totalTransfers = UInt(8.W); val hasPartial = Bool(); val partial = UInt(log2Ceil(c.dataBusSize + 1).W)})

  /// DEBUG ///

  io.debug.state := StateReg
  io.debug.axiReady := io.AXIST.tready
  io.debug.readPortValid := io.readport.data.valid

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
      val totalElements = reg.size * reg.size

      // FIXME: Parameterize all this 

      val fullTransfers = totalElements >> 3.U // Right shift by 3 for division by 8
      val partial = totalElements & 0x7.U // Bitwise AND with 7 (0x7) for modulo
      val hasPartial = partial =/= 0.U

      val totalTransfers = fullTransfers + Mux(hasPartial, 1.U, 0.U)

      transferReg.totalTransfers := totalTransfers
      transferReg.hasPartial := hasPartial
      transferReg.partial := partial

      StateReg := 2.U
    }
    is(2.U){
      io.readport.request.bits.addr := reg.addrs(0).addr
      io.readport.request.bits.burstStride := c.dataBusSize.U
      io.readport.request.bits.burstSize := c.dataBusSize.U
      io.readport.request.bits.burstCnt := transferReg.totalTransfers

      when(io.readport.request.ready){
        io.readport.request.valid := true.B
        StateReg := 3.U
      } 
    }
    is(3.U){
      when(io.AXIST.tready) {
				io.readport.data.ready := true.B

				when(io.readport.data.valid){
          io.AXIST.tdata := io.readport.data.bits.readData.reverse.reduce((a, b) => Cat(a, b))
					io.AXIST.tkeep := "hff".U
					io.AXIST.tstrb := "hff".U
          io.AXIST.tvalid := true.B

          when(io.readport.data.bits.last){
            when(transferReg.hasPartial){
              io.AXIST.tstrb := HelperFunctions.uintToBoolVec(transferReg.hasPartial, c.dataBusSize).asUInt
            }

						io.AXIST.tlast := true.B
            StateReg := 0.U
					}			
				}
      }
    }  
  }
}