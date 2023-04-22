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

class DemoSoCConfig extends Config(
  new chipyard.config.WithSPIFlash(address=0x10030000, fAddress=0x20000000, size=0x10000000) ++

  new chipyard.config.WithUART(address=0x10021000, baudrate=115200) ++
  new chipyard.config.WithUARTOverride(address=0x10020000, baudrate=115200) ++
  
  new chipyard.config.WithGPIO(address=0x10012000, width=24) ++
  new chipyard.config.WithGPIO(address=0x10010000, width=3) ++

  new chipyard.config.WithTLSerialLocation(
    freechips.rocketchip.subsystem.FBUS,
    freechips.rocketchip.subsystem.PBUS) ++                       // attach TL serial adapter to f/p busses
  new freechips.rocketchip.subsystem.WithIncoherentBusTopology ++ // use incoherent bus topology
  new freechips.rocketchip.subsystem.WithNBanks(0) ++             // remove L2$
  new freechips.rocketchip.subsystem.WithNoMemPort ++             // remove backing memory
  new freechips.rocketchip.subsystem.With1TinyCore ++             // single tiny rocket-core
  new chipyard.config.AbstractConfig
  )
