package chipyard

import org.chipsalliance.cde.config.{Config}
import freechips.rocketchip.diplomacy._

// A simple config demonstrating how to set up a basic chip in Chipyard
class ChipLikeQuadRocketConfig extends Config(
  //==================================
  // Set up TestHarness
  //==================================
  new chipyard.WithAbsoluteFreqHarnessClockInstantiator ++ // use absolute frequencies for simulations in the harness
                                                           // NOTE: This only simulates properly in VCS

  //==================================
  // Set up tiles
  //==================================
  new freechips.rocketchip.subsystem.WithAsynchronousRocketTiles(3, 3) ++    // Add rational crossings between RocketTile and uncore
  new freechips.rocketchip.subsystem.WithNBigCores(4) ++                     // quad-core (4 RocketTiles)

  //==================================
  // Set up I/O
  //==================================
  new testchipip.WithSerialTLWidth(4) ++
  new chipyard.harness.WithSimAXIMemOverSerialTL ++                                     // Attach fast SimDRAM to TestHarness
  new chipyard.config.WithSerialTLBackingMemory ++                                      // Backing memory is over serial TL protocol
  new freechips.rocketchip.subsystem.WithExtMemSize((1 << 30) * 4L) ++                  // 4GB max external memory

  //==================================
  // Set up clock./reset
  //==================================
  new chipyard.clocking.WithPLLSelectorDividerClockGenerator ++   // Use a PLL-based clock selector/divider generator structure

  // Create two clock groups, uncore and fbus, in addition to the tile clock groups
  new chipyard.clocking.WithClockGroupsCombinedByName("uncore", "implicit", "sbus", "mbus", "cbus", "system_bus") ++
  new chipyard.clocking.WithClockGroupsCombinedByName("fbus", "fbus", "pbus") ++

  // Set up the crossings
  new chipyard.config.WithFbusToSbusCrossingType(AsynchronousCrossing()) ++  // Add Async crossing between SBUS and FBUS
  new chipyard.config.WithCbusToPbusCrossingType(AsynchronousCrossing()) ++  // Add Async crossing between PBUS and CBUS
  new chipyard.config.WithSbusToMbusCrossingType(AsynchronousCrossing()) ++  // Add Async crossings between backside of L2 and MBUS
  new testchipip.WithAsynchronousSerialSlaveCrossing ++                      // Add Async crossing between serial and MBUS. Its master-side is tied to the FBUS

  new chipyard.config.AbstractConfig)

// class DemoSoCConfig extends Config(
//   new chipyard.config.WithSPIFlash(address=0x10030000, fAddress=0x20000000, size=0x10000000) ++

//   new chipyard.config.WithUART(address=0x10021000, baudrate=115200) ++
//   new chipyard.config.WithUARTOverride(address=0x10020000, baudrate=115200) ++
  
//   new chipyard.config.WithGPIO(address=0x10012000, width=24) ++
//   new chipyard.config.WithGPIO(address=0x10010000, width=3) ++

//   new chipyard.config.WithTLSerialLocation(
//     freechips.rocketchip.subsystem.FBUS,
//     freechips.rocketchip.subsystem.PBUS) ++                       // attach TL serial adapter to f/p busses
//   new freechips.rocketchip.subsystem.WithIncoherentBusTopology ++ // use incoherent bus topology
//   new freechips.rocketchip.subsystem.WithNBanks(0) ++             // remove L2$
//   new freechips.rocketchip.subsystem.WithNoMemPort ++             // remove backing memory
//   new freechips.rocketchip.subsystem.With1TinyCore ++             // single tiny rocket-core
//   new chipyard.config.AbstractConfig
//   )

// class DemoSoCConfig extends Config(
  
//   new chipyard.config.WithNPMPs(0) ++
//   new chipyard.config.WithL2TLBs(0) ++

//   new chipyard.config.WithJTAGDTMKey(idcodeVersion = 2, partNum = 0x000, manufId = 0x489, debugIdleCycles = 5) ++
  
  
//   new chipyard.config.WithSPIFlash(address=0x10030000, fAddress=0x20000000, size=0x10000000) ++

