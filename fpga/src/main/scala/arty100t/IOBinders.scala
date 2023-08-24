package chipyard.fpga.arty100t

import chisel3._
import chisel3.util._

import freechips.rocketchip.devices.debug.{HasPeripheryDebug}

import sifive.blocks.devices.gpio._
import sifive.blocks.devices.uart._
import sifive.blocks.devices.spi._
import sifive.blocks.devices.i2c._
import sifive.blocks.devices.pwm._
import testchipip.{CanHavePeripheryCustomBootPin}

import chipyard.iobinders.{ComposeIOBinder, OverrideIOBinder}

class WithDebugResetPassthrough extends ComposeIOBinder({
  (system: HasPeripheryDebug) => {
    // Debug module reset
    val io_ndreset: Bool = IO(Output(Bool())).suggestName("ndreset")
    io_ndreset := system.debug.get.ndreset

    // JTAG reset
    // val sjtag = system.debug.get.systemjtag.get
    // val io_sjtag_reset: Bool = IO(Input(Bool())).suggestName("sjtag_reset")
    // sjtag.reset := io_sjtag_reset

    // (Seq(io_ndreset, io_sjtag_reset), Nil)
    (Seq(io_ndreset), Nil)
  }
})

class WithArtyCustomBootPassthrough extends OverrideIOBinder({
  (system: CanHavePeripheryCustomBootPin) => system.custom_boot_pin.map({p =>
    val name = s"bootsel_0"
    val port = IO(Input(Bool())).suggestName(name)
    port <> p.getWrappedValue
    (Seq(port), Nil)
  }).getOrElse((Nil, Nil))
})

class WithUARTPassthrough extends OverrideIOBinder({
  (system: HasPeripheryUARTModuleImp) => {
    val (ports: Seq[UARTPortIO]) = system.uart.zipWithIndex.map({ case (s, i) =>
      val name = s"uart_${i}"
      val port = IO(new UARTPortIO(s.c)).suggestName(name)
      port <> s
      port
    })
    (ports, Nil)
  }
})

class WithGPIOPassthrough extends OverrideIOBinder({
  (system: HasPeripheryGPIOModuleImp) => {
    val (ports: Seq[GPIOPortIO]) = system.gpio.zipWithIndex.map({ case (s, i) =>
      val name = s"gpio_${i}"
      val port = IO(new GPIOPortIO(s.c)).suggestName(name)
      port <> s
      port
    })
    (ports, Nil)
  }
})

class WithQSPIPassthrough extends OverrideIOBinder({
  (system: HasPeripherySPIFlashModuleImp) => {
    val (ports: Seq[SPIPortIO]) = system.qspi.zipWithIndex.map({ case (s, i) =>
      val name = s"spi_${i}"
      val port = IO(new SPIPortIO(s.c)).suggestName(name)
      port <> s
      port
    })
    (ports, Nil)
  }
})