package ATA8

import chisel3._
import chisel3.util._
import PENIS._

object PENIS {
	class allocData(implicit c: Configuration) extends Bundle{
    val issue = new ExecuteInst
		val issueReady = Output(Bool())
		val age = Output(UInt(5.W))
	}

  class ElementPort(implicit c: Configuration) extends Bundle{
    val in = Flipped(Decoupled(new ExecuteInst))
		val allocData = Decoupled(new allocData)
    val event = Flipped(Valid(new Event))
  }

  def weightCalc(allocData: allocData, psuedoFill: UInt, avg: UInt)(implicit c: Configuration): (UInt, Bool) = { // TODO: fix this shit 
    val weight = WireDefault (0.U)
    val valid = WireDefault (false.B)

		valid := allocData.issueReady

    if(c.sysDim == 1){
      weight := allocData.age + allocData.issue.size
    }else{ //FIXME: fix this balancing shit
      when(allocData.issue.grainSize === 1.U){
        weight := allocData.age + (avg - (psuedoFill + allocData.issue.size))			
      }.otherwise{
        weight := allocData.issue.grainSize + allocData.age
      }
    }

		when(allocData.issue.grainSize === 1.U){
			weight := allocData.age + (avg - (psuedoFill + allocData.issue.size))			
		}.otherwise{
			weight := allocData.issue.grainSize
		}
		
    (weight,valid)
  }

  def findMax(WeighVec: Seq[(UInt, Bool)]): (UInt,UInt,Bool) = { // TODO: fix this shit 
    val maxVal = WireDefault (0.U)
    val maxIdx = WireDefault (0.U)
    val maxReady = WireDefault (false.B)

		val scalaVector = WeighVec.zipWithIndex
    .map { case ((weight, ready), i) => MixedVecInit (weight, ready, i.U(8.W)) }
    val resFun2 = VecInit ( scalaVector )
    . reduceTree ((x, y) => Mux(x(0) >= y(0) && x(1).asBool, x, y))
		
    maxVal := resFun2 (0)
    maxIdx := resFun2 (2)
    maxReady := resFun2 (1)
		
    (maxVal,maxIdx,maxReady)
  }
  
  def issueMax(elements: Vec[allocData], psuedoFill: UInt, avg: UInt)(implicit c: Configuration): (UInt, UInt, Bool) = {
    val WeighVec: Seq[(UInt, Bool)] = elements.map(q => weightCalc(q, psuedoFill, avg))
    //val WeighVec: Seq[(UInt, Bool)] = Seq.tabulate(){i => weightCalc(allocDataVec(i), psuedoFill, avg)}
    
    val (maxVal,maxIdx,maxReady) = findMax(WeighVec)
    (maxVal,maxIdx,maxReady)
  }

  def countConsecutiveOnes(vec: Vec[Bool]): UInt = {
    val onesCount = RegInit(0.U(4.W)) // Initialize to 0
    when(vec.reduce(_ && _)) {        // Check if all bits are 1
      onesCount := onesCount + 1.U
    }.otherwise {
      onesCount := 0.U
    }
    onesCount
  }
}

class IssueQueue(implicit c: Configuration) extends Module {
//class IssueQueue(config: Configuration) extends Module {
  //implicit val c = config

  val positions = c.issueQueueSize
  val w = c.tagWidth

  val PosIndex = log2Ceil(positions).W

  val io = IO(new Bundle {
    val alloc = Flipped(Decoupled(new ExecuteInst))
		//val issue = Vec(c.sysDim, Decoupled(new IssuePackage))
    val issue = Vec(c.sysDim, Decoupled(new ExecuteInst)) // TODO: Fix this so its sysDim * sysDim also change to IssuePackage //FIXME: Make this the issuepackage type
    val event = Flipped(Valid(new Event))
  })


  io.issue.foreach(_.valid := false.B)
	io.issue.foreach(_.bits := DontCare)


  // Instansiate issue queue

  val QueueVec = Seq.fill(positions)(Module(new IssueElement())) // Create a 2D array of PE modules
  val issueIO = VecInit(QueueVec.map(_.io.Port))
 
  issueIO.foreach { IssueElement =>
    IssueElement.event <> io.event
    IssueElement.in.bits <> io.alloc.bits
    IssueElement.in.valid := false.B
    IssueElement.allocData.ready := false.B
  }
  
  //io.alloc.ready := VecInit(Seq.tabulate(positions)(n => QueueVec(n).io.Port.in.ready)).reduceTree(_ | _)

