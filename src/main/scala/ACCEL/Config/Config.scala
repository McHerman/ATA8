package ATA8

import chisel3._
import chisel3.util._
//import chisel3.util.experimental.BoringUtils
import scala.io.Source
import play.api.libs.json._



class Config(implicit c: Configuration) extends Module {
  val registersCount = 33

  val io = IO(new Bundle {
    val axi_s0 = Flipped(new CustomAXI4Lite(32, 32))
    val loadDebug = Flipped(new LoadDebug)
    val exeDebug = Flipped(new ExeDebug)
    val storeDebug = Flipped(new StoreDebug)
    val frontendDebug = Flipped(Valid(UInt(64.W)))
    val event = Vec(2,Flipped(Valid(new Event()))) 
    val robDebug = Input(Vec(c.tagCount,new mapping()))
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
  regs(2) := io.storeDebug.state
  regs(3) := 16.U

  when(io.frontendDebug.valid){
    regs(4) := io.frontendDebug.bits
    regs(5) := regs(4)
    regs(6) := regs(5)
    regs(7) := regs(6)
  }

  when(io.event(0).valid){
    regs(8) := io.event(0).bits.tag
    regs(9) := regs(8)
    regs(10) := regs(9)
    regs(11) := regs(10)
  }.elsewhen(io.event(1).valid){
    regs(8) := io.event(1).bits.tag
    regs(9) := regs(8)
    regs(10) := regs(9)
    regs(11) := regs(10)
  }

  io.robDebug.zipWithIndex.foreach{case (id,i) => 
    regs(16+i) := Cat(id.valid, id.ready, id.addr)
  }

  regs(32) := 31.U




  //BoringUtils.addSink(regs(0), "loadcontrollerstate")
  //BoringUtils.addSink(regs(1), "syscontrollerstate")
  //BoringUtils.addSink(regs(2), "storecontrollerstate")

  slaveInterface.io.bus.dataIn := regs(slaveInterface.io.bus.addr)
}
