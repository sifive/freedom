Freedom
=======

This repository contains the RTL created by SiFive for its Freedom E300 and U500
platforms. The Freedom E310 Arty FPGA Dev Kit implements the Freedom E300
Platform and is designed to be mapped onto an [Arty FPGA Evaluation
Kit](https://www.xilinx.com/products/boards-and-kits/arty.html). The Freedom
U500 VC707 FPGA Dev Kit implements the Freedom U500 Platform and is designed to
be mapped onto a [VC707 FPGA Evaluation
Kit](https://www.xilinx.com/products/boards-and-kits/ek-v7-vc707-g.html).
Both systems boot autonomously and can be controlled via an external debugger.

Please read the section corresponding to the kit you are interested in for
instructions on how to use this repo.


Freedom E310 Arty FPGA Dev Kit
------------------------------

The Freedom E310 Arty FPGA Dev Kit implements a Freedom E310 chip.

### How to build

The Makefile corresponding to the Freedom E310 Arty FPGA Dev Kit is
`Makefile.e300artydevkit` and it consists of two main targets:

- `verilog`: to compile the Chisel source files and generate the Verilog files.
- `mcs`: to create a Configuration Memory File (.mcs) that can be programmed
onto an Arty FPGA board.

To execute these targets, you can run the following commands:

```sh
$ make -f Makefile.e300artydevkit verilog
$ make -f Makefile.e300artydevkit mcs
```

These will place the files under `builds/e300artydevkit`.

Note that in order to run the `mcs` target, you need to have the `vivado`
executable on your `PATH`.

### Bootrom

The default bootrom consists of a program that immediately jumps to address
0x20400000, which is 0x00400000 bytes into the SPI flash memory on the Arty
board.


Freedom U500 VC707 FPGA Dev Kit
-------------------------------

The Freedom U500 VC707 FPGA Dev Kit implements the Freedom U500 platform.

### How to build

The Makefile corresponding to the Freedom U500 VC707 FPGA Dev Kit is
`Makefile.u500vc707devkit` and it consists of two main targets:

- `verilog`: to compile the Chisel source files and generate the Verilog files.
- `mcs`: to create a Configuration Memory File (.mcs) that can be programmed
onto an VC707 FPGA board.

To execute these targets, you can run the following commands:

```sh
$ make -f Makefile.u500vc707devkit verilog
$ make -f Makefile.u500vc707devkit mcs
```

These will place the files under `builds/u500vc707devkit`.

Note that in order to run the `mcs` target, you need to have the `vivado`
executable on your `PATH`.

### Bootrom

The default bootrom consists of a bootloader that loads a program off the SD
card slot on the VC707 board.
