import java.io.{BufferedReader, FileReader, FileWriter, PrintWriter}

object Assembler {
  def main(args: Array[String]): Unit = {
    val reader = new BufferedReader(new FileReader("testProgram.txt"))
    val binaryStringWriter = new PrintWriter(new FileWriter("binaryStringOutput.txt"))
    val int64Writer = new PrintWriter(new FileWriter("int64Output.txt"))

    var line = ""
    while ({line = reader.readLine(); line != null}) {
      val tokens = line.split(" ")
      val op = tokens(0)
      val binaryInstruction = encodeInstruction(op, tokens.slice(1, tokens.length))

      binaryStringWriter.println(binaryInstruction)
      val int64Instruction = java.lang.Long.parseLong(binaryInstruction, 2)
      int64Writer.println(int64Instruction)
    }

    reader.close()
    binaryStringWriter.close()
    int64Writer.close()
  }

  def encodeInstruction(op: String, tokens: Array[String]): String = {
    def decToBinary(dec: Int, bits: Int): String = String.format(s"%${bits}s", dec.toBinaryString).replace(' ', '0')

    val binaryInstruction = op.toLowerCase match {
      case "gemm" =>
        val addrd = decToBinary(tokens(3).toInt, 16)
        val addrs2 = decToBinary(tokens(2).toInt, 16)
        val addrs1 = decToBinary(tokens(1).toInt, 16)
        val size = decToBinary(tokens(0).toInt, 8)
        val func = decToBinary(1, 4)
        addrd + addrs2 + addrs1 + size + "0" + "0" + "00" + func

      case "load" =>
        val addr = decToBinary(tokens(1).toInt, 32)
        val size = decToBinary(tokens(0).toInt, 8)
        val func = decToBinary(2, 4)
        addr + size + "000000000000000000" + "0" + "0" + func

      case "store" =>
        val addr = decToBinary(tokens(1).toInt, 32)
        val size = decToBinary(tokens(0).toInt, 8)
        val func = decToBinary(3, 4)
        addr + size + "0000000000000000000" + "0" + func

      case _ =>
        println(s"Unknown instruction: $op")
        ""
    }

    binaryInstruction
  }
}
