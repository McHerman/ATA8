package ATA8

import chisel3._
import chisel3.util._
  
class SysController(config: Configuration) extends Module {

  implicit val c = config
  
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new ExecuteInstIssue))
    val scratchOut = new WriteportScratch
    val scratchIn = Vec(2,new ReadportScratch)
		val memport = Vec(2,Decoupled(new Memport_V3(c.arithDataWidth*c.grainDim,10)))
    val readPort = new Readport(Vec(c.grainDim,UInt(c.arithDataWidth.W)),10)
		val out = Decoupled(new SysOP)
    val sysCompleted = Flipped(Valid(new Bundle{val id = UInt(4.W)}))
  })

  val opbuffer = Module(new BufferFIFO(8,new SysOP)) // FIXME: Magic number
  val readBuffer = Module(new BufferFIFO(8, new Bundle{val addr = UInt(16.W); val size = UInt(8.W)}))

	val SysDMA = Module(new SysDMA())
	val SysDMA2 = Module(new SysDMA())

  val SysWriteDMA = Module(new SysWriteDMA())
 
	io.in.ready := false.B

	io.scratchOut.request.valid := false.B
	io.scratchOut.request.bits := DontCare

	io.scratchOut.data.valid := false.B
	io.scratchOut.data.bits := DontCare

	io.memport(0).valid := false.B
	io.memport(0).bits := DontCare

	io.memport(1).valid := false.B
	io.memport(1).bits := DontCare

  io.scratchOut <> SysWriteDMA.io.scratchOut
  io.readPort <> SysWriteDMA.io.readPort

  SysWriteDMA.io.in.valid := false.B
  SysWriteDMA.io.in.bits := DontCare

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
  opbuffer.io.ReadData.request.bits := DontCare

  readBuffer.io.WriteData.valid := false.B
	readBuffer.io.WriteData.bits := DontCare
	
	readBuffer.io.ReadData.request.valid := false.B
  readBuffer.io.ReadData.request.bits := DontCare


	val reg = Reg(new ExecuteInstIssue)
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
			when(SysDMA.io.in.ready && SysDMA2.io.in.ready){ //TODO: clean up this shit
				SysDMA.io.in.bits.addr := reg.addrs(0).addr
				SysDMA.io.in.bits.size := reg.size
				SysDMA2.io.in.bits.addr := reg.addrs(0).addr
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
			when(opbuffer.io.WriteData.ready && readBuffer.io.WriteData.ready){
				opbuffer.io.WriteData.valid := true.B
				opbuffer.io.WriteData.bits.mode := reg.mode
				opbuffer.io.WriteData.bits.size := reg.mode
        opbuffer.io.WriteData.bits.id := 0.U // FIXME: Temporary fix, build tag allocator


        readBuffer.io.WriteData.valid := true.B
				readBuffer.io.WriteData.bits.addr := reg.addrd.addr
				readBuffer.io.WriteData.bits.size := reg.size

				StateReg := 0.U
			}
		}
	}


  when(io.sysCompleted.valid && readBuffer.io.ReadData.request.ready){ // FIXME: Dangerous, can lock if syscompleted only stays high for one cc
    when(SysWriteDMA.io.in.ready){
      readBuffer.io.ReadData.request.valid := true.B
      SysWriteDMA.io.in.bits <> readBuffer.io.ReadData.response.bits.readData
      SysWriteDMA.io.in.valid := true.B
    }
  }

  opbuffer.io.ReadData.request.valid := io.out.ready
  io.out.valid := opbuffer.io.ReadData.response.valid
	io.out.bits <> opbuffer.io.ReadData.response.bits.readData


}

object SysController extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new SysController(Configuration.default()))
}
