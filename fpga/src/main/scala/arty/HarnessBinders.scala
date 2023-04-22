package chipyard.fpga.arty

import chisel3._

import freechips.rocketchip.devices.debug.{HasPeripheryDebug, HasPeripheryDebugModuleImp}
import freechips.rocketchip.jtag.{JTAGIO}

import sifive.blocks.devices.uart.{UARTPortIO, HasPeripheryUARTModuleImp}
import sifive.blocks.devices.jtag.{JTAGPins, JTAGPinsFromPort}
import sifive.blocks.devices.pinctrl.{BasePin}

import sifive.blocks.devices.gpio._
import sifive.blocks.devices.spi._
import sifive.blocks.devices.i2c._
import sifive.blocks.devices.pwm._

import sifive.fpgashells.ip.xilinx.{IBUFG, IOBUF, PULLUP, PowerOnResetFPGAOnly}

import chipyard.harness.{ComposeHarnessBinder, OverrideHarnessBinder}
import chipyard.iobinders.JTAGChipIO

class WithArtyResetHarnessBinder extends ComposeHarnessBinder({
  (system: HasPeripheryDebug, th: ArtyFPGATestHarness, ports: Seq[Data]) => {
    val resetPorts = ports.collect { case b: Bool => b }
    require(resetPorts.size == 2)
    withClockAndReset(th.clock_32MHz, th.ck_rst) {
      // Debug module reset
      th.dut_ndreset := resetPorts(0)

      // JTAG reset
      resetPorts(1) := PowerOnResetFPGAOnly(th.clock_32MHz)
    }
  }
})

class WithArtyJTAGHarnessBinder extends OverrideHarnessBinder({
  (system: HasPeripheryDebug, th: ArtyFPGATestHarness, ports: Seq[Data]) => {
    ports.map {
      case j: JTAGChipIO => withClockAndReset(th.buildtopClock, th.hReset) {
        val jtag_wire = Wire(new JTAGIO)
        jtag_wire.TDO.data := j.TDO
        jtag_wire.TDO.driven := true.B
        j.TCK := jtag_wire.TCK
        j.TMS := jtag_wire.TMS
        j.TDI := jtag_wire.TDI

        val io_jtag = Wire(new JTAGPins(() => new BasePin(), false)).suggestName("jtag")

        JTAGPinsFromPort(io_jtag, jtag_wire)

        io_jtag.TCK.i.ival := IBUFG(IOBUF(th.jd_2).asClock).asBool

        IOBUF(th.jd_5, io_jtag.TMS)
        PULLUP(th.jd_5)

        IOBUF(th.jd_4, io_jtag.TDI)
        PULLUP(th.jd_4)

        IOBUF(th.jd_0, io_jtag.TDO)

        // mimic putting a pullup on this line (part of reset vote)
        th.SRST_n := IOBUF(th.jd_6)
        PULLUP(th.jd_6)

        // ignore the po input
        io_jtag.TCK.i.po.map(_ := DontCare)
        io_jtag.TDI.i.po.map(_ := DontCare)
        io_jtag.TMS.i.po.map(_ := DontCare)
        io_jtag.TDO.i.po.map(_ := DontCare)
      }
      case b: Bool =>
    }
  }
})

class WithArtyUARTHarnessBinder extends OverrideHarnessBinder({
  (system: HasPeripheryUARTModuleImp, th: ArtyFPGATestHarness, ports: Seq[UARTPortIO]) => {
    withClockAndReset(th.clock_32MHz, th.ck_rst) {
      IOBUF(th.uart_rxd_out, ports(0).txd)
      ports(0).rxd := IOBUF(th.uart_txd_in)
      
      IOBUF(th.jd_3, ports(1).txd)
      ports(1).rxd := IOBUF(th.jd_7)
    }
  }
})

