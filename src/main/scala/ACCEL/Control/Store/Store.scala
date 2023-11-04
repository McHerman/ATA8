package ATA8

import chisel3._
import chisel3.experimental._
import chisel3.util._

class Store(config: Configuration) extends Module {
  implicit val c = config

  val io = IO(new Bundle {
    val instructionStream = Flipped(Decoupled(new StoreInstIssue))
    //val tagDealloc = Decoupled(UInt(c.tagWidth.W))
    val AXIST = new AXIST_2(64,2,1,1,1)
    val readPort = new ReadportScratch
		//val tagRead = new TagRead
		val event = Vec(2,Flipped(Valid(new Event())))
    //val debug = new StoreDebug
  })

  //val queue = Module(new BufferFIFO(16,new StoreInstIssue))
  val queue = Module(new DependTrack(16,new StoreInstIssue,1)) 

  //val queue = Module(new DependTrack(16)) 

  val StoreController = Module(new StoreController)

  queue.io.WriteData <> io.instructionStream
  queue.dependio.event <> io.event
  //queue.io.event <> io.event


  //io.instructionStream.ready := queue.io.WriteData.ready

 	StoreController.io.instructionStream <> queue.io.ReadData
  StoreController.io.AXIST <> io.AXIST
	//StoreController.io.event <> io.event
	//StoreController.io.tagRead <> io.tagRead
	
	//io.tagDealloc <> StoreController.io.tagDealloc

  io.readPort <> StoreController.io.readport 

  /// DEBUG ///

  //StoreController.io.debug <> io.debug
  
}

object Store extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new Store(Configuration.default()))
}