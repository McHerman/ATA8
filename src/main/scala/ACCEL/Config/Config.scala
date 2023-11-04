package ATA8

import chisel3._
import chisel3.util._
import chisel3.util.experimental.BoringUtils
import scala.io.Source
import play.api.libs.json._



class Config(implicit c: Configuration) extends Module {
  val registersCount = 8

  val io = IO(new Bundle {
    val axi_s0 = Flipped(new CustomAXI4Lite(32, 32))
    //val loadDebug = Flipped(new LoadDebug)
    //val exeDebug = Flipped(new ExeDebug)
    //val storeDebug = Flipped(new StoreDebug)
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

  /* regs(0) := io.loadDebug.state
  regs(1) := io.exeDebug.state
  regs(2) := io.storeDebug.state */

  BoringUtils.addSink(regs(0), "loadcontrollerstate")
  BoringUtils.addSink(regs(1), "syscontrollerstate")
  BoringUtils.addSink(regs(2), "storecontrollerstate")

  slaveInterface.io.bus.dataIn := regs(slaveInterface.io.bus.addr)
}
