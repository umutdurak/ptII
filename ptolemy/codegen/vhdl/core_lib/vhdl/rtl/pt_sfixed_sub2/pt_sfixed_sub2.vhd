--Ptolemy VHDL Code Generation Core Library
--pt_sfixed_sub2: 	
--2 input adder for signed fixed point operands. 
--Latency programmable from 1 to 5.
--Parametrizeable to any size fixed point operation.
--Uses truncate and wrap on overflow.					
--Posedge clock.
--Author: Vinayak Nagpal

library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;
library ieee_proposed;
use ieee_proposed.math_utility_pkg.all;
use ieee_proposed.fixed_pkg.all;
--use work.pt_utility.all;

entity pt_sfixed_sub2 is
	generic
	(
		INPUTA_HIGH			:	integer		:= 15;
		INPUTA_LOW			:	integer		:= 0;
		INPUTB_HIGH			:	integer		:= 15;
		INPUTB_LOW			:	integer		:= 0;
		OUTPUT_HIGH			:	integer		:= 16;
		OUTPUT_LOW			:	integer		:= 0;
		LATENCY				: 	integer		:= 3
	) ;
	port
	(
		clk				: IN std_logic;
		A				: IN std_logic_vector (INPUTA_HIGH-INPUTA_LOW DOWNTO 0) ;	
		B				: IN std_logic_vector (INPUTB_HIGH-INPUTB_LOW DOWNTO 0) ;
		OUTPUT			: OUT std_logic_vector (OUTPUT_HIGH-OUTPUT_LOW DOWNTO 0) 
	) ;
end pt_sfixed_sub2;


ARCHITECTURE behave OF pt_sfixed_sub2 IS
--Constants
--Type Declarations
TYPE DELAYLINE is ARRAY (1 to LATENCY) of sfixed (sfixed_high(INPUTA_HIGH,INPUTA_LOW,'-',INPUTB_HIGH,INPUTB_LOW) DOWNTO sfixed_low(INPUTA_HIGH,INPUTA_LOW,'-',INPUTB_HIGH,INPUTB_LOW) ;

--Signal Declarations
SIGNAL delay 	: 	DELAYLINE;	 
--Input A
signal As 		:	sfixed (INPUTA_HIGH DOWNTO INPUTA_LOW);
--Input B
signal Bs 		:	sfixed (INPUTB_HIGH DOWNTO INPUTB_LOW);
SIGNAL DIFFs	 	:	sfixed (OUTPUT_HIGH DOWNTO OUTPUT_LOW);

BEGIN
As <= to_sfixed(A,As'high,As'low);
Bs <= to_sfixed(B,Bs'high,Bs'low);

OUTPUT 	<= 	to_slv(DIFFs);
DIFFs 	<=		resize(	arg				=>	delay(LATENCY),
					left_index		=> 	OUTPUT_HIGH,
					right_index		=>	OUTPUT_LOW,
					round_style		=>	fixed_truncate,
					overflow_style	=>	fixed_wrap);

subtract : process(clk)
begin
	if clk'event and clk = '1' then
		delay(1) <= As-Bs;
		for i in 1 to LATENCY loop
			delay(i+1)<=delay(i);
		end loop;	
	end if;
end process subtract ;
END behave ;
