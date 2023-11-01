package ATA8

import chisel3._
import chisel3.util._

class AXI_out extends Module {
  val io = IO(new Bundle {
    val AXIST = new AXIST_2(64,2,1,1,1)
    val enq_in = Flipped(Decoupled(new Bundle{val data = UInt(64.W); val keep = UInt(8.W); val strb = UInt(8.W); val last = Bool()}))

    val enq_clk = Input(Clock())
    val enq_rst = Input(Bool())
    val deq_clk = Input(Clock())
    val deq_rst = Input(Bool())
  })

  val myParams = AsyncQueueParams(
    depth = 8,   // Your desired depth
    sync = 3,     // Your desired sync
    safe = false,  // Your desired safe value
    narrow = false // Your desired narrow value
  )

  val myQueue = Module(new AsyncQueue(new Bundle{val data = UInt(64.W); val keep = UInt(8.W); val strb = UInt(8.W); val last = Bool()},myParams))

  // Main clock domain (Dequeue Side)
  myQueue.io.enq <> io.enq_in
  myQueue.io.enq_clock := io.enq_clk
  myQueue.io.enq_reset := io.enq_rst

  // Another clock domain (Enqueue Side)
  /* withClockAndReset(io.deq_clk, io.deq_rst) {
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

    myQueue.io.deq_clock := io.deq_clk // 'clock' and 'reset' are implicit in withClockAndReset scope
    myQueue.io.deq_reset := io.deq_rst
  } */

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

  myQueue.io.deq_clock := io.deq_clk // 'clock' and 'reset' are implicit in withClockAndReset scope
  myQueue.io.deq_reset := io.deq_rst
}
