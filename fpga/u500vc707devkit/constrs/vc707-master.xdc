#---------------Physical Constraints-----------------

set_property BOARD_PIN {clk_p} [get_ports sys_diff_clock_clk_p]
set_property BOARD_PIN {clk_n} [get_ports sys_diff_clock_clk_n]
set_property BOARD_PIN {reset} [get_ports reset]

# The MIG has its own create_clock
#create_clock -name ddr_ref_clk -period 5.0 [get_ports sys_diff_clock_clk_p]
set_input_jitter [get_clocks -of_objects [get_ports sys_diff_clock_clk_p]] 0.5

set_property BOARD_PIN {leds_8bits_tri_o_0} [get_ports led[0]]
set_property BOARD_PIN {leds_8bits_tri_o_1} [get_ports led[1]]
set_property BOARD_PIN {leds_8bits_tri_o_2} [get_ports led[2]]
set_property BOARD_PIN {leds_8bits_tri_o_3} [get_ports led[3]]
set_property BOARD_PIN {leds_8bits_tri_o_4} [get_ports led[4]]
set_property BOARD_PIN {leds_8bits_tri_o_5} [get_ports led[5]]
set_property BOARD_PIN {leds_8bits_tri_o_6} [get_ports led[6]]
set_property BOARD_PIN {leds_8bits_tri_o_7} [get_ports led[7]]

set_property PACKAGE_PIN AU33 [get_ports uart_rx]
set_property IOSTANDARD LVCMOS18 [get_ports uart_rx]
set_property IOB TRUE [get_ports uart_rx]
set_property PACKAGE_PIN AT32 [get_ports uart_ctsn]
set_property IOSTANDARD LVCMOS18 [get_ports uart_ctsn]
set_property IOB TRUE [get_ports uart_ctsn]
set_property PACKAGE_PIN AU36 [get_ports uart_tx]
set_property IOSTANDARD LVCMOS18 [get_ports uart_tx]
set_property IOB TRUE [get_ports uart_tx]
set_property PACKAGE_PIN AR34 [get_ports uart_rtsn]
set_property IOSTANDARD LVCMOS18 [get_ports uart_rtsn]
set_property IOB TRUE [get_ports uart_rtsn]

set_property IOB TRUE [get_cells "top/uart0/txm/out_reg"]
set_property IOB TRUE [get_cells "uart_rx_sync_reg[0]"]


# PCI Express 
#FMC 1 refclk
#set_property IOSTANDARD DIFF_HSTL_II_18 [get_ports {pci_exp_refclk_rxp}]
set_property PACKAGE_PIN A10 [get_ports {pci_exp_refclk_rxp}]
set_property PACKAGE_PIN A9 [get_ports {pci_exp_refclk_rxn}]
create_clock -name pcie_ref_clk -period 10 [get_ports pci_exp_refclk_rxp]
set_input_jitter [get_clocks -of_objects [get_ports pci_exp_refclk_rxp]] 0.5

set_property PACKAGE_PIN H4 [get_ports {pci_exp_txp[0]}]
set_property PACKAGE_PIN H3 [get_ports {pci_exp_txn[0]}]

set_property PACKAGE_PIN G6 [get_ports {pci_exp_rxp[0]}]
set_property PACKAGE_PIN G5 [get_ports {pci_exp_rxn[0]}]

# JTAG
set_property CLOCK_DEDICATED_ROUTE FALSE [get_nets jtag_TCK_IBUF]
set_property -dict { PACKAGE_PIN R32  IOSTANDARD LVCMOS18  PULLUP TRUE } [get_ports {jtag_TCK}]
set_property -dict { PACKAGE_PIN W36  IOSTANDARD LVCMOS18  PULLUP TRUE } [get_ports {jtag_TMS}]
set_property -dict { PACKAGE_PIN W37  IOSTANDARD LVCMOS18  PULLUP TRUE } [get_ports {jtag_TDI}]
set_property -dict { PACKAGE_PIN V40  IOSTANDARD LVCMOS18  PULLUP TRUE } [get_ports {jtag_TDO}]

