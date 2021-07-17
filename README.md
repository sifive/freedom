# Freedom Repository

This repository contains the RTL created by SiFive for its Freedom E300 and U500
platforms. The Zybo Dev Kit 
implement the Freedom E300 Platform.

Note: The branch [freedom_zybo](https://github.com/gongqingfeng/freedom/tree/freedom_zybo)  is for Zybo Dev Kit. If you use Ebaz4205 FPGA Board, please switch to the branch [ebaz4205](https://github.com/gongqingfeng/freedom/tree/ebaz4205).


Please read the section corresponding to the kit you are interested in for
instructions on how to use this repo.

## Software Requirement
--------------------
To compile the bootloaders for Freedom E300 Zybo, the RISC-V software toolchain must be installed locally and
set the $(RISCV) environment variable to point to the location of where the
RISC-V toolchains are installed. You must pay attention to the version, or 
you may meet much trouble.

First, you should download the repository(it may take much time. Maybe vpn need?):
```sh
$ git clone --recursive https://github.com/gongqingfeng/freedom.git
```

Second, you should enter the work dir like this:
```sh
$ cd freedom
# check your branch
git branch
# if your branch is not freedom_zybo, you should switch to the branch freedom_zybo
git checkout freedom_zybo
```

Optionally, if you did not compile the toolchain and you do like this(it will take much time):
```sh
$ cd rocket-chip/riscv-tools
$ export RISCV=/yout/install/toolchain/location # eg: export RISCV=/home/xx/xxx/risc-v_dev/tools
$ export MAKEFLAGS="$MAKEFLAGS -jN" # Assuming you have N cores on your host system
$ ./build.sh
```
Ubuntu packages needed:
```sh
$ sudo apt-get install autoconf automake autotools-dev curl libmpc-dev libmpfr-dev libgmp-dev libusb-1.0-0-dev gawk build-essentia
```
OK! Let's go ahead!

## Freedom E300 Zybo dev Kit

The Freedom E300 Dev Kit implements a Freedom E300 chip.

### How to build
Noets: I use Ubuntu16.04

Install java:
```sh
$ sudo apt install openjdk-8-jdk
```

Install sbt:
```sh
$ echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | sudo tee /etc/apt/sources.list.d/sbt.list
$ echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | sudo tee /etc/apt/sources.list.d/sbt_old.list
$ curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | sudo apt-key add
$ sudo apt-get update
$ sudo apt-get install sbt
```

if you occur problem like this:
```sh
error: error while loading package, Missing dependency 'object java.lang.Object in compiler mirror', required by
```
you should switch java version to 8:
```sh
$ sudo update-alternatives --config java
$ sudo update-alternatives --config javac
```

If you are not in freedom directory, please enter the dir like this:
```sh
$ cd freedom
```

The Makefile corresponding to the Freedom E300 Zybo Dev Kit is
`Makefile.e300zybodevkit` and it consists of several targets:

- `verilog`: to compile the Chisel source files and generate the Verilog files.
- `project`: to compile the Chisel source files and generate the vivado project.
- `vivado`: to launch the vivado project with GUI mode. So you can systhesis, implement and generate bitstream by yourself.

To execute these targets, you can run the following commands:

```sh
$ make -f Makefile.e300zybodevkit verilog
$ make -f Makefile.e300zybodevkit project
$ make -f Makefile.e300zybodevkit vivado
```
Note1: This lab tested within vivado 2016.2.

Note2: Before you generate bitstream, you are supposed to add the `freedom/fpga-shells/xilinx/ebaz4205/tcl/no_connect.tcl` file to your Bitstream Settings's tcl.pre blank line. Otherwise, you will get errors.

The vivado project files place under `builds/e300zybodevkit` and the *.v files place under `builds/e300zybodevkit/obj`.

Note that in order to run the `projest` and `vivado`target, you need to have the `vivado`
executable on your `PATH`.

### Bootrom

The default bootrom consists of a program that can blink three LED from address 0x0001FFFF on the Zybo Dev Kit.