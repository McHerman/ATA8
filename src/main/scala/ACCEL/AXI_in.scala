package ATA8

import chisel3._
import chisel3.util._

/* class AXI_in extends Module {
  val io = IO(new Bundle {
    val enq_in  = Flipped(Decoupled(UInt(8.W)))
    val deq_out = Decoupled(UInt(8.W))
    
    val enq_clk = Input(Clock())
    val enq_rst = Input(Bool())
    val deq_clk = Input(Clock())
    val deq_rst = Input(Bool())
  })

  // Instantiate AsyncQueue
  val myQueue = Module(new AsyncQueue(UInt(8.W)))

  // Connect Enqueue Side
  myQueue.io.enq <> io.enq_in
  
  // Connect Dequeue Side
  io.deq_out <> myQueue.io.deq

  // Set Clock and Reset for Enqueue Side
  myQueue.io.enq_clock := io.enq_clk
  myQueue.io.enq_reset := io.enq_rst

  // Set Clock and Reset for Dequeue Side
  myQueue.io.deq_clock := io.deq_clk
  myQueue.io.deq_reset := io.deq_rst
} */

class AXI_in extends Module {
  val io = IO(new Bundle {
    //val enq_in  = Flipped(Decoupled(UInt(8.W)))
    val AXIST = Flipped(new AXIST_2(64,2,1,1,1))
    val deq_out = Decoupled(new Bundle{val data = UInt(64.W); val strb = UInt(8.W); val last = Bool()})
    //val readPort = new Readport(new Bundle{val data = UInt(64.W); val last = Bool()},0)
    
    val enq_clk = Input(Clock())
    val enq_rst = Input(Bool())
    val deq_clk = Input(Clock())
    val deq_rst = Input(Bool())
  })

  // Instantiate AsyncQueue
  //val myQueue = Module(new AsyncQueue(UInt(8.W)))

  val myQueue = Module(new AsyncQueue(new Bundle{val data = UInt(64.W); val strb = UInt(8.W); val last = Bool()}))

  // Connect Enqueue Side

  io.AXIST.tready := myQueue.io.enq.ready
  myQueue.io.enq.valid := io.AXIST.tvalid
  
  myQueue.io.enq.bits.data := io.AXIST.tdata
  myQueue.io.enq.bits.last := io.AXIST.tlast
  myQueue.io.enq.bits.strb := io.AXIST.tstrb

  //myQueue.io.enq <> io.enq_in
  
  // Connect Dequeue Side
  io.deq_out <> myQueue.io.deq

  /* io.readPort.request.ready := myQueue.io.deq.ready 
  myQueue.io.deq.valid := io.readPort.request.valid 

  io.readPort.response.bits := myQueue.io.deq.bits
  io.readPort.response.valid := true.B
 */


  // Set Clock and Reset for Enqueue Side
  myQueue.io.enq_clock := io.enq_clk
  myQueue.io.enq_reset := io.enq_rst

  // Set Clock and Reset for Dequeue Side
  myQueue.io.deq_clock := io.deq_clk
  myQueue.io.deq_reset := io.deq_rst
}

object AXI_in extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new AXI_in())
}