# SDIO
#set_property -dict { PACKAGE_PIN AR32  IOSTANDARD LVCMOS18 } [get_ports {sdio_sdwp}]
#set_property -dict { PACKAGE_PIN AP32  IOSTANDARD LVCMOS18 } [get_ports {sdio_sddet}]
set_property -dict { PACKAGE_PIN AN30  IOSTANDARD LVCMOS18  IOB TRUE } [get_ports {sdio_clk}]
set_property -dict { PACKAGE_PIN AP30  IOSTANDARD LVCMOS18  IOB TRUE  PULLUP TRUE } [get_ports {sdio_cmd}]
set_property -dict { PACKAGE_PIN AR30  IOSTANDARD LVCMOS18  IOB TRUE  PULLUP TRUE } [get_ports {sdio_dat[0]}]
set_property -dict { PACKAGE_PIN AU31  IOSTANDARD LVCMOS18  IOB TRUE  PULLUP TRUE } [get_ports {sdio_dat[1]}]
set_property -dict { PACKAGE_PIN AV31  IOSTANDARD LVCMOS18  IOB TRUE  PULLUP TRUE } [get_ports {sdio_dat[2]}]
set_property -dict { PACKAGE_PIN AT30  IOSTANDARD LVCMOS18  IOB TRUE  PULLUP TRUE } [get_ports {sdio_dat[3]}]

#set_property MARK_DEBUG TRUE [get_nets "ip_axi_pcie_x1/inst/comp_axi_pcie_mm_s/comp_slave_bridge/comp_axi_slave_read/blk_lnk_up"]
#set_property MARK_DEBUG TRUE [get_nets "ip_axi_pcie_x1/inst/comp_axi_pcie_mm_s/comp_slave_bridge/comp_axi_slave_read/blk_lnk_up_d"]
#set_property MARK_DEBUG TRUE [get_nets "ip_axi_pcie_x1/inst/comp_axi_pcie_mm_s/comp_slave_bridge/comp_axi_slave_read/read_reqSM_cs*"]
#set_property MARK_DEBUG TRUE [get_nets "ip_axi_pcie_x1/inst/comp_axi_pcie_mm_s/comp_slave_bridge/comp_axi_slave_read/pcie_bme"]
#set_property MARK_DEBUG TRUE [get_nets "ip_axi_pcie_x1/inst/comp_axi_pcie_mm_s/comp_slave_bridge/comp_axi_slave_read/s_axi_arvalid"]
#set_property MARK_DEBUG TRUE [get_nets "ip_axi_pcie_x1/inst/comp_axi_pcie_mm_s/comp_slave_bridge/comp_axi_slave_read/arready_int"]
#set_property MARK_DEBUG TRUE [get_nets "ip_axi_pcie_x1/inst/comp_axi_pcie_mm_s/comp_slave_bridge/comp_axi_slave_read/en_barhit"]
#set_property MARK_DEBUG TRUE [get_nets "ip_axi_pcie_x1/inst/comp_axi_pcie_mm_s/comp_slave_bridge/comp_axi_slave_read/read_reqSM_ns*"]
#set_property MARK_DEBUG TRUE [get_nets "ip_axi_pcie_x1/inst/comp_axi_pcie_mm_s/comp_slave_bridge/comp_axi_slave_read/illegal_burst_int"]
#set_property MARK_DEBUG TRUE [get_nets "ip_axi_pcie_x1/inst/comp_axi_pcie_mm_s/comp_slave_bridge/comp_axi_slave_read/read_req_sent"]
#set_property MARK_DEBUG TRUE [get_nets "ip_axi_pcie_x1/inst/comp_axi_pcie_mm_s/comp_slave_bridge/comp_axi_slave_read/slot_request"]
#set_property MARK_DEBUG TRUE [get_nets "ip_axi_pcie_x1/inst/comp_axi_pcie_mm_s/comp_slave_bridge/comp_axi_slave_read/open_slot"]
#set_property MARK_DEBUG TRUE [get_nets "ip_axi_pcie_x1/inst/comp_axi_pcie_mm_s/comp_slave_bridge/comp_axi_slave_read/s_axi_arvalid"]

#set_property MARK_DEBUG TRUE [get_nets "top/uncore/outmemsys/L2BroadcastHub_1/BufferedBroadcastAcquireTracker_2/state*"]
#set_property MARK_DEBUG TRUE [get_nets "top/uncore/outmemsys/L2BroadcastHub_1/BufferedBroadcastAcquireTracker_1_1/*acquire*"]
#set_property MARK_DEBUG TRUE [get_nets "top/uncore/outmemsys/L2BroadcastHub_1/io_*"]

set_clock_groups -asynchronous \
  -group [list \
     [get_clocks -include_generated_clocks -of_objects [get_pins -hier -filter {name =~ *pcie*TXOUTCLK}]]] \
  -group [list \
     [get_clocks -include_generated_clocks -of_objects [get_ports sys_diff_clock_clk_p]]]
