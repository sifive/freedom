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

Run the following commands to clone the repository and get started:

```sh
$ git clone https://github.com/sifive/freedom.git
$ cd freedom

#Run this command to update subrepositories used by freedom
$ git submodule update --init --recursive
```

Next, read the section corresponding to the kit you are interested in for
instructions on how to use this repo.

Software Requirement
--------------------

### After installing Ubuntu/Debian

Do not forget updating all packages.
```
sudo apt update
sudo apt upgrade
```

Install required additional packages.
```
sudo apt-get install autoconf automake autotools-dev curl libmpc-dev libmpfr-dev libgmp-dev libusb-1.0-0-dev gawk build-essential bison flex texinfo gperf libtool patchutils bc zlib1g-dev device-tree-compiler pkg-config libexpat-dev python wget

sudo apt-get install default-jre
```

### Install sbt, varilator and scala which are required for building from Chisel

Build and install sbt.
```
echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 642AC823
sudo apt-get update
sudo apt-get install sbt
```

Build and install varilator.
```
sudo apt-get install git make autoconf g++ flex bison
git clone http://git.veripool.org/git/verilator
cd verilator
git checkout -b verilator_3_922 verilator_3_922
unset VERILATOR_ROOT # For bash, unsetenv for csh
autoconf # To create ./configure script
./configure
make -j `nproc`
sudo make install
```

Install scala
```
sudo apt install scala
```

### Toolchain

To compile the bootloaders for both Freedom E300 Arty and U500 VC707
FPGA dev kits, the RISC-V software toolchain must be installed locally and
set the $(RISCV) environment variable to point to the location of where the
RISC-V toolchains are installed. You can build the toolchain from scratch
or download the tools here: https://www.sifive.com/products/tools/

After installing toolchain and vivado, you must set the environment variables.

If you have installed toolchain to
`/home/riscv/riscv64-elf-tc/bin/riscv64-unknown-elf-gcc`
then run the following command. Do not include /bin at the end of the string.

```sh
$ export RISCV=/home/riscv/riscv64-elf-tc
```

In order to run the `mcs` target in the next step, you need to have the `vivado`
executable on your `PATH`.

If vivado is installed to `/tools/Xilinx/Vivado/2016.4/bin`,
you can set the `PATH` with the following command.
```sh
$ export PATH=${PATH}:/tools/Xilinx/Vivado/2016.4/bin
```
Change the line above if the `vivado` is installed to
`/opt/Xilinx/Vivado/2016.4/bin` accordingly.

### Vivado license

Please acquire vivado license and install if you are using vc707 or vcu118 from logging in to Xilinx website with your account.


Type `$ ifconfig -a` to make sure that the network interface name is `eth0`. If not, the Vivado cannot recognize the license from the NIC interface when it is similar to `enp0s25`.

Must follow the bellow to rename the network interface:

```
$ sudo vi /etc/default/grub
```

Then add this line beneath those GRUB... lines:

```
GRUB_CMDLINE_LINUX="net.ifnames=0 biosdevname=0"
```

Then update the grub:
```
$ sudo grub-mkconfig -o /boot/grub/grub.cfg
$ sudo update-grub
```

And reboot the machine.
Check with `ifconfig` again if `eth0` is shown or not.


Freedom E300 Arty FPGA Dev Kit
------------------------------

The Freedom E300 Arty FPGA Dev Kit implements a Freedom E300 chip.

### How to build

The Makefile corresponding to the Freedom E300 Arty FPGA Dev Kit is
`Makefile.e300artydevkit` and it consists of two main targets:

- `verilog`: to compile the Chisel source files and generate the Verilog files.
- `mcs`: to create a Configuration Memory File (.mcs) that can be programmed
onto an Arty FPGA board.

To execute these targets, you can run the following commands:

```sh
$ make -f Makefile.e300artydevkit verilog
$ make -f Makefile.e300artydevkit mcs
```

Note: This flow requires Vivado 2017.1. Old versions are known to fail.

These will place the files under `builds/e300artydevkit/obj`.


### Bootrom

The default bootrom consists of a program that immediately jumps to address
0x20400000, which is 0x00400000 bytes into the SPI flash memory on the Arty
board.

### Using the generated MCS Image

For instructions for getting the generated image onto an FPGA and programming it with software using the [Freedom E SDK](https://github.com/sifive/freedom-e-sdk), please see the [Freedom E310 Arty FPGA Dev Kit Getting Started Guide](https://www.sifive.com/documentation/freedom-soc/freedom-e300-arty-fpga-dev-kit-getting-started-guide/).

Freedom U500 VC707 FPGA Dev Kit
-------------------------------

The Freedom U500 VC707 FPGA Dev Kit implements the Freedom U500 platform.

### How to build

The Makefile corresponding to the Freedom U500 VC707 FPGA Dev Kit is
`Makefile.vc707-u500devkit` and it consists of two main targets:

- `verilog`: to compile the Chisel source files and generate the Verilog files.
- `mcs`: to create a Configuration Memory File (.mcs) that can be programmed
onto an VC707 FPGA board.

To execute these targets, you can run the following commands:

```sh
$ make -f Makefile.vc707-u500devkit verilog
$ make -f Makefile.vc707-u500devkit mcs
```
If you do not have PCI Express Gen1/2/3 FMC Module run following commands:
```sh
$ make MODEL=VC707BaseShell -f Makefile.vc707-u500devkit verilog
$ make MODEL=VC707BaseShell -f Makefile.vc707-u500devkit mcs
```

Note: This flow requires Vivado 2016.4. Newer versions are known to fail.

These will place the files under `builds/vc707-u500devkit/obj`.

### Bootrom

The default bootrom consists of a bootloader that loads a program off the SD
card slot on the VC707 board.

### Linux boot Image

The bootable Linux image for vc707 is able to build from the link
[SD boot image](https://github.com/sifive/freedom-u-sdk).
