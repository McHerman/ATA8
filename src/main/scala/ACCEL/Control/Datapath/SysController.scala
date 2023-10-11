package ATA8

import chisel3._
import chisel3.util._
  
class SysController(config: Configuration) extends Module {

  implicit val c = config
  
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new ExecuteInst))
    val scratchOut = new WriteportScratch
    val scratchIn = Vec(2,new ReadportScratch)
		val readAddr = Vec(2/* FIXME: magic fucking number*/,new Readport(UInt(c.addrWidth.W), c.tagWidth))
		val memport = Vec(2,Decoupled(new Memport_V3(c.arithDataWidth*c.grainDim,10)))
		val out = Decoupled(new SysOP)
  })

  val opbuffer = Module(new BufferFIFO(8,new SysOP))

	val SysDMA = Module(new SysDMA())
	val SysDMA2 = Module(new SysDMA())

	io.in.ready := false.B

	io.scratchOut.request.valid := false.B
	io.scratchOut.request.bits := DontCare

	io.scratchOut.data.valid := false.B
	io.scratchOut.data.bits := DontCare

	/* io.scratchIn(0).request.valid := false.B
	io.scratchIn(0).data.ready := false.B
	io.scratchIn(0).request.bits := DontCare

	io.scratchIn(1).request.valid := false.B
	io.scratchIn(1).data.ready := false.B
	io.scratchIn(1).data.bits := DontCare */

	io.readAddr(0).request.valid := false.B
	io.readAddr(0).request.bits := DontCare

	io.readAddr(1).request.valid := false.B
	io.readAddr(1).request.bits := DontCare

	io.memport(0).valid := false.B
	io.memport(0).bits := DontCare

	io.memport(1).valid := false.B
	io.memport(1).bits := DontCare

	SysDMA.io.scratchIn <> io.scratchIn(0)
	SysDMA2.io.scratchIn <> io.scratchIn(1)
	
	SysDMA.io.memport <> io.memport(0)
	SysDMA2.io.memport <> io.memport(1)

	SysDMA.io.in.valid := false.B
	SysDMA.io.in.bits := DontCare

	SysDMA2.io.in.valid := false.B
	SysDMA2.io.in.bits := DontCare

	opbuffer.io.WriteData.valid := false.B
	opbuffer.io.WriteData.bits := DontCare
	
	opbuffer.io.ReadData.request.valid := false.B

	val reg = Reg(new ExecuteInst)
  val StateReg = RegInit(0.U(4.W))
	
	val transfercompleted = Reg(Vec(2,Bool()))

	switch(StateReg){
		is(0.U){
			io.in.ready := true.B

			when(io.in.valid){
				reg := io.in.bits
			}
		}
		is(1.U){
			io.readAddr.zipWithIndex.foreach { case (element,i) => element.request.valid := true.B; element.request.bits.addr := reg.ids(i).tag}

			when(SysDMA.io.in.ready && SysDMA2.io.in.ready && io.readAddr(0).response.valid && io.readAddr(1).response.valid){ //TODO: clean up this shit
				SysDMA.io.in.bits.addr := io.readAddr(0).response.bits.readData
				SysDMA.io.in.bits.size := reg.size
				SysDMA2.io.in.bits.addr := io.readAddr(1).response.bits.readData
				SysDMA2.io.in.bits.size := reg.size

				StateReg := 2.U
			}
		}
		is(2.U){
			when(SysDMA.io.completed){
				transfercompleted(0) := true.B
			}

			when(SysDMA2.io.completed){
				transfercompleted(1) := true.B
			}

			when((transfercompleted(0) || SysDMA.io.completed) && (transfercompleted(1) || SysDMA2.io.completed)){
				StateReg := 3.U
			}
		}
		is(3.U){
			when(opbuffer.io.WriteData.ready){
				opbuffer.io.WriteData.valid := true.B
				opbuffer.io.WriteData.bits.mode := reg.mode
				opbuffer.io.WriteData.bits.size := reg.mode

				StateReg := 0.U
			}
		}
	}

	io.out <> opbuffer.io.ReadData

}

object SysController extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new SysController(Configuration.default()))
}
