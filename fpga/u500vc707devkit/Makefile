VIVADO ?= vivado
VIVADOFLAGS := \
	-nojournal -mode batch \
	-source script/board.tcl \
	-source script/prologue.tcl

bit := obj/system.bit
$(bit): script/impl.tcl  script/init.tcl
	VSRC_TOP=$(VSRC_TOP) EXTRA_VSRCS="$(EXTRA_VSRCS)" $(VIVADO) $(VIVADOFLAGS) -source script/init.tcl -source script/impl.tcl

.PHONY: bit
bit: $(bit)

mcs := obj/system.mcs
$(mcs): $(bit)
	$(VIVADO) $(VIVADOFLAGS) script/cfgmem.tcl -tclargs $@ $^

.PHONY: mcs
mcs: $(mcs)

.PHONY: clean
clean::
	rm -rf -- .Xil .ip_user_files *.os obj src/generated usage_statistics_webtalk.xml usage_statistics_webtalk.html *.log
