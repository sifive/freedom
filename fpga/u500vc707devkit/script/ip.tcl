#MIG
create_ip -vendor xilinx.com -library ip -name mig_7series -module_name vc707mig -dir $ipdir -force
set migprj [file join [pwd] $scriptdir {mig.prj}] 
set_property CONFIG.XML_INPUT_FILE $migprj [get_ips vc707mig]

puts "SCRIPTDIR $scriptdir"

#AXI_PCIE
create_ip -vendor xilinx.com -library ip -version 2.8 -name axi_pcie -module_name vc707axi_to_pcie_x1 -dir $ipdir -force
set_property -dict [list \
CONFIG.AXIBAR2PCIEBAR_0             {0x60000000} \
CONFIG.AXIBAR2PCIEBAR_1             {0x00000000} \
CONFIG.AXIBAR2PCIEBAR_2             {0x00000000} \
CONFIG.AXIBAR2PCIEBAR_3             {0x00000000} \
CONFIG.AXIBAR2PCIEBAR_4             {0x00000000} \
CONFIG.AXIBAR2PCIEBAR_5             {0x00000000} \
CONFIG.AXIBAR_0                     {0x60000000} \
CONFIG.AXIBAR_1                     {0xFFFFFFFF} \
CONFIG.AXIBAR_2                     {0xFFFFFFFF} \
CONFIG.AXIBAR_3                     {0xFFFFFFFF} \
CONFIG.AXIBAR_4                     {0xFFFFFFFF} \
CONFIG.AXIBAR_5                     {0xFFFFFFFF} \
CONFIG.AXIBAR_AS_0                  {true} \
CONFIG.AXIBAR_AS_1                  {false} \
CONFIG.AXIBAR_AS_2                  {false} \
CONFIG.AXIBAR_AS_3                  {false} \
CONFIG.AXIBAR_AS_4                  {false} \
CONFIG.AXIBAR_AS_5                  {false} \
CONFIG.AXIBAR_HIGHADDR_0            {0x7FFFFFFF} \
CONFIG.AXIBAR_HIGHADDR_1            {0x00000000} \
CONFIG.AXIBAR_HIGHADDR_2            {0x00000000} \
CONFIG.AXIBAR_HIGHADDR_3            {0x00000000} \
CONFIG.AXIBAR_HIGHADDR_4            {0x00000000} \
CONFIG.AXIBAR_HIGHADDR_5            {0x00000000} \
CONFIG.AXIBAR_NUM                   {1} \
CONFIG.BAR0_ENABLED                 {true} \
CONFIG.BAR0_SCALE                   {Gigabytes} \
CONFIG.BAR0_SIZE                    {4} \
CONFIG.BAR0_TYPE                    {Memory} \
CONFIG.BAR1_ENABLED                 {false} \
CONFIG.BAR1_SCALE                   {N/A} \
CONFIG.BAR1_SIZE                    {8} \
CONFIG.BAR1_TYPE                    {N/A} \
CONFIG.BAR2_ENABLED                 {false} \
CONFIG.BAR2_SCALE                   {N/A} \
CONFIG.BAR2_SIZE                    {8} \
CONFIG.BAR2_TYPE                    {N/A} \
CONFIG.BAR_64BIT                    {true} \
CONFIG.BASEADDR                     {0x50000000} \
CONFIG.BASE_CLASS_MENU              {Bridge_device} \
CONFIG.CLASS_CODE                   {0x060400} \
CONFIG.COMP_TIMEOUT                 {50us} \
CONFIG.Component_Name               {design_1_axi_pcie_1_0} \
CONFIG.DEVICE_ID                    {0x7111} \
CONFIG.ENABLE_CLASS_CODE            {true} \
CONFIG.HIGHADDR                     {0x53FFFFFF} \
CONFIG.INCLUDE_BAROFFSET_REG        {true} \
CONFIG.INCLUDE_RC                   {Root_Port_of_PCI_Express_Root_Complex} \
CONFIG.INTERRUPT_PIN                {false} \
CONFIG.MAX_LINK_SPEED               {2.5_GT/s} \
CONFIG.MSI_DECODE_ENABLED           {true} \
CONFIG.M_AXI_ADDR_WIDTH             {32} \
CONFIG.M_AXI_DATA_WIDTH             {64} \
CONFIG.NO_OF_LANES                  {X1} \
CONFIG.NUM_MSI_REQ                  {0} \
CONFIG.PCIEBAR2AXIBAR_0_SEC         {1} \
CONFIG.PCIEBAR2AXIBAR_0             {0x00000000} \
CONFIG.PCIEBAR2AXIBAR_1             {0xFFFFFFFF} \
CONFIG.PCIEBAR2AXIBAR_1_SEC         {1} \
CONFIG.PCIEBAR2AXIBAR_2             {0xFFFFFFFF} \
CONFIG.PCIEBAR2AXIBAR_2_SEC         {1} \
CONFIG.PCIE_BLK_LOCN                {X1Y1} \
CONFIG.PCIE_USE_MODE                {GES_and_Production} \
CONFIG.REF_CLK_FREQ                 {100_MHz} \
CONFIG.REV_ID                       {0x00} \
CONFIG.SLOT_CLOCK_CONFIG            {true} \
CONFIG.SUBSYSTEM_ID                 {0x0007} \
CONFIG.SUBSYSTEM_VENDOR_ID          {0x10EE} \
CONFIG.SUB_CLASS_INTERFACE_MENU     {Host_bridge} \
CONFIG.S_AXI_ADDR_WIDTH             {32} \
CONFIG.S_AXI_DATA_WIDTH             {64} \
CONFIG.S_AXI_ID_WIDTH               {4} \
CONFIG.S_AXI_SUPPORTS_NARROW_BURST  {false} \
CONFIG.VENDOR_ID                    {0x10EE} \
CONFIG.XLNX_REF_BOARD               {None} \
CONFIG.axi_aclk_loopback            {false} \
CONFIG.en_ext_ch_gt_drp             {false} \
CONFIG.en_ext_clk                   {false} \
CONFIG.en_ext_gt_common             {false} \
CONFIG.en_ext_pipe_interface        {false} \
CONFIG.en_transceiver_status_ports  {false} \
CONFIG.no_slv_err                   {false} \
CONFIG.rp_bar_hide                  {true} \
CONFIG.shared_logic_in_core         {false} ] [get_ips vc707axi_to_pcie_x1]



