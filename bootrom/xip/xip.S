// See LICENSE for license details.
// Execute in place
// Jump directly to XIP_TARGET_ADDR

  .text
  .option norvc
  .globl _start
_start:
  j 1f
  nop
  nop
#ifdef CONFIG_STRING
  .word cfg_string
#else
  .word 0  // Filled in by GenerateBootROM in Chisel
#endif

1:
  li t0, XIP_TARGET_ADDR
  jr t0

  .section .rodata
#ifdef CONFIG_STRING
cfg_string:
  .incbin CONFIG_STRING
#endif
