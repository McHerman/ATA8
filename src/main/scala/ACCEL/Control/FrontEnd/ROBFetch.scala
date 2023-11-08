package ATA8

import chisel3._
import chisel3.util._
import chisel3.util.MixedVec._

class DecodePackage(implicit c: Configuration) extends Bundle {
  val op = UInt(4.W)
  val data = MixedVec(new ExeInstDecode, new LoadInstDecode, new StoreInstDecode)
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
  //val stallReg = RegInit(false.B)
  //val inputStall = WireDefault(false.B)

  //io.issueStream.ready := !stall || !inputStall

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

  when(!stall){
    switch(inReg.op){
      is(1.U){
        val addrs = VecInit(inReg.data(0).asInstanceOf[ExeInstDecode].addrs1, inReg.data(0).asInstanceOf[ExeInstDecode].addrs2)
        io.tagFetch.zipWithIndex.foreach { case (element,i) => element.request.valid := true.B; element.request.bits.addr := addrs(i); 
          issueReg.data(0).asInstanceOf[ExecuteInstIssue].addrs(i).addr := addrs(i)
          
          when(element.response.valid){
            issueReg.data(0).asInstanceOf[ExecuteInstIssue].addrs(i).depend := element.response.bits
            //issueReg.data(0).addrs(i).depend.tag := element.response.bits.tag
            //issueReg.data(0).addrs(i).depend.ready := element.response.bits.ready
          }.otherwise{
            issueReg.data(0).asInstanceOf[ExecuteInstIssue].addrs(i).depend.tag := 0.U
            issueReg.data(0).asInstanceOf[ExecuteInstIssue].addrs(i).depend.ready := true.B
          }
        }

        when(io.tagRegister.addr.ready){
          io.tagRegister.addr.bits.addr := inReg.data(0).asInstanceOf[ExeInstDecode].addrd
          io.tagRegister.addr.bits.ready := false.B
          io.tagRegister.addr.valid := true.B 

          when(io.tagRegister.tag.valid){
            issueReg.data(0).asInstanceOf[ExecuteInstIssue].addrd.tag := io.tagRegister.tag.bits
            issueReg.data(0).asInstanceOf[ExecuteInstIssue].addrd.addr := inReg.data(0).asInstanceOf[ExeInstDecode].addrd
            issueReg.op := 1.U
            assignValid := true.B
          }.otherwise{
            inputStall := true.B
            issueReg.op := 0.U
          }
        }.otherwise{
          inputStall := true.B
          issueReg.op := 0.U
        }

        issueReg.data(0).asInstanceOf[ExecuteInstIssue].size := inReg.data(0).asInstanceOf[ExeInstDecode].size
        issueReg.data(0).asInstanceOf[ExecuteInstIssue].mode := inReg.data(0).asInstanceOf[ExeInstDecode].mode

      }
      is(2.U){
        when(io.tagRegister.addr.ready){
          io.tagRegister.addr.bits.addr := inReg.data(1).asInstanceOf[LoadInstDecode].addr
          io.tagRegister.addr.bits.ready := false.B
          io.tagRegister.addr.valid := true.B
          
          when(io.tagRegister.tag.valid){
            issueReg.data(1).asInstanceOf[LoadInstIssue].addr.tag := io.tagRegister.tag.bits
            issueReg.data(1).asInstanceOf[LoadInstIssue].addr.addr := inReg.data(1).asInstanceOf[LoadInstDecode].addr

            issueReg.data(1).asInstanceOf[LoadInstIssue].size := inReg.data(1).asInstanceOf[LoadInstDecode].size
            issueReg.data(1).asInstanceOf[LoadInstIssue].op := inReg.data(1).asInstanceOf[LoadInstDecode].op
            issueReg.data(1).asInstanceOf[LoadInstIssue].mode := inReg.data(1).asInstanceOf[LoadInstDecode].mode

            issueReg.op := 2.U
            assignValid := true.B

            //issueReg.data(1).addr.ready := true.B 
          }.otherwise{
            inputStall := true.B
            issueReg.op := 0.U
          }
        }.otherwise{
          inputStall := true.B
          issueReg.op := 0.U
        }

      }
      is(3.U){
        io.tagFetch(0).request.valid := true.B
        io.tagFetch(0).request.bits.addr := inReg.data(2).asInstanceOf[StoreInstDecode].addr

        issueReg.data(2).asInstanceOf[StoreInstIssue].addrs(0).addr := inReg.data(2).asInstanceOf[StoreInstDecode].addr
        when(io.tagFetch(0).response.valid){
          issueReg.data(2).asInstanceOf[StoreInstIssue].addrs(0).depend := io.tagFetch(0).response.bits
          //issueReg(2).data.addrs(0).depend.tag := io.tagFetch(0).response.bits.tag
          //issueReg(2).data.addrs(0).depend.ready := io.tagFetch(0).response.bits.ready
        }.otherwise{
          issueReg.data(2).asInstanceOf[StoreInstIssue].addrs(0).depend.tag := 0.U
          issueReg.data(2).asInstanceOf[StoreInstIssue].addrs(0).depend.ready := true.B
        }

        issueReg.data(2).asInstanceOf[StoreInstIssue].size := inReg.data(2).asInstanceOf[StoreInstDecode].size
        issueReg.data(2).asInstanceOf[StoreInstIssue].op := inReg.data(2).asInstanceOf[StoreInstDecode].op

        issueReg.op := 3.U
        assignValid := true.B
      } 
    }
  }.otherwise{
    inputStall := true.B
  }

  //io.instructionStream.bits := issueReg.data

  //io.instructionStream.zipWithIndex.foreach{case (element,i) => element.bits.asInstanceOf[element.cloneType] := issueReg.data(i)}

  io.issueStream(0).bits.asInstanceOf[ExecuteInstIssue] := issueReg.data(0).asInstanceOf[ExecuteInstIssue]
  io.issueStream(1).bits.asInstanceOf[LoadInstIssue] := issueReg.data(1).asInstanceOf[LoadInstIssue]
  io.issueStream(2).bits.asInstanceOf[StoreInstIssue] := issueReg.data(2).asInstanceOf[StoreInstIssue]


  /* when(issueReg.op =/= 0.U){
    when(io.issueStream(issueReg.op).ready){
      io.issueStream(issueReg.op).valid := true.B
    }.otherwise{
      stall := true.B
    }
  } */

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
