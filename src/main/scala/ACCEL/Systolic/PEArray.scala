package ATA8

import chisel3._
import chisel3.util._
  
class PEArray(val size: Int)(implicit c: Configuration) extends Module {  
  val io = IO(new Bundle {
    val x = Input(Vec(size, new PEX(c.arithDataWidth)))
    val xOut = Output(Vec(size, new PEX(c.arithDataWidth)))
    val y = Input(Vec(size, new PEY(c.arithDataWidth,1)))
    val yOut = Output(Vec(size, new PEY(c.arithDataWidth,1)))
    val ctrl = Input(new Bundle{val state = UInt(1.W); val shift = Bool()})
  })

  val peArray = Seq.fill(size, size)(Module(new PE())) // Create a 2D matrix of PE modules

  // Interconnect PE modules with conditional logic to handle edges
  peArray.zipWithIndex.foreach { case (row, i) =>
    row.zipWithIndex.foreach { case (pe, k) =>
      pe.io.ctrl <> io.ctrl

      if (i != size - 1) {
        pe.io.xOut <> peArray(i + 1)(k).io.x
      }
      if (k != size - 1) {
        pe.io.yOut <> peArray(i)(k + 1).io.y
      }
    }
  }

  (peArray.head zip io.x).foreach{case (pe,port) => 
    pe.io.x <> port
  }

  (peArray.last zip io.xOut).foreach{case (pe,port) => 
    pe.io.xOut <> port
  }

  (peArray.transpose.head zip io.y).foreach{case (pe,port) => 
    pe.io.y <> port
  }

  (peArray.transpose.last zip io.yOut).foreach{case (pe,port) => 
    pe.io.yOut <> port
  }
}