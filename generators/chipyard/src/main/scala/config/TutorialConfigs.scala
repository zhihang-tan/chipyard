package chipyard

import scala.collection.immutable.ListMap
import chisel3.util._
import org.chipsalliance.cde.config.{Config}
import org.chipsalliance.cde.config._
import freechips.rocketchip.devices.debug._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.rocket._
import freechips.rocketchip.tile._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.util._
import constellation.channel._
import constellation.routing._
import constellation.topology._
import constellation.noc._
import constellation.soc.{GlobalNoCParams}

// This file is designed to accompany a live tutorial, with slides.
// For each of 4 phases, participants will customize and build a
// small demonstration config.

// This file is designed to be used after running chipyard/scripts/tutorial-setup.sh,
// which removes the SHA3 accelerator RTL, and provides participants
// the experience of integrating external RTL.

// This file was originally developed for the cancelled ASPLOS-2020
// Chipyard tutorial. While the configs here work, the corresponding
// slideware has not yet been created.

// NOTE: Configs should be read bottom-up, since they are applied bottom-up

// NOTE: The TutorialConfigs build off of the AbstractConfig defined in AbstractConfig.scala
//       Users should try to understand the functionality of the AbstractConfig before proceeding
//       with the TutorialConfigs below


class SubsystemConfig extends Config ((site, here, up) => {
  // Tile parameters
  case PgLevels => if (site(XLen) == 64) 3 /* Sv39 */ else 2 /* Sv32 */
  case XLen => 64 // Applies to all cores
  case MaxHartIdBits => log2Up((site(PossibleTileLocations).flatMap(loc => site(TilesLocated(loc)))
      .map(_.tileParams.tileId) :+ 0).max+1)
  // Interconnect parameters
  case SystemBusKey => SystemBusParams(
    beatBytes = site(XLen)/8,
    blockBytes = site(CacheBlockBytes))
  case ControlBusKey => PeripheryBusParams(
    beatBytes = site(XLen)/8,
    blockBytes = site(CacheBlockBytes),
    dtsFrequency = Some(100000000), // Default to 100 MHz cbus clock
    errorDevice = Some(BuiltInErrorDeviceParams(
      errorParams = DevNullParams(List(AddressSet(0x3000, 0xfff)), maxAtomic=site(XLen)/8, maxTransfer=4096))))
  case PeripheryBusKey => PeripheryBusParams(
    beatBytes = site(XLen)/8,
    blockBytes = site(CacheBlockBytes),
    dtsFrequency = Some(100000000)) // Default to 100 MHz pbus clock
  case MemoryBusKey => MemoryBusParams(
    beatBytes = site(XLen)/8,
    blockBytes = site(CacheBlockBytes))
  case FrontBusKey => FrontBusParams(
    beatBytes = site(XLen)/8,
    blockBytes = site(CacheBlockBytes))
  // Additional device Parameters
  
  case HasTilesExternalResetVectorKey => false
  case TilesLocated(InSubsystem) => Nil
  case PossibleTileLocations => Seq(InSubsystem)
})