//   new chipyard.config.WithUART(address=0x10021000, baudrate=115200) ++
//   new chipyard.config.WithUARTOverride(address=0x10020000, baudrate=115200) ++
  
//   new chipyard.config.WithGPIO(address=0x10012000, width=24) ++
//   new chipyard.config.WithGPIO(address=0x10010000, width=3) ++


//   // new freechips.rocketchip.subsystem.WithL1ICacheSets(64) ++
//   // new freechips.rocketchip.subsystem.WithL1ICacheWays(2) ++
//   // new freechips.rocketchip.subsystem.WithL1DCacheSets(64) ++
//   // new freechips.rocketchip.subsystem.WithL1DCacheWays(2) ++
  
//   new chipyard.config.WithTLSerialLocation(
//     freechips.rocketchip.subsystem.FBUS,
//     freechips.rocketchip.subsystem.PBUS) ++                       // attach TL serial adapter to f/p busses
//   new freechips.rocketchip.subsystem.WithIncoherentBusTopology ++ // use incoherent bus topology
//   // new freechips.rocketchip.subsystem.WithNoMemPort ++             // remove backing memory
//   new freechips.rocketchip.subsystem.WithNSmallCores(1) ++
//   // new freechips.rocketchip.subsystem.With1TinyCore ++


//   // The HarnessBinders control generation of hardware in the TestHarness
//   new chipyard.harness.WithUARTAdapter ++                       // add UART adapter to display UART on stdout, if uart is present
//   new chipyard.harness.WithBlackBoxSimMem ++                    // add SimDRAM DRAM model for axi4 backing memory, if axi4 mem is enabled
//   new chipyard.harness.WithSimSerial ++                         // add external serial-adapter and RAM
//   new chipyard.harness.WithSimDebug ++                          // add SimJTAG or SimDTM adapters if debug module is enabled
//   new chipyard.harness.WithGPIOTiedOff ++                       // tie-off chiptop GPIOs, if GPIOs are present
//   new chipyard.harness.WithSimSPIFlashModel ++                  // add simulated SPI flash memory, if SPI is enabled
//   new chipyard.harness.WithSimAXIMMIO ++                        // add SimAXIMem for axi4 mmio port, if enabled
//   new chipyard.harness.WithTieOffInterrupts ++                  // tie-off interrupt ports, if present
//   new chipyard.harness.WithTieOffL2FBusAXI ++                   // tie-off external AXI4 master, if present
//   new chipyard.harness.WithCustomBootPinPlusArg ++
//   new chipyard.harness.WithClockAndResetFromHarness ++

//   // The IOBinders instantiate ChipTop IOs to match desired digital IOs
//   // IOCells are generated for "Chip-like" IOs, while simulation-only IOs are directly punched through
//   new chipyard.iobinders.WithAXI4MemPunchthrough ++
//   new chipyard.iobinders.WithAXI4MMIOPunchthrough ++
//   new chipyard.iobinders.WithTLMemPunchthrough ++
//   new chipyard.iobinders.WithL2FBusAXI4Punchthrough ++
//   new chipyard.iobinders.WithBlockDeviceIOPunchthrough ++
//   new chipyard.iobinders.WithNICIOPunchthrough ++
//   new chipyard.iobinders.WithSerialTLIOCells ++
//   new chipyard.iobinders.WithDebugIOCells ++
//   new chipyard.iobinders.WithUARTIOCells ++
//   new chipyard.iobinders.WithGPIOCells ++
//   new chipyard.iobinders.WithSPIIOCells ++
//   new chipyard.iobinders.WithTraceIOPunchthrough ++
//   new chipyard.iobinders.WithExtInterruptIOCells ++
//   new chipyard.iobinders.WithCustomBootPin ++

