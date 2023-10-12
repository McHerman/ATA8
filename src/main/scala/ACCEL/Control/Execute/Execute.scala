package ATA8

import chisel3._
import chisel3.util._



//class ResStation(implicit c: Configuration) extends Module {
class Execute(config: Configuration) extends Module {
  implicit val c = config

  val io = IO(new Bundle {
    val instructionStream = Flipped(Decoupled(new InstructionPackage))
    val event = Flipped(Valid(new Event()))
    val tagFetch = Vec(2,Flipped(new TagRead())) // Incoming fetch 
    val tagFetchfromLoad = Vec(2,new TagRead())  // Outgoing fetch 
    val tagDealloc = Flipped(Decoupled(UInt(c.tagWidth.W)))
    val DMARead = Vec(c.sysDim, Decoupled(new ExecuteInst()))
    val readAddr = Vec(2 /*FIXME: magic fucking number*/ ,Flipped(new Readport(UInt(c.addrWidth.W), c.tagWidth)))
  })

  val Decode = Module(new ExeDecode(c))
  val TagMap = Module(new TagMap(c.exTagCount,4,0))
  val IssueQueue = Module(new IssueQueue())
  //val ResStations = Seq.fill(c.sysDim)(Module(new ResStation())) // Queues for indidual sys Grains 

  Decode.io.instructionStream <> io.instructionStream
  Decode.io.tagFetch.zipWithIndex.foreach{case (element,i) => element <> TagMap.io.ReadData(i)} 

  Decode.io.tagFetch(0) <> TagMap.io.ReadData(0)
  Decode.io.tagFetch(1) <> TagMap.io.ReadData(1)
  io.tagFetchfromLoad <> Decode.io.tagFetchfromLoad

  io.tagFetch(0) <> TagMap.io.ReadData(2)
  io.tagFetch(1) <> TagMap.io.ReadData(3)

  Decode.io.tagRegister <> TagMap.io.Writeport

  TagMap.io.tagDealloc <> io.tagDealloc
  TagMap.io.event <> io.event
  TagMap.io.readAddr <> io.readAddr

  io.tagFetch.zipWithIndex.foreach{case (element,i) => element <> TagMap.io.ReadData(i+2)} 

  IssueQueue.io.alloc <> Decode.io.issueStream
  IssueQueue.io.event := io.event

  //ResStations.zipWithIndex.foreach{case (queue,i) => queue.io.WriteData <> IssueQueue.io.issue(i)} 
  //io.DMARead.zipWithIndex.foreach{case (element,i) => element <> ResStations(i).io.ReadData} 

  io.DMARead.zipWithIndex.foreach{case (element,i) => element <> IssueQueue.io.issue(i)} 

}

object Execute extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new Execute(Configuration.default()))
}
