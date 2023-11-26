package ATA8

import chisel3._
import chisel3.experimental._
import chisel3.util._

class Store(implicit c: Configuration) extends Module {
  val io = IO(new Bundle {
    val instructionStream = Flipped(Decoupled(new StoreInstIssue))

    val AXIST = new AXIST_2(64,2,1,1,1) //TODO: add to config 
    val readPort = new ReadportScratch

		val event = Vec(2,Flipped(Valid(new Event())))
    val debug = new StoreDebug
  })

  val queue = Module(new DependTrack(32,new StoreInstIssue,1)) //TODO: add to config

  val StoreController = Module(new StoreController)

  queue.io.WriteData <> io.instructionStream
  queue.dependio.event <> io.event

 	StoreController.io.instructionStream <> queue.io.ReadData
  StoreController.io.AXIST <> io.AXIST

  io.readPort <> StoreController.io.readport 

  /// DEBUG ///

  StoreController.io.debug <> io.debug
  
}