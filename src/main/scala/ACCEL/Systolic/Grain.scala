package ATA8

import chisel3._
import chisel3.util._
  
class Grain(implicit c: Configuration) extends Module {  
  val io = IO(new Bundle {
    //val State = Input(UInt(1.W))
    //val Size = Input(UInt(8.W))
    val in = Flipped(Decoupled(new SysOP)) 
    //val Memport = Flipped(Decoupled(new Memport_V3(c.arithDataWidth*c.grainDim,10))) // Add actual memport
    //val Memport = Vec(2,Flipped(Decoupled(new Memport(Vec(c.grainDim,UInt(c.arithDataWidth.W)),10))))
    val writePort = Vec(2,Vec(c.grainDim,Flipped(Decoupled(Vec(c.dataBusSize,UInt(8.W))))))
    //val readPort = Flipped(new Readport(Vec(c.grainDim,UInt(c.arithDataWidth.W)),10)) 
    val readPort = Vec(c.grainDim,Flipped(new Readport(Vec(c.dataBusSize,UInt(c.arithDataWidth.W)),0)))
    val completed = Valid(new Bundle{val tag = UInt(c.tagWidth.W)})
  })

  /* val XFile = Module(new XFile())
  val YFile = Module(new YFile())
  val ACCUFile = Module(new ACCUFile()) */

  val xFiles = Seq.fill(c.grainDim)(Module(new XFile())) 
  val yFiles = Seq.fill(c.grainDim)(Module(new YFile())) 
  //val ACCUFile = Seq.fill(c.grainDim)(Module(new ACCUFile())) 
  //val accuFiles = Seq.tabulate(c.grainDim)(i => Module(new ACCUFile(i == 0))) // First module has a delay on the input on activate  
  val accuFiles = Seq.fill(c.grainDim)(Module(new ACCUFile(false))) 


  val SysCtrl = Module(new SysCtrl())

  val array = Seq.fill(c.grainDim, c.grainDim)(Module(new PEArray(c.dataBusSize))) 

  SysCtrl.io.in <> io.in
  io.completed := SysCtrl.io.completed

  /// MEM SIGNALS ///

  (xFiles zip io.writePort(0)).foreach{case (file, port) => 
    file.io.Memport <> port
  }

  (yFiles zip io.writePort(1)).foreach{case (file, port) => 
    file.io.Memport <> port
  }

  (accuFiles zip io.readPort).foreach{case (file,port) => 
    file.io.Readport <> port
  }

  /// CARRY SIGNALS ///

  xFiles.head.io.Activate := SysCtrl.io.Activate
  accuFiles.head.io.Activate := xFiles.last.io.ActivateOut

  yFiles.head.io.Activate := SysCtrl.io.Activate
  yFiles.head.io.Enable := SysCtrl.io.Enable

  if(c.grainDim != 1){
    xFiles.sliding(2).foreach{case Seq(producer, reciever) => 
      reciever.io.Activate := producer.io.ActivateOut
    }

    accuFiles.sliding(2).foreach{case Seq(producer, reciever) => 
      reciever.io.Activate := producer.io.ActivateOut
    }

    yFiles.sliding(2).foreach{case Seq(producer, reciever) => 
      reciever.io.Activate := producer.io.ActivateOut
      reciever.io.Enable := producer.io.EnableOut
    }
  }

  /// GLOBAL SIGNALS ///

  yFiles.foreach{file => 
    file.io.State := SysCtrl.io.Mode
    file.io.Shift := SysCtrl.io.Shift
  }

  accuFiles.foreach{file => 
    file.io.State := SysCtrl.io.Mode
    file.io.Shift := SysCtrl.io.Shift
  }

  SysCtrl.io.activateLoopBack := accuFiles.last.io.ActivateOut

  /// SIZE SIGNALS /// 

  (xFiles zip SysCtrl.io.sizes).foreach{case (file,size) => 
    file.io.size := size
  }

  (yFiles zip SysCtrl.io.sizes).foreach{case (file,size) => 
    file.io.size := size
  }

  (accuFiles zip SysCtrl.io.sizes).foreach{case (file,size) => 
    file.io.size := size
  }

  /// ARRAY CONNECTIONS /// 

  (array.head zip xFiles).foreach{case (row,file) =>
    row.io.x := file.io.Out
  }

  (array.transpose.head zip yFiles).foreach{case (collum,yFile) =>
    collum.io.y := yFile.io.Out
  }

  (array.transpose.last zip accuFiles).foreach{case (collum,accuFile) =>
    accuFile.io.In := collum.io.yOut
  }



  array.zipWithIndex.foreach { case (row, i) =>
    row.zipWithIndex.foreach { case (pe, k) =>
      if (i != c.grainDim - 1) {
        pe.io.xOut <> array(i + 1)(k).io.x
      }
      if (k != c.grainDim - 1) {
        pe.io.yOut <> array(i)(k + 1).io.y
      }
    }
  }
}

