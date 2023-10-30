package ATA8

import chisel3._
import chisel3.util._

class AXI_out extends Module {
  val io = IO(new Bundle {
    val AXIST = new AXIST_2(64,2,1,1,1)
    val enq_in = Flipped(Decoupled(new Bundle{val data = UInt(64.W); val keep = UInt(8.W); val strb = UInt(8.W); val last = Bool()}))
    
    val deq_clk = Input(Clock())
    val deq_rst = Input(Bool())
  })




  val myQueue = Module(new AsyncQueue(new Bundle{val data = UInt(64.W); val keep = UInt(8.W); val strb = UInt(8.W); val last = Bool()}))

  // Main clock domain (Dequeue Side)
  myQueue.io.enq <> io.enq_in
  myQueue.io.enq_clock := clock
  myQueue.io.enq_reset := reset

  // Another clock domain (Enqueue Side)
  withClockAndReset(io.deq_clk, io.deq_rst) {
    myQueue.io.deq.ready := io.AXIST.tready
    io.AXIST.tvalid := myQueue.io.deq.valid

    io.AXIST.tdata := myQueue.io.deq.bits.data
    io.AXIST.tlast := myQueue.io.deq.bits.last
    io.AXIST.tkeep := myQueue.io.deq.bits.keep
    io.AXIST.tstrb := myQueue.io.deq.bits.strb

    io.AXIST.tuser := 0.U
    io.AXIST.tid := 0.U
    io.AXIST.tdest := 0.U
    io.AXIST.tresp := 0.U

    myQueue.io.deq_clock := clock // 'clock' and 'reset' are implicit in withClockAndReset scope
    myQueue.io.deq_reset := reset
  }
}