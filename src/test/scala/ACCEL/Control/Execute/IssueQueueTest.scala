package ATA8

import chisel3._
import chiseltest._
import org.scalatest.FlatSpec
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.experimental.ChiselEnum

import scala.util.Random


class IssueQueueTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "IssueQueue"

  //val n = 2 + Random.nextInt(30)
  val n = 8


  implicit val Config = Configuration.test()

  it should "allocate n spaces sequentially" in {
    //val n = 2 + Random.nextInt(30)
    //val n = 16

    test(new IssueQueue()).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>
      for(tag <- 0 until n) {

        dut.io.alloc.ready.expect(1.B)

        dut.io.alloc.bits.grainSize.poke(1.U)
        dut.io.alloc.bits.size.poke(7.U)

        dut.io.alloc.bits.idd.poke(tag.U)
        

        dut.io.alloc.bits.ids(0).ready.poke(false.B)
        dut.io.alloc.bits.ids(1).ready.poke(false.B)

        dut.io.alloc.bits.ids(0).tag.poke(tag.U)
        dut.io.alloc.bits.ids(1).tag.poke((tag+1).U)

        dut.io.alloc.valid.poke(1.B)
        
        dut.clock.step()
        //println(tag)
      }
      dut.io.alloc.ready.expect(0.B)
    }
  }

  

  it should "deallocate n spaces sequentially" in {

    test(new IssueQueue()).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>
      for(tag <- 0 until n) {

        /*
        dut.io.alloc.tag.expect(tag)
        dut.io.alloc.offer.expect(1.B)

        */
        dut.io.alloc.ready.expect(true.B)
        dut.io.alloc.bits.idd.poke(tag.U)

        dut.io.alloc.bits.ids(0).ready.poke(false.B)
        dut.io.alloc.bits.ids(1).ready.poke(false.B)

        dut.io.alloc.bits.ids(0).tag.poke(tag.U)
        dut.io.alloc.bits.ids(1).tag.poke((tag+1).U)

        dut.io.alloc.valid.poke(true.B)
        
        dut.clock.step()
        //println(tag)
      }

      dut.io.alloc.bits.idd.poke(0.U)
      dut.io.alloc.bits.ids(0).tag.poke(0.U)
      dut.io.alloc.bits.ids(1).tag.poke(0.U)






      
      dut.io.alloc.valid.poke(false.B)
      dut.io.alloc.ready.expect(false.B)

      //dut.io.Event.poke(true.B)
      //dut.io.EventTag.poke(0.U)

      dut.io.event.valid.poke(true.B)
      //dut.io.event.bits.eventType.poke(CompletionWithValue)
      dut.io.event.bits.tag.poke(0.U)
      //dut.io.event.bits.writeBackValue.poke(0.U)

      dut.io.issue(0).ready.poke(true.B)
      dut.io.issue(0).valid.expect(false.B)

      //println("1")


      dut.clock.step()

      
      dut.io.event.valid.poke(true.B)
      //dut.io.event.bits.eventType.poke(CompletionWithValue)
      dut.io.event.bits.tag.poke(1.U)
      //dut.io.event.bits.writeBackValue.poke(0.U)

      dut.io.issue(0).ready.poke(true.B)
      dut.io.issue(0).valid.expect(false.B)

      

      //println("2")


      dut.clock.step()

      // Dumb shit 

      dut.io.event.valid.poke(true.B)
      //dut.io.event.bits.eventType.poke(CompletionWithValue)
      dut.io.event.bits.tag.poke(2.U)
      //dut.io.event.bits.writeBackValue.poke(0.U)
      dut.io.issue(0).ready.poke(true.B)
      dut.io.issue(0).valid.expect(true.B)
      dut.io.issue(0).bits.idd.expect(0.U)

      //dut.clock.step(10)

      dut.clock.step(1)


      for(tag <- 1 until n){

        /*

        dut.io.Event.poke(true.B)
        dut.io.EventTag.poke((tag + 2).U)

        */

        dut.io.event.valid.poke(true.B)
        //dut.io.event.bits.eventType.poke(CompletionWithValue)
        dut.io.event.bits.tag.poke((tag+2).U)
        //dut.io.event.bits.writeBackValue.poke(0.U)


        dut.io.issue(0).ready.poke(true.B)
        dut.io.issue(0).valid.expect(true.B)

        dut.io.issue(0).bits.idd.expect(tag.U)

        dut.clock.step()
      }

      dut.io.issue(0).ready.poke(true.B)
      dut.io.issue(0).valid.expect(false.B)

      dut.clock.step(16)

    }

  }
  
}
