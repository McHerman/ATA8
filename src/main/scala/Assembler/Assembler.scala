import java.io.{BufferedWriter, FileWriter, PrintWriter}
import scala.io.Source

object Assembler {

  def padZeros(value: String, length: Int): String = {
    "0" * (length - value.length) + value
  }

  def decToBinary(value: Int, length: Int): String = {
    padZeros(value.toBinaryString, length)
  }

  def main(args: Array[String]): Unit = {
    if (args.length != 1) {
      println("Usage: Assembler <input file>")
      return
    }

    val inputFile = args(0)
    val baseFileName = inputFile.takeWhile(_ != '.')
    val outputBinaryFile = baseFileName + "_Out_bin.txt"
    val outputIntFile = baseFileName + "_Out_int.txt"

    val source = Source.fromFile(inputFile)
    val outBinary = new PrintWriter(new BufferedWriter(new FileWriter(outputBinaryFile)))
    val outInt = new PrintWriter(new BufferedWriter(new FileWriter(outputIntFile)))
    
    for (line <- source.getLines()) {
      val tokens = line.split(" ").map(_.replace(",", ""))
      val op = tokens(0)
      val binaryInstruction: String = op.toLowerCase match {
        case "gemm" =>
          val size = decToBinary(tokens(1).toInt, 8)
          val addrs1 = decToBinary(tokens(2).toInt, 16)
          val addrs2 = decToBinary(tokens(3).toInt, 16)
          val addrd = decToBinary(tokens(4).toInt, 16)
          val func = decToBinary(1, 4) // func is 1 for execute
          addrd + addrs2 + addrs1 + size + padZeros("0", 1) + padZeros("0", 2) + padZeros("0", 1) + func
        
        case "load" =>
          val size = decToBinary(tokens(1).toInt, 8)
          val addr = decToBinary(tokens(2).toInt, 32)
          val func = decToBinary(2, 4) // func is 2 for load
          addr + size + padZeros("0", 18) + padZeros("0", 1) + padZeros("0", 1) + func
        
        case "store" =>
          val size = decToBinary(tokens(1).toInt, 8)
          val addr = decToBinary(tokens(2).toInt, 32)
          val func = decToBinary(3, 4) // func is 3 for store
          addr + size + padZeros("0", 19) + padZeros("0", 1) + func

        case _ =>
          println(s"Unknown instruction: $op")
          "" // Return an empty string
      }

      if (binaryInstruction.nonEmpty) {
        outBinary.println(binaryInstruction)
        outInt.println(java.lang.Long.parseLong(binaryInstruction, 2))
      } else {
        println(s"Empty or invalid binary instruction for line: $line")
      }
    }

    source.close()
    outBinary.close()
    outInt.close()
  }
}