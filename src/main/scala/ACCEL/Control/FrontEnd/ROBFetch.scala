package ATA8

import chisel3._
import chisel3.util._
import chisel3.util.MixedVec._

class DecodePackage(implicit c: Configuration) extends Bundle {
  val op = UInt(4.W)
  val data = MixedVec(new ExecuteInst, new LoadInst, new StoreInst)
}

class IssuePackage(implicit c: Configuration) extends Bundle {
  val op = UInt(4.W)
  val data = MixedVec(new ExecuteInstIssue, new LoadInstIssue, new StoreInstIssue)
}

class ROBFetch(implicit c: Configuration) extends Module {
  val io = IO(new Bundle {
    val instructionStream = Flipped(Decoupled(new DecodePackage))

    val tagFetch = Vec(2, new TagRead())
    val tagRegister = new TagWrite()
    val event = Vec(2,Flipped(Valid(new Event)))

    val issueStream = MixedVec(Decoupled(new ExecuteInstIssue), Decoupled(new LoadInstIssue), Decoupled(new StoreInstIssue))
  })


  io.instructionStream.ready := false.B

  io.tagFetch.foreach{element => 
    element.request.valid := false.B
    element.request.bits.addr := DontCare
  }

  io.tagRegister.addr.valid := false.B
  io.tagRegister.addr.bits := DontCare

  io.issueStream.foreach{element => element.valid := false.B}

  val inReg = Reg(new DecodePackage)

  val stall = WireDefault(false.B)
  val inputStall = WireDefault(false.B)
  val assignValid = WireDefault(false.B)

  val issueReg = Reg(new IssuePackage)

  when(!inputStall){
    io.instructionStream.ready := true.B

    when(io.instructionStream.valid){
      inReg := io.instructionStream.bits
    }.otherwise{
      inReg.op := 0.U
      inReg.data := DontCare
    }
  }

  def fetchAndRegister(inst: InstBase, issue: InstIssueBase): Unit = {
    // Fetch IDs for all addresses in the addrs vector
    inst.addrs.zipWithIndex.foreach { case (addrBundle, i) =>
      io.tagFetch(i).request.valid := true.B
      io.tagFetch(i).request.bits.addr := addrBundle.addr
      when(io.tagFetch(i).response.valid) {
        issue.addrs(i).depend := io.tagFetch(i).response.bits
      }.otherwise {
        issue.addrs(i).depend.tag := 0.U
        issue.addrs(i).depend.ready := true.B
      }

      issue.addrs(i).addr := addrBundle.addr
    }

    // Register addresses in the addrd vector
    inst.addrd.zipWithIndex.foreach { case (addrBundle, i) =>
      // Set up the tag register for writing
      io.tagRegister.addr.valid := true.B
      io.tagRegister.addr.bits.addr := addrBundle.addr
      when(io.tagRegister.tag.valid) { 
        issue.addrd(i).tag := io.tagRegister.tag.bits
      }.otherwise {
        inputStall := true.B
      }

      issue.addrd(i).addr := addrBundle.addr
    }
  }

  when(!stall) {
    switch(inReg.op) {
      is(1.U) { // ExecuteInst
        fetchAndRegister(inReg.data(0).asInstanceOf[ExecuteInst], issueReg.data(0).asInstanceOf[ExecuteInstIssue])
        issueReg.data(0).asInstanceOf[ExecuteInstIssue].op := inReg.data(0).asInstanceOf[ExecuteInst].op
        issueReg.data(0).asInstanceOf[ExecuteInstIssue].size := inReg.data(0).asInstanceOf[ExecuteInst].size
        issueReg.data(0).asInstanceOf[ExecuteInstIssue].mode := inReg.data(0).asInstanceOf[ExecuteInst].mode
        
        when(!inputStall){
          issueReg.op := 1.U
          assignValid := true.B
        }

      }
      is(2.U) { // LoadInst
        fetchAndRegister(inReg.data(1).asInstanceOf[LoadInst], issueReg.data(1).asInstanceOf[LoadInstIssue])
        issueReg.data(1).asInstanceOf[LoadInstIssue].op := inReg.data(1).asInstanceOf[LoadInst].op
        issueReg.data(1).asInstanceOf[LoadInstIssue].size := inReg.data(1).asInstanceOf[LoadInst].size
        issueReg.data(1).asInstanceOf[LoadInstIssue].mode := inReg.data(1).asInstanceOf[LoadInst].mode
        
        when(!inputStall){
          issueReg.op := 2.U
          assignValid := true.B
        }
      }
      is(3.U) { // StoreInst
        fetchAndRegister(inReg.data(2).asInstanceOf[StoreInst], issueReg.data(2).asInstanceOf[StoreInstIssue])
        issueReg.data(2).asInstanceOf[StoreInstIssue].op := inReg.data(2).asInstanceOf[StoreInst].op
        issueReg.data(2).asInstanceOf[StoreInstIssue].size := inReg.data(2).asInstanceOf[StoreInst].size

        when(!inputStall){
          issueReg.op := 3.U
          assignValid := true.B
        }
      }
    }
  }.otherwise{
    inputStall := true.B
  }


  //TODO: Fix that garbage

  io.issueStream.zip(issueReg.data).foreach { case (issuePort, issueData) => issuePort.bits := issueData.asInstanceOf[issuePort.bits.type] }


  io.event.foreach{case (event) => 
    issueReg.data.foreach{element => 
      element.addrs.foreach{addr => 
        when(event.valid && event.bits.tag === addr.depend.tag){
			    addr.depend.ready := true.B
        }
      }
    }
	}

  when(issueReg.op =/= 0.U){
    switch(issueReg.op){
      is(1.U){
        when(io.issueStream(0).ready){
          io.issueStream(0).valid := true.B

          when(!assignValid){
            issueReg.op := 0.U
          }
        }.otherwise{
          stall := true.B
        }
      }
      is(2.U){
        when(io.issueStream(1).ready){
          io.issueStream(1).valid := true.B

          when(!assignValid){
            issueReg.op := 0.U
          }
        }.otherwise{
          stall := true.B
        }
      }
      is(3.U){
        when(io.issueStream(2).ready){
          io.issueStream(2).valid := true.B

          when(!assignValid){
            issueReg.op := 0.U
          }
        }.otherwise{
          stall := true.B
        }
      }
    }
  }
}