//   // Default behavior is to use a divider-only clock-generator
//   // This works in VCS, Verilator, and FireSim/
//   // This should get replaced with a PLL-like config instead
//   new chipyard.clocking.WithDividerOnlyClockGenerator ++

//   new testchipip.WithSerialTLWidth(32) ++                           // fatten the serialTL interface to improve testing performance
//   new testchipip.WithDefaultSerialTL ++                             // use serialized tilelink port to external serialadapter/harnessRAM
//   new chipyard.config.WithBootROM ++                                // use default bootrom
//   new chipyard.config.WithL2TLBs(1024) ++                           // use L2 TLBs
//   new chipyard.config.WithNoSubsystemDrivenClocks ++                // drive the subsystem diplomatic clocks from ChipTop instead of using implicit clocks
//   new chipyard.config.WithInheritBusFrequencyAssignments ++         // Unspecified clocks within a bus will receive the bus frequency if set
//   new chipyard.config.WithPeripheryBusFrequencyAsDefault ++         // Unspecified frequencies with match the pbus frequency (which is always set)
//   new chipyard.config.WithMemoryBusFrequency(100.00) ++              // MBus frequency
//   new chipyard.config.WithPeripheryBusFrequency(100.00) ++           // PBus frequency
//   new freechips.rocketchip.subsystem.WithClockGateModel ++          // add default EICG_wrapper clock gate model
//   new freechips.rocketchip.subsystem.WithJtagDTM ++                 // set the debug module to expose a JTAG port
//   new freechips.rocketchip.subsystem.WithNoMMIOPort ++              // no top-level MMIO master port (overrides default set in rocketchip)
//   new freechips.rocketchip.subsystem.WithNoSlavePort ++             // no top-level MMIO slave port (overrides default set in rocketchip)
//   new freechips.rocketchip.subsystem.WithInclusiveCache ++          // use Sifive L2 cache
//   new freechips.rocketchip.subsystem.WithNExtTopInterrupts(0) ++    // no external interrupts
//   new freechips.rocketchip.subsystem.WithDontDriveBusClocksFromSBus ++ // leave the bus clocks undriven by sbus
//   new freechips.rocketchip.subsystem.WithDTS("ucb-bar, chipyard", Nil) ++ // custom device name for DTS

//   new freechips.rocketchip.subsystem.WithCoherentBusTopology ++     // hierarchical buses including sbus/mbus/pbus/fbus/cbus/l2
//   new freechips.rocketchip.system.BaseConfig                        // "base" rocketchip system
// )

class DemoSoCConfig extends Config(
  
  new chipyard.config.WithJTAGDTMKey(idcodeVersion = 2, partNum = 0x000, manufId = 0x489, debugIdleCycles = 5) ++
  
  
  new chipyard.config.WithSPIFlash(address=0x10030000, fAddress=0x20000000, size=0x10000000) ++

  new chipyard.config.WithUART(address=0x10021000, baudrate=115200) ++
  new chipyard.config.WithUARTOverride(address=0x10020000, baudrate=115200) ++
  
  new chipyard.config.WithGPIO(address=0x10012000, width=24) ++
  new chipyard.config.WithGPIO(address=0x10010000, width=3) ++


  new chipyard.iobinders.WithDontTouchIOBinders(false) ++         // TODO FIX: Don't dontTouch the ports
  
  new freechips.rocketchip.subsystem.WithScratchpadsOnly ++

  new chipyard.config.WithTLSerialLocation(
    freechips.rocketchip.subsystem.FBUS,
    freechips.rocketchip.subsystem.PBUS) ++                       // attach TL serial adapter to f/p busses
  new freechips.rocketchip.subsystem.WithIncoherentBusTopology ++ // use incoherent bus topology
  new freechips.rocketchip.subsystem.WithNBanks(0) ++             // remove L2$
  new freechips.rocketchip.subsystem.WithNoMemPort ++             // remove backing memory
  new freechips.rocketchip.subsystem.WithNSmallCores(1) ++             // single tiny rocket-core
  new chipyard.config.AbstractConfig)