class TutorialConfig extends Config(
  // ================================================
  //   Set up TestHarness
  // ================================================
  // The HarnessBinders control generation of hardware in the TestHarness
  new chipyard.harness.WithUARTAdapter ++                          /** add UART adapter to display UART on stdout, if uart is present */
  new chipyard.harness.WithBlackBoxSimMem ++                       /** add SimDRAM DRAM model for axi4 backing memory, if axi4 mem is enabled */
  new chipyard.harness.WithSimTSIOverSerialTL ++                   /** add external serial-adapter and RAM */
  new chipyard.harness.WithSimJTAGDebug ++                         /** add SimJTAG if JTAG for debug exposed */
  new chipyard.harness.WithSimDMI ++                               /** add SimJTAG if DMI exposed */
  new chipyard.harness.WithGPIOTiedOff ++                          /** tie-off chiptop GPIOs, if GPIOs are present */
  new chipyard.harness.WithSimSPIFlashModel ++                     /** add simulated SPI flash memory, if SPI is enabled */
  new chipyard.harness.WithSimAXIMMIO ++                           /** add SimAXIMem for axi4 mmio port, if enabled */
  new chipyard.harness.WithTieOffInterrupts ++                     /** tie-off interrupt ports, if present */
  new chipyard.harness.WithTieOffL2FBusAXI ++                      /** tie-off external AXI4 master, if present */
  new chipyard.harness.WithCustomBootPinPlusArg ++                 /** drive custom-boot pin with a plusarg, if custom-boot-pin is present */
  new chipyard.harness.WithDriveChipIdPin ++                       /** drive chip id pin from harness binder, if chip id pin is present */
  new chipyard.harness.WithSimUARTToUARTTSI ++                     /** connect a SimUART to the UART-TSI port */
  new chipyard.harness.WithClockFromHarness ++                     /** all Clock I/O in ChipTop should be driven by harnessClockInstantiator */
  new chipyard.harness.WithResetFromHarness ++                     /** reset controlled by harness */
  new chipyard.harness.WithAbsoluteFreqHarnessClockInstantiator ++ /** generate clocks in harness with unsynthesizable ClockSourceAtFreqMHz */


  // ================================================
  //   Set up I/O cells + punch I/Os in ChipTop
  // ================================================
  // The IOBinders instantiate ChipTop IOs to match desired digital IOs
  // IOCells are generated for "Chip-like" IOs
  new chipyard.iobinders.WithSerialTLIOCells ++
  new chipyard.iobinders.WithDebugIOCells ++
  new chipyard.iobinders.WithUARTIOCells ++
  new chipyard.iobinders.WithGPIOCells ++
  new chipyard.iobinders.WithSPIFlashIOCells ++
  new chipyard.iobinders.WithExtInterruptIOCells ++
  new chipyard.iobinders.WithChipIdIOCells ++
  new chipyard.iobinders.WithCustomBootPin ++
  // The "punchthrough" IOBInders below don't generate IOCells, as these interfaces shouldn't really be mapped to ASIC IO
  // Instead, they directly pass through the DigitalTop ports to ports in the ChipTop
  new chipyard.iobinders.WithI2CPunchthrough ++
  new chipyard.iobinders.WithSPIIOPunchthrough ++
  new chipyard.iobinders.WithAXI4MemPunchthrough ++
  new chipyard.iobinders.WithAXI4MMIOPunchthrough ++
  new chipyard.iobinders.WithTLMemPunchthrough ++
  new chipyard.iobinders.WithL2FBusAXI4Punchthrough ++
  new chipyard.iobinders.WithBlockDeviceIOPunchthrough ++
  new chipyard.iobinders.WithNICIOPunchthrough ++
  new chipyard.iobinders.WithTraceIOPunchthrough ++
  new chipyard.iobinders.WithUARTTSIPunchthrough ++
  new chipyard.iobinders.WithNMITiedOff ++


  // ================================================
  //   Set up External Memory and IO Devices
  // ================================================
  // External memory section
  new testchipip.serdes.WithSerialTL(Seq(                           /** add a serial-tilelink interface */
    testchipip.serdes.SerialTLParams(
      client = Some(testchipip.serdes.SerialTLClientParams(idBits=4)), /** serial-tilelink interface will master the FBUS, and support 4 idBits */
      phyParams = testchipip.serdes.ExternalSyncSerialParams(width=32) /** serial-tilelink interface with 32 lanes */
    )
  )) ++
  new freechips.rocketchip.subsystem.WithNMemoryChannels(1) ++         /** Default 1 AXI-4 memory channels */
  new freechips.rocketchip.subsystem.WithNoMMIOPort ++                 /** no top-level MMIO master port (overrides default set in rocketchip) */
  new freechips.rocketchip.subsystem.WithNoSlavePort ++                /** no top-level MMIO slave port (overrides default set in rocketchip) */

  // MMIO device section
  new chipyard.config.WithUART ++                                  /** add a UART */


  // ================================================
  //   Set up Debug/Bringup/Testing Features
  // ================================================
  // JTAG
  new chipyard.config.WithDebug ++
  new freechips.rocketchip.subsystem.WithDebugSBA ++                /** enable the SBA (system-bus-access) feature of the debug module */
  new chipyard.config.WithDebugModuleAbstractDataWords(8) ++        /** increase debug module data word capacity */
  new freechips.rocketchip.subsystem.WithJtagDTM ++                 /** set the debug module to expose a JTAG port */

  // Boot Select Pins
  new testchipip.boot.WithCustomBootPin ++                          /** add a custom-boot-pin to support pin-driven boot address */
  new testchipip.boot.WithBootAddrReg ++                            /** add a boot-addr-reg for configurable boot address */


  // ================================================
  //   Set up Interrupts
  // ================================================
  // CLINT and PLIC related settings goes here
  new freechips.rocketchip.subsystem.WithNExtTopInterrupts(0) ++    /** no external interrupts */
  new chipyard.config.WithPLIC ++
  new chipyard.config.WithCLINT ++

  // ================================================
  //   Set up Tiles
  // ================================================
  // tile-local settings goes here

  new freechips.rocketchip.subsystem.WithNBigCores(1) ++         // single rocket-core

  // ================================================
  //   Set up Memory system
  // ================================================
  // On-chip memory section
  new freechips.rocketchip.subsystem.WithDTS("ucb-bar,chipyard", Nil) ++ /** custom device name for DTS (embedded in BootROM) */
  new chipyard.config.WithBootROM ++                                     /** use default bootrom */
  new testchipip.soc.WithMbusScratchpad(base = 0x08000000,               /** add 64 KiB on-chip scratchpad */
                                        size = 64 * 1024) ++

  // Coherency settings
  new freechips.rocketchip.subsystem.WithInclusiveCache ++          /** use Sifive LLC cache as root of coherence */

  // Bus/interconnect settings
  new freechips.rocketchip.subsystem.WithCoherentBusTopology ++     /** hierarchical buses including sbus/mbus/pbus/fbus/cbus/l2 */
  new freechips.rocketchip.subsystem.WithDefaultMemPort ++
  new freechips.rocketchip.subsystem.WithDefaultMMIOPort ++
  new freechips.rocketchip.subsystem.WithDefaultSlavePort ++

  // ================================================
  //   Set up power, reset and clocking
  // ================================================

  // ChipTop clock IO/PLL/Divider/Mux settings
  new chipyard.clocking.WithClockTapIOCells ++                      /** Default generate a clock tapio */
  new chipyard.clocking.WithPassthroughClockGenerator ++

  // DigitalTop-internal clocking settings
  new freechips.rocketchip.subsystem.WithDontDriveBusClocksFromSBus ++  /** leave the bus clocks undriven by sbus */
  new freechips.rocketchip.subsystem.WithClockGateModel ++              /** add default EICG_wrapper clock gate model */
  new chipyard.clocking.WithClockGroupsCombinedByName(("uncore",        /** create a "uncore" clock group tieing all the bus clocks together */
    Seq("sbus", "mbus", "pbus", "fbus", "cbus", "obus", "implicit", "clock_tap"), 
    Seq("tile"))) ++

  new chipyard.config.WithPeripheryBusFrequency(500.0) ++           /** Default 500 MHz pbus */
  new chipyard.config.WithMemoryBusFrequency(500.0) ++              /** Default 500 MHz mbus */
  new chipyard.config.WithControlBusFrequency(500.0) ++             /** Default 500 MHz cbus */
  new chipyard.config.WithSystemBusFrequency(500.0) ++              /** Default 500 MHz sbus */
  new chipyard.config.WithFrontBusFrequency(500.0) ++               /** Default 500 MHz fbus */
  new chipyard.config.WithOffchipBusFrequency(500.0) ++             /** Default 500 MHz obus */
  new chipyard.config.WithInheritBusFrequencyAssignments ++         /** Unspecified clocks within a bus will receive the bus frequency if set */
  new chipyard.config.WithNoSubsystemClockIO ++                     /** drive the subsystem diplomatic clocks from ChipTop instead of using implicit clocks */
  new freechips.rocketchip.subsystem.WithTimebase(BigInt(1000000)) ++ // 1 MHz

  // reset

  // power


  // ==================================
  //   Base Settings
  // ==================================
  new SubsystemConfig
)















