package ATA8

import chisel3._
import chisel3.util._

class Scratchpad(writeports: Int)(implicit c: Configuration) extends Module {
  val io = IO(new Bundle {
  	//val Readport = Vec(c.bufferReadPorts, Flipped(new ReadportBuf(c.arithDataWidth,10)))
  	//val Writeport = Vec(c.bufferWritePorts, Flipped(new WriteportBuf(c.arithDataWidth,10)))
  	//val Writeport = Vec(writeports, Flipped(Decoupled(new Writeport(Vec(c.grainDim,UInt(c.arithDataWidth.W)),16))))
    //val Readport = Vec(c.bufferReadPorts, Flipped(new Readport(Vec(c.grainDim,UInt(c.arithDataWidth.W)),16)))
    val Writeport = Vec(writeports, Flipped(Decoupled(new Writeport(new Bundle{val writeData = Vec(c.dataBusSize,UInt(8.W)); val strb = Vec(c.dataBusSize, Bool())},16))))
    val Readport = Vec(c.bufferReadPorts, Flipped(new Readport(Vec(c.dataBusSize,UInt(8.W)),16)))
  })

  val memBanks = Seq.fill(c.dataBusSize)(SyncReadMem(c.scratchpadSize, UInt(8.W)))

  io.Writeport.foreach { port =>
    port.ready := true.B
    val writePorts = Wire(Vec(c.dataBusSize, new Writeport(new Bundle{val writeData = UInt(8.W); val en = Bool()}, c.addrWidth - log2Ceil(c.dataBusSize))))

    writePorts.foreach { wp =>
      wp.addr := 0.U // Replace with a sensible default if necessary
      wp.data.writeData := 0.U
      wp.data.en := false.B // Writes disabled by default
    }

    writePorts.zip(memBanks).foreach { case (port, mem) =>
      when(port.data.en) {
        mem.write(port.addr, port.data.writeData)
      }
    }

    val wrAddr = port.bits.addr
    val wrData = port.bits.data.writeData
    val mask = port.bits.data.strb

    // Bank addressing logic
    val bankAddr = wrAddr(c.addrWidth - 1, log2Ceil(c.dataBusSize))
    val bankIdxOffset = wrAddr(log2Ceil(c.dataBusSize) - 1, 0)

    wrData.zipWithIndex.foreach { case (data, i) =>
      val bankIdx = (bankIdxOffset + i.U)(log2Ceil(c.dataBusSize) - 1, 0)
      val isWrapAround = (bankIdxOffset + i.U) >= c.dataBusSize.U
      val effectiveBankAddr = bankAddr + isWrapAround.asUInt

      writePorts(bankIdx).addr := effectiveBankAddr
      writePorts(bankIdx).data.writeData := data

      writePorts(bankIdx).data.en := mask(i) && port.fire
    }
  }

  // Read logic
  io.Readport.foreach { port =>
    port.request.ready := true.B

    val readPorts = Wire(Vec(c.dataBusSize, new ReadportSimple(UInt(8.W),c.addrWidth - log2Ceil(c.dataBusSize))))
    readPorts.foreach { rp =>
      rp.addr := 0.U // Replace with a sensible default if necessary
    }

    readPorts.zip(memBanks).foreach { case (port, mem) =>
      port.readData := mem.read(port.addr, true.B) // Always enabled for simplicity
    }

    val rdAddr = port.request.bits.addr
    val rdData = port.response.bits.readData

    val bankAddr = rdAddr(c.addrWidth - 1, log2Ceil(c.dataBusSize))
    val bankIdxOffset = rdAddr(log2Ceil(c.dataBusSize) - 1, 0)

    rdData.zipWithIndex.foreach {case (data,i) =>
      val bankIdx = (bankIdxOffset + i.U)(log2Ceil(c.dataBusSize) - 1, 0)
      val isWrapAround = (bankIdxOffset + i.U) >= c.dataBusSize.U
      val effectiveBankAddr = bankAddr + isWrapAround.asUInt
      
      readPorts(bankIdx).addr := effectiveBankAddr
      data := readPorts(bankIdx).readData

      port.response.valid := RegNext(port.request.valid)
    }
  }
}
