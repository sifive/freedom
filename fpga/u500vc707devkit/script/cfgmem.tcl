lassign $argv mcsfile bitfile datafile

set iface bpix16
set size 128
set bitaddr 0x3000000

write_cfgmem -format mcs -interface $iface -size $size \
  -loadbit "up ${bitaddr} ${bitfile}" \
  -loaddata [expr {$datafile ne "" ? "up 0x400000 ${datafile}" : ""}] \
  -file $mcsfile -force
