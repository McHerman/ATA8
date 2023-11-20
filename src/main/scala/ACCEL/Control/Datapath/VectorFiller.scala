import chisel3._
import chisel3.util.log2Ceil

object VectorFillerFunctions {
  // Helper function to truncate values to y
  private def truncate(value: UInt, y: Int): UInt = Mux(value > y.U, y.U, value)

  // Recursive function to build the binary tree and calculate the vector
  def buildTree(n: UInt, x: Int, y: Int): Vec[UInt] = {
    def recurse(currentValue: UInt, subtractor: UInt, depth: Int, maxDepth: Int): Vec[UInt] = {
      if (depth == maxDepth) {
        VecInit(Seq(truncate(currentValue, y)))
      } else {
        val nextSubtractor = subtractor >> 1
        val withSubtraction = recurse(Mux(currentValue >= subtractor, currentValue - subtractor, 0.U), nextSubtractor, depth + 1, maxDepth)
        val withoutSubtraction = recurse(currentValue, nextSubtractor, depth + 1, maxDepth)
        
        VecInit(withoutSubtraction ++ withSubtraction)
      }
    }

    val initialSubtractor = ((x * y) / 2).U
    recurse(n, initialSubtractor, 0, log2Ceil(x))
  }
}

class VectorFiller(val x: Int, val y: Int) extends Module {  
  val io = IO(new Bundle {
    val n = Input(UInt((log2Ceil(x * y) + 1).W))
    val vector = Output(Vec(x, UInt(5.W)))
  })

  // Use the function to build the tree and get the vector
  io.vector := VectorFillerFunctions.buildTree(io.n, x, y)
}