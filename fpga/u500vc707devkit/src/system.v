// See LICENSE for license details.
`timescale 1ns/1ps
`default_nettype none

module system
(
  //200Mhz differential sysclk
  input  wire sys_diff_clock_clk_n,
  input  wire sys_diff_clock_clk_p,
  //active high reset
  input  wire reset,
  // DDR3 SDRAM
  output wire [13:0] ddr3_addr,
  output wire [2:0]  ddr3_ba,
  output wire        ddr3_cas_n,
  output wire [0:0]  ddr3_ck_n,
  output wire [0:0]  ddr3_ck_p,
  output wire [0:0]  ddr3_cke,
  output wire [0:0]  ddr3_cs_n,
  output wire [7:0]  ddr3_dm,
  inout  wire [63:0] ddr3_dq,
  inout  wire [7:0]  ddr3_dqs_n,
  inout  wire [7:0]  ddr3_dqs_p,
  output wire [0:0]  ddr3_odt,
  output wire        ddr3_ras_n,
  output wire        ddr3_reset_n,
  output wire        ddr3_we_n,
  // LED
  output wire [7:0] led,
  //UART
  output wire uart_tx,
  input  wire uart_rx,
  output wire uart_rtsn,
  input  wire uart_ctsn,
  //SDIO
  output wire       sdio_clk,
  inout  wire       sdio_cmd,
  inout  wire [3:0] sdio_dat,
  //JTAG
  input  wire       jtag_TCK,
  input  wire       jtag_TMS,
  input  wire       jtag_TDI,
  output wire       jtag_TDO,
  //PCIe
  output wire [0:0] pci_exp_txp,
  output wire [0:0] pci_exp_txn,
  input  wire [0:0] pci_exp_rxp,
  input  wire [0:0] pci_exp_rxn,
  input  wire       pci_exp_refclk_rxp,
  input  wire       pci_exp_refclk_rxn
);

reg [1:0] uart_rx_sync;
wire [3:0] sd_spi_dq_i;
wire [3:0] sd_spi_dq_o;
wire sd_spi_sck;
wire sd_spi_cs;
wire top_clock,top_reset;

U500VC707DevKitTop top
(
  //UART
  .io_uarts_0_rxd(uart_rx_sync[1]),
  .io_uarts_0_txd(uart_tx),
  //SPI
  .io_spis_0_sck(sd_spi_sck),
  .io_spis_0_dq_0_i(sd_spi_dq_i[0]),
  .io_spis_0_dq_1_i(sd_spi_dq_i[1]),
  .io_spis_0_dq_2_i(sd_spi_dq_i[2]),
  .io_spis_0_dq_3_i(sd_spi_dq_i[3]),
  .io_spis_0_dq_0_o(sd_spi_dq_o[0]),
  .io_spis_0_dq_1_o(sd_spi_dq_o[1]),
  .io_spis_0_dq_2_o(sd_spi_dq_o[2]),
  .io_spis_0_dq_3_o(sd_spi_dq_o[3]),
  .io_spis_0_dq_0_oe(),
  .io_spis_0_dq_1_oe(),
  .io_spis_0_dq_2_oe(),
  .io_spis_0_dq_3_oe(),
  .io_spis_0_cs_0(sd_spi_cs),
  //GPIO
  .io_gpio_pins_0_i_ival(1'b0),
  .io_gpio_pins_1_i_ival(1'b0),
  .io_gpio_pins_2_i_ival(1'b0),
  .io_gpio_pins_3_i_ival(1'b0),
  .io_gpio_pins_0_o_oval(led[0]),
  .io_gpio_pins_1_o_oval(led[1]),
  .io_gpio_pins_2_o_oval(led[2]),
  .io_gpio_pins_3_o_oval(led[3]),
  .io_gpio_pins_0_o_oe(),
  .io_gpio_pins_1_o_oe(),
  .io_gpio_pins_2_o_oe(),
  .io_gpio_pins_3_o_oe(),
  .io_gpio_pins_0_o_pue(),
  .io_gpio_pins_1_o_pue(),
  .io_gpio_pins_2_o_pue(),
  .io_gpio_pins_3_o_pue(),
  .io_gpio_pins_0_o_ds(),
  .io_gpio_pins_1_o_ds(),
  .io_gpio_pins_2_o_ds(),
  .io_gpio_pins_3_o_ds(),
  //JTAG
  .io_jtag_TRST(1'b0),
  .io_jtag_TCK(jtag_TCK),
  .io_jtag_TMS(jtag_TMS),
  .io_jtag_TDI(jtag_TDI),
  .io_jtag_DRV_TDO(),
  .io_jtag_TDO(jtag_TDO),
  //MIG
  .io_xilinxvc707mig__inout_ddr3_dq(ddr3_dq),
  .io_xilinxvc707mig__inout_ddr3_dqs_n(ddr3_dqs_n),
  .io_xilinxvc707mig__inout_ddr3_dqs_p(ddr3_dqs_p),
  .io_xilinxvc707mig_ddr3_addr(ddr3_addr),
  .io_xilinxvc707mig_ddr3_ba(ddr3_ba),
  .io_xilinxvc707mig_ddr3_ras_n(ddr3_ras_n),
  .io_xilinxvc707mig_ddr3_cas_n(ddr3_cas_n),
  .io_xilinxvc707mig_ddr3_we_n(ddr3_we_n),
  .io_xilinxvc707mig_ddr3_reset_n(ddr3_reset_n),
  .io_xilinxvc707mig_ddr3_ck_p(ddr3_ck_p),
  .io_xilinxvc707mig_ddr3_ck_n(ddr3_ck_n),
  .io_xilinxvc707mig_ddr3_cke(ddr3_cke),
  .io_xilinxvc707mig_ddr3_cs_n(ddr3_cs_n),
  .io_xilinxvc707mig_ddr3_dm(ddr3_dm),
  .io_xilinxvc707mig_ddr3_odt(ddr3_odt),
  //PCIe
  .io_xilinxvc707pcie_pci_exp_txp(pci_exp_txp),
  .io_xilinxvc707pcie_pci_exp_txn(pci_exp_txn),
  .io_xilinxvc707pcie_pci_exp_rxp(pci_exp_rxp),
  .io_xilinxvc707pcie_pci_exp_rxn(pci_exp_rxn),
  //Clock + Reset
  .io_pcie_refclk_p(pci_exp_refclk_rxp),
  .io_pcie_refclk_n(pci_exp_refclk_rxn),
  .io_sys_clk_p(sys_diff_clock_clk_p),
  .io_sys_clk_n(sys_diff_clock_clk_n),
  .io_sys_reset(reset),
  //Misc outputs for system.v
  .io_core_clock(top_clock),
  .io_core_reset(top_reset)
);

  sdio_spi_bridge ip_sdio_spi
  (
   .clk(top_clock),
   .reset(top_reset),
   .sd_cmd(sdio_cmd),
   .sd_dat(sdio_dat),
   .sd_sck(sdio_clk),
   .spi_sck(sd_spi_sck),
   .spi_dq_o(sd_spi_dq_o),
   .spi_dq_i(sd_spi_dq_i),
   .spi_cs(sd_spi_cs)
  );

  //UART
  assign uart_rtsn =1'b0;  
  always @(posedge top_clock) begin
    if (top_reset) begin
      uart_rx_sync <= 2'b11;
    end else begin
      uart_rx_sync[0] <= uart_rx;
      uart_rx_sync[1] <= uart_rx_sync[0];
    end
  end

  assign led[7:4] = 4'b0000;

endmodule

`default_nettype wire
