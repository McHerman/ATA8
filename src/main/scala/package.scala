import chisel3._
import chisel3.util.log2Ceil

package object ATA8 {

  case class Configuration(
    scratchpadSize: Int,
    bufferReadPorts: Int,
    bufferWritePorts: Int,
    grainDim: Int,
    sysDim: Int,
    grainFIFOSize: Int,
    grainACCUSize: Int,
    arithDataWidth: Int,
    modeWidth: Int,
    tagCount: Int,
    addrWidth: Int,
    dmaCount: Int,
    issueWidth: Int,
    tagProducers: Int,
    tagRecievers: Int,
    multibleDMA: Boolean/* ,
    initialStateMap: Seq[Int],
    simulation: Boolean = false */
  ){
    val tagWidth =  log2Ceil(tagCount)
    val syncIdWidth = log2Ceil(sysDim)
    val sysWidth = log2Ceil(2)
    //val sysWidth = log2Ceil(sysDim*sysDim) //Gives 0 
    val grainSizeWidth = log2Ceil(grainDim)

    /* val physRegisterIdWidth = log2Ceil(reorderBufferSize).W
    val memIdWidth = log2Ceil(memQueueSize).W
    val snapshotIdWidth = log2Ceil(numOfSnapshots).W */
  }

  object Configuration {
    def default(): Configuration = {
      Configuration(
        512, // scratchpadSize
        3,    // bufferReadPorts
        2,    // bufferWritePorts
        8,    // grainDim
        1,    // sysDim
        64,  // grainFIFOSize
        64,  // grainACCUSize
        8,    // arithDataWidth
        1,    // modeWidth
        8,    // tagCount
        16,   // addrWidth
        4,    // dmaCount
        4,    // issueWidth
        2,    // tagProducers
        3,    // tagRecievers
        false
      )
    }
    def test(): Configuration = {
      Configuration(
        64, // scratchpadSize
        1,    // bufferReadPorts
        1,    // bufferWritePorts
        8,     // grainDim
        1,    // sysDim
        128,  // grainFIFOSize
        128,  // grainACCUSize
        8,    // arithDataWidth
        1,    // modeWidth
        8,    // tagCount
        16,    // addrWidth
				1,    // dmaCount
        1,    // issueWidth
        2,    // tagProducers
        3,    // tagRecievers
        false
      )
    }
    def test16(): Configuration = {
      Configuration(
        128, // scratchpadSize
        1,    // bufferReadPorts
        1,    // bufferWritePorts
        16,   // grainDim
        1,    // sysDim
        128,  // grainFIFOSize
        128,  // grainACCUSize
        8,    // arithDataWidth
        1,    // modeWidth
        8,    // tagCount
        16,    // addrWidth
				4,    // dmaCount
        4,    // issueWidth
        2,    // tagProducers
        3,    // tagRecievers
        false
      )
    }
    def buftest(): Configuration = {
      Configuration(
        128, // scratchpadSize
        1,    // bufferReadPorts
        2,    // bufferWritePorts
        8,   // grainDim
        1,    // sysDim
        128,  // grainFIFOSize
        128,  // grainACCUSize
        8,    // arithDataWidth
        1,    // modeWidth
        8,    // tagCount
        16,    // addrWidth
				4,    // dmaCount
        4,    // issueWidth
        2,    // tagProducers
        3,    // tagRecievers
        false
      )
    }
  }
}
