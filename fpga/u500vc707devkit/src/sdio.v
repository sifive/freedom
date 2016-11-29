// See LICENSE for license details.
`timescale 1ns/1ps
`default_nettype none

module sdio_spi_bridge (
  input wire clk,
  input wire reset,
  // SDIO
  inout  wire       sd_cmd,
  inout  wire [3:0] sd_dat,
  output wire       sd_sck,
  // QUAD SPI
  input  wire       spi_sck,
  input  wire [3:0] spi_dq_o,
  output wire [3:0] spi_dq_i,
  output wire       spi_cs
);

  wire mosi, miso;
  reg miso_sync [1:0];

  assign mosi = spi_dq_o[0];
  assign spi_dq_i = {2'b00, miso_sync[1], 1'b0};

  assign sd_sck = spi_sck;

  IOBUF buf_cmd (
    .IO(sd_cmd),
    .I(mosi),
    .O(),
    .T(1'b0)
  );

  IOBUF buf_dat0 (
    .IO(sd_dat[0]),
    .I(),
    .O(miso),
    .T(1'b1)
  );

  IOBUF buf_dat3 (
    .IO(sd_dat[3]),
    .I(spi_cs),
    .O(),
    .T(1'b0)
  );

  always @(posedge clk) begin
    if (reset) begin
       miso_sync[0] <= 1'b0;
       miso_sync[1] <= 1'b0;
    end else begin
       miso_sync[0] <= miso;
       miso_sync[1] <= miso_sync[0];
    end
  end
endmodule

`default_nettype wire
