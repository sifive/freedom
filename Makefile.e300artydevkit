# See LICENSE for license details.
base_dir := $(patsubst %/,%,$(dir $(abspath $(lastword $(MAKEFILE_LIST)))))
BUILD_DIR := $(base_dir)/builds/e300artydevkit
FPGA_DIR := $(base_dir)/fpga/e300artydevkit
MODEL := E300ArtyDevKitTop
PROJECT := sifive.freedom.everywhere.e300artydevkit
CONFIG_PROJECT := sifive.freedom.everywhere.e300artydevkit
CONFIG := E300ArtyDevKitConfig

rocketchip_dir := $(base_dir)/rocket-chip
sifiveblocks_dir := $(base_dir)/sifive-blocks
EXTRA_FPGA_VSRCS := \
	$(rocketchip_dir)/vsrc/AsyncResetReg.v \
	$(rocketchip_dir)/vsrc/DebugTransportModuleJtag.v \
	$(sifiveblocks_dir)/vsrc/SRLatch.v

include common.mk