class WithArtyGPIOHarnessBinder extends OverrideHarnessBinder({
  (system: HasPeripheryGPIOModuleImp, th: ArtyFPGATestHarness, ports: Seq[GPIOPortIO]) => {
    withClockAndReset(th.clock_32MHz, th.ck_rst) {
      IOBUF(th.ja_2, ports(0).pins(0).toBasePin())
      IOBUF(th.ja_3, ports(0).pins(1).toBasePin())
      IOBUF(th.ja_4, ports(0).pins(2).toBasePin())

      
      IOBUF(th.led0_r, ports(1).pins(0).toBasePin())
      IOBUF(th.led0_g, ports(1).pins(1).toBasePin())
      IOBUF(th.led0_b, ports(1).pins(2).toBasePin())
      IOBUF(th.led_0,  ports(1).pins(3).toBasePin())
      IOBUF(th.led1_r, ports(1).pins(4).toBasePin())
      IOBUF(th.led1_g, ports(1).pins(5).toBasePin())
      IOBUF(th.led1_b, ports(1).pins(6).toBasePin())
      IOBUF(th.led_1,  ports(1).pins(7).toBasePin())
      IOBUF(th.led2_r, ports(1).pins(8).toBasePin())
      IOBUF(th.led2_g, ports(1).pins(9).toBasePin())
      IOBUF(th.led2_b, ports(1).pins(10).toBasePin())
      IOBUF(th.led_2,  ports(1).pins(11).toBasePin())
      IOBUF(th.led3_r, ports(1).pins(12).toBasePin())
      IOBUF(th.led3_g, ports(1).pins(13).toBasePin())
      IOBUF(th.led3_b, ports(1).pins(14).toBasePin())
      IOBUF(th.led_3,  ports(1).pins(15).toBasePin())
      IOBUF(th.sw_0,   ports(1).pins(16).toBasePin())
      IOBUF(th.sw_1,   ports(1).pins(17).toBasePin())
      IOBUF(th.sw_2,   ports(1).pins(18).toBasePin())
      IOBUF(th.sw_3,   ports(1).pins(19).toBasePin())
      IOBUF(th.btn_0,  ports(1).pins(20).toBasePin())
      IOBUF(th.btn_1,  ports(1).pins(21).toBasePin())
      IOBUF(th.btn_2,  ports(1).pins(22).toBasePin())
      IOBUF(th.btn_3,  ports(1).pins(23).toBasePin())
    }
  }
})

class WithArtyQSPIHarnessBinder extends OverrideHarnessBinder({
  (system: HasPeripherySPIFlashModuleImp, th: ArtyFPGATestHarness, ports: Seq[SPIPortIO]) => {
    // // connect to on-board SPI Flash
    // th.connectSPIFlash(ports(0), th.clock_32MHz, th.ck_rst)
    withClockAndReset(th.clock_32MHz, th.ck_rst) {
      
      IOBUF(th.qspi_sck, ports(0).sck)
      IOBUF(th.qspi_cs, ports(0).cs(0))
      
      ports(0).dq(0).i := IOBUF(th.qspi_dq(0), ports(0).dq(0).o, true.B)
      ports(0).dq(1).i := IOBUF(th.qspi_dq(1), ports(0).dq(1).o, true.B)
      ports(0).dq(2).i := IOBUF(th.qspi_dq(2), ports(0).dq(2).o, true.B)
      ports(0).dq(3).i := IOBUF(th.qspi_dq(3), ports(0).dq(3).o, true.B)

      // val qspi_pins = Wire(new SPIPins(() => {new BasePin()}, ports(0).c))
      
      // // SPIPinsFromPort(qspi_pins, ports(0), th.clock_32MHz, th.ck_rst, syncStages = ports(0).c.defaultSampleDel)
      // val syncStages = ports(0).c.defaultSampleDel
      // qspi_pins.sck.outputPin(ports(0).sck)

      // (qspi_pins.dq zip ports(0).dq).zipWithIndex.foreach {case ((p, s), i) =>
      //   p.outputPin(s.o, pullup_en=true.B)
      //   p.o.oe := s.oe
      //   p.o.ie := ~s.oe
      //   s.i := SynchronizerShiftReg(p.i.ival, syncStages, name = Some(s"spi_dq_${i}_sync"))
      // }

      // (qspi_pins.cs zip ports(0).cs) foreach { case (c, s) =>
      //   c.outputPin(s)
      // }
      
      // IOBUF(th.qspi_sck, ports(0).sck)
      // IOBUF(th.qspi_cs,  ports(0).cs(0))
      // (th.qspi_dq zip qspi_pins.dq).foreach { case(a, b) => IOBUF(a, b) }
    }
  }
})
