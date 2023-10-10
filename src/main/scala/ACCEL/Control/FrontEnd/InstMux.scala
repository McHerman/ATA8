package ATA8

import chisel3._
import chisel3.experimental._
import chisel3.util._

class InstMux(implicit c: Configuration) extends Module {
  val io = FlatIO(new Bundle {
    val in = Flipped(Decoupled(new InstructionPackage))
    val ex = Decoupled(new InstructionPackage)
    val load = Decoupled(new InstructionPackage)
    val store = Decoupled(new InstructionPackage)
  })

	switch(io.in.bits.instruction(1,0)){
		is(0.U){
			
		}
		is(1.U){
			io.in <> io.ex			
		}
		is(2.U){	
			io.in <> io.load
		}
		is(3.U){
			io.in <> io.store
		}
	}
  

}

