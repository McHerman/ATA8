package ATA8

import chisel3._
import chisel3.util._

class AXI_in extends Module {
  val io = IO(new Bundle {
    val AXIST = Flipped(new AXIST_2(64,2,1,1,1))
    val deq_out = Decoupled(new Bundle{val data = UInt(64.W); val strb = UInt(8.W); val last = Bool()})
    
    val enq_clk = Input(Clock())
    val enq_rst = Input(Bool())
    val deq_clk = Input(Clock())
    val deq_rst = Input(Bool())
  })

  val myQueue = Module(new AsyncQueue(new Bundle{val data = UInt(64.W); val strb = UInt(8.W); val last = Bool()}))

  // Main clock domain (Dequeue Side)
  myQueue.io.deq <> io.deq_out
  myQueue.io.deq_clock := io.deq_clk
  myQueue.io.deq_reset := io.deq_rst

  // Another clock domain (Enqueue Side)
  /* withClockAndReset(io.enq_clk, io.enq_rst) {
    io.AXIST.tready := myQueue.io.enq.ready
    myQueue.io.enq.valid := io.AXIST.tvalid

    myQueue.io.enq.bits.data := io.AXIST.tdata
    myQueue.io.enq.bits.last := io.AXIST.tlast
    myQueue.io.enq.bits.strb := io.AXIST.tstrb
    
    myQueue.io.enq_clock := io.enq_clk // 'clock' and 'reset' are implicit in withClockAndReset scope
    myQueue.io.enq_reset := io.enq_rst
  } */


  io.AXIST.tready := myQueue.io.enq.ready

  myQueue.io.enq.valid := io.AXIST.tvalid
  myQueue.io.enq.bits.data := io.AXIST.tdata
  myQueue.io.enq.bits.last := io.AXIST.tlast
  myQueue.io.enq.bits.strb := io.AXIST.tstrb
  
  myQueue.io.enq_clock := io.enq_clk // 'clock' and 'reset' are implicit in withClockAndReset scope
  myQueue.io.enq_reset := io.enq_rst
}