// Tutorial Phase 1: Configure the cores, caches
class TutorialStarterConfig extends Config(
  // CUSTOMIZE THE CORE
  // Uncomment out one (or multiple) of the lines below, and choose
  // how many cores you want.
  // new freechips.rocketchip.subsystem.WithNBigCores(1) ++    // Specify we want some number of Rocket cores
  // new boom.common.WithNSmallBooms(1) ++                     // Specify we want some number of BOOM cores

  // CUSTOMIZE the L2
  // Uncomment this line, and specify a size if you want to have a L2
  // new freechips.rocketchip.subsystem.WithInclusiveCache(nBanks=1, nWays=4, capacityKB=128) ++

  new chipyard.config.AbstractConfig
)

// Tutorial Phase 2: Integrate a TileLink or AXI4 MMIO device
class TutorialMMIOConfig extends Config(

  // Attach either a TileLink or AXI4 version of GCD
  // Uncomment one of the below lines
  // new chipyard.example.WithGCD(useAXI4=false) ++ // Use TileLink version
  // new chipyard.example.WithGCD(useAXI4=true) ++  // Use AXI4 version

  // For this demonstration we assume the base system is a single-core Rocket, for fast elaboration
  new freechips.rocketchip.subsystem.WithNBigCores(1) ++
  new chipyard.config.AbstractConfig
)