  val readyVec = VecInit(QueueVec.map(_.io.Port.in.ready))
  io.alloc.ready := readyVec.reduceTree(_ | _)

  val firstReadyIdx = PriorityEncoder(readyVec)
  QueueVec.zipWithIndex.foreach { case (element, i) => element.io.Port.in.valid := (firstReadyIdx === i.U) && readyVec(firstReadyIdx) }


  // TODO: build code to allow for multigrain shit, find the largest n x n matrix 

  val resAvail = VecInit(io.issue.map(_.ready))

  val psuedoFill = Reg(Vec(c.sysDim,UInt(16.W))) // Indicates the aproximate computation lenght of the given queue
  val avg = Wire(UInt(16.W))
  avg := psuedoFill.reduceTree(_ +& _) >> (c.sysDim - 1) // Computes the average of the current 

  //////

  val allocDataVec = VecInit(issueIO.map(_.allocData.bits))

  val resultSeq = Seq.tabulate(c.sysDim){i => issueMax(allocDataVec, psuedoFill(i), avg)} // Finds the element with highest weight for each res
  val modifiedSeq: Seq[(UInt, Bool)] = resultSeq.map { case (uint1, _, boolValue) => (uint1, boolValue) } // Removes index 
  val (maxVal, resIndex, maxReady) = findMax(modifiedSeq) // Finds highest total weight

  val scalaVector = resultSeq.map { case (value, index, valid) => MixedVecInit (value, index, valid)} // Makes chiselVec out of the highest weights of each res
  val resultVec = VecInit(scalaVector)

  val elementIndex = Wire(UInt(PosIndex)) 
  elementIndex := resultVec(resIndex)(1) // Finds the index of the highest weight 

  io.issue(resIndex).bits <> issueIO(elementIndex).allocData.bits.issue
  
  when(io.issue(resIndex).ready && maxReady.asBool){ 
    issueIO(elementIndex).allocData.ready := true.B
    io.issue(resIndex).valid := true.B
    psuedoFill(resIndex) := psuedoFill(resIndex) + issueIO(elementIndex).allocData.bits.issue.size
  } 

  

  /*

  val allocDataVec = VecInit(issueIO.map(_.allocData.bits))

  val (maxVal, resIndex, maxReady) = issueMax(allocDataVec, psuedoFill(0), avg) // Finds highest total weight

  //QueueVec.zipWithIndex.collectFirst { case (element, _) if element.io.Port.in.ready === true.B => element.io.Port.in.valid := true.B }
  
 

  io.issue(0).bits <> issueIO(resIndex).allocData.bits.issue

  when(io.issue(0).ready && maxReady.asBool){ 
    issueIO(resIndex).allocData.ready := true.B
    io.issue(0).valid := true.B
    psuedoFill(0) := psuedoFill(0) + issueIO(resIndex).allocData.bits.issue.size
  } 

  */


}

class IssueElement()(implicit c: Configuration) extends Module{
  val io = IO(new Bundle {
    val Port = new ElementPort()
  })

  io.Port.allocData.valid := false.B
  io.Port.in.ready := false.B

  io.Port.allocData.bits.issueReady := false.B
  io.Port.allocData.bits.age := 0.U


  val valueReg = Reg(new ExecuteInst())

  val emptyReg = RegInit(1.B)
  val AgeReg = RegInit(0.U(8.W))

  when(io.Port.event.valid && !emptyReg){
    when(io.Port.event.bits.tag === valueReg.ids(0).tag){
      valueReg.ids(0).ready := true.B
    }

    when(io.Port.event.bits.tag === valueReg.ids(1).tag){
      valueReg.ids(1).ready := true.B
    }
  }

  when(valueReg.ids(0).ready && valueReg.ids(1).ready){
    io.Port.allocData.bits.issueReady := true.B
    io.Port.allocData.bits.age := AgeReg

    when(io.Port.allocData.ready){
      io.Port.allocData.valid := true.B
      valueReg.ids(0).ready := false.B
      valueReg.ids(1).ready := false.B

      emptyReg := true.B
      AgeReg := 0.U
    } 
  }

  when(!emptyReg){
    AgeReg := AgeReg + 1.U
  }.otherwise{
    io.Port.in.ready := true.B

    when(io.Port.in.valid){
      valueReg := io.Port.in.bits
      emptyReg := false.B
      AgeReg := 1.U
    }
  }

  io.Port.allocData.bits.issue := valueReg

}

/* object IssueQueue extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new IssueQueue(Configuration.default()))
} */
