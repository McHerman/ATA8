package ATA8

import chisel3._
import chisel3.util._
//import chisel3.util.experimental.BoringUtils
import scala.io.Source
import play.api.libs.json._



class Config(implicit c: Configuration) extends Module {
  val registersCount = 38

  val io = IO(new Bundle {
    val axi_s0 = Flipped(new CustomAXI4Lite(32, 32))
    val loadDebug = Flipped(new LoadDebug)
    val exeDebug = Flipped(new ExeDebug)
    val storeDebug = Flipped(new StoreDebug)
    val receiverDebug = Flipped(Valid(UInt(64.W)))
    val decodeDebug = Flipped(Valid(new LoadInst))
    val decodeOutLoad = Flipped(Valid(new LoadInstIssue))
    val event = Vec(2,Flipped(Valid(new Event()))) 
    val robDebug = Input(Vec(c.tagCount,new mapping()))
    val frontEndDebug = Input(new Bundle{val decodeReady = Bool(); val ROBFetchReady = Bool(); val exeOutReady = Bool(); val loadOutReady = Bool(); val storeOutReady = Bool()})
    val AXIDebug = Input(new Bundle{val data_ready = Bool(); val data_valid = Bool(); val inst_ready = Bool(); val inst_valid = Bool(); val out_ready = Bool(); val out_valid = Bool()})
  })

  val regs = RegInit(VecInit(Seq.fill(registersCount)(0.U(32.W))))
  val slaveInterface = Module(new AXI4LiteCSR(32, 32))

  slaveInterface.io.ctl <> io.axi_s0

  when(slaveInterface.io.bus.write) {
    regs(slaveInterface.io.bus.addr) := slaveInterface.io.bus.dataOut
  }

  // Read the configuration from a JSON file
  /* val configFile = "/home/karlhk/PCIE/ATA8/config.json" // Adjust the path to your JSON file
  val jsonConfig = Json.parse(Source.fromFile(configFile).mkString)

  // Parse the JSON to get the state names and their corresponding addresses
  val stateMappings = jsonConfig.as[Map[String, Int]]

  // Add BoringUtils sources based on the configuration
  stateMappings.foreach { case (stateName, address) =>
    BoringUtils.addSink(regs(address), stateName)
  } */

  regs(0) := io.loadDebug.state
  regs(1) := io.exeDebug.state
  regs(2) := io.storeDebug.asUInt
  regs(3) := io.AXIDebug.asUInt

  when(io.receiverDebug.valid){
    regs(4) := io.receiverDebug.bits
    regs(5) := regs(4)
    regs(6) := regs(5)
    regs(7) := regs(6)
  }

  when(io.decodeDebug.valid){
    regs(8) := io.decodeDebug.bits.asUInt
    regs(9) := regs(8)
    regs(10) := regs(9)
    regs(11) := regs(10)
  }

  when(io.decodeOutLoad.valid){
    regs(12) := io.decodeOutLoad.bits.asUInt
    regs(13) := regs(12)
    regs(14) := regs(13)
    regs(15) := regs(14)
  }

  when(io.event(0).valid){

    when(io.event(1).valid){
      regs(16) := Cat(io.event(1).bits.tag, 1.U(1.W))
      regs(17) := Cat(io.event(0).bits.tag, 1.U(1.W))
      regs(18) := regs(16)
      regs(19) := regs(17)
    }.otherwise{
      regs(16) := Cat(io.event(0).bits.tag, 1.U(1.W))
      regs(17) := regs(16)
      regs(18) := regs(17)
      regs(19) := regs(18)
    }

  }.elsewhen(io.event(1).valid){
    regs(16) := Cat(io.event(1).bits.tag, 1.U(1.W))
    regs(17) := regs(16)
    regs(18) := regs(17)
    regs(19) := regs(18)
  }

  io.robDebug.zipWithIndex.foreach{case (id,i) => 
    regs(20+i) := Cat(id.valid, id.ready, id.addr)
  } 

  regs(36) := io.frontEndDebug.asUInt







  //BoringUtils.addSink(regs(0), "loadcontrollerstate")
  //BoringUtils.addSink(regs(1), "syscontrollerstate")
  //BoringUtils.addSink(regs(2), "storecontrollerstate")

  slaveInterface.io.bus.dataIn := regs(slaveInterface.io.bus.addr)
}