// Tutorial Phase 3: Integrate a SHA3 RoCC accelerator
class TutorialSha3Config extends Config(
  // Uncomment this line once you added SHA3 to the build.sbt, and cloned the SHA3 repo
  // new sha3.WithSha3Accel ++

  // For this demonstration we assume the base system is a single-core Rocket, for fast elaboration
  new freechips.rocketchip.subsystem.WithNBigCores(1) ++
  new chipyard.config.AbstractConfig
)

// Tutorial Phase 4: Integrate a Black-box verilog version of the SHA3 RoCC accelerator
class TutorialSha3BlackBoxConfig extends Config(
  // Uncomment these lines once SHA3 is integrated
  // new sha3.WithSha3BlackBox ++ // Specify we want the Black-box verilog version of Sha3 Ctrl
  // new sha3.WithSha3Accel ++

  // For this demonstration we assume the base system is a single-core Rocket, for fast elaboration
  new freechips.rocketchip.subsystem.WithNBigCores(1) ++
  new chipyard.config.AbstractConfig
)

// Tutorial Phase 5: Map a multicore heterogeneous SoC with multiple cores and memory-mapped accelerators
class TutorialNoCConfig extends Config(
  new chipyard.harness.WithDontTouchChipTopPorts(false) ++
  // Try changing the dimensions of the Mesh topology
  new constellation.soc.WithGlobalNoC(constellation.soc.GlobalNoCParams(
    NoCParams(
      topology        = TerminalRouter(Mesh2D(3, 4)),
      channelParamGen = (a, b) => UserChannelParams(Seq.fill(12) { UserVirtualChannelParams(4) }),
      routingRelation = NonblockingVirtualSubnetworksRouting(TerminalRouterRouting(
        Mesh2DEscapeRouting()), 10, 1)
    )
  )) ++
  // The inNodeMapping and outNodeMapping values are the physical identifiers of
  // routers on the topology to map the agents to. Try changing these to any
  // value within the range [0, topology.nNodes)
  new constellation.soc.WithPbusNoC(constellation.protocol.GlobalTLNoCParams(
    constellation.protocol.DiplomaticNetworkNodeMapping(
      inNodeMapping = ListMap("Core" -> 7),
      outNodeMapping = ListMap(
        "pbus" -> 8, "uart" -> 9, "control" -> 10, "gcd" -> 11,
        "writeQueue[0]" -> 0, "writeQueue[1]" -> 1, "tailChain[0]" -> 2))
  )) ++
  new constellation.soc.WithSbusNoC(constellation.protocol.GlobalTLNoCParams(
    constellation.protocol.DiplomaticNetworkNodeMapping(
      inNodeMapping = ListMap(
        "Core 0" -> 0, "Core 1" -> 1,
        "serial-tl" -> 2),
      outNodeMapping = ListMap(
        "system[0]" -> 3, "system[1]" -> 4, "system[2]" -> 5, "system[3]" -> 6,
        "pbus" -> 7))
  )) ++
  new chipyard.example.WithGCD ++
  new chipyard.harness.WithLoopbackNIC ++
  new icenet.WithIceNIC ++
  new fftgenerator.WithFFTGenerator(numPoints=8) ++
  new chipyard.example.WithStreamingFIR ++
  new chipyard.example.WithStreamingPassthrough ++

  new freechips.rocketchip.subsystem.WithNBanks(4) ++
  new freechips.rocketchip.subsystem.WithNBigCores(2) ++
  new chipyard.config.AbstractConfig
)
