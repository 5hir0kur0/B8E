; Default include file for the B8E assembler.
; Some assembler features known from other assemblers
; wont be possible without the inclusion of this file.
;
; The file will be included automatically.
; To disable this, just set the "assembler.include-default-file" 
; setting to 'false'.
;
; Author: Jannik

; Convert any pseudo instructions/directives into valid assembler directives
$regex "s/^\s*(equ|set|[ix]?data|code|bit|end|include|line|file|org|d[bws]|if|else|elif|endif)(\s*?.*?$)/\$\g1\g2/igSM"
; Convert "infix" directives to "prefix" directives
$regex "s/^\s*(\S+)\s+(equ|set|code|bit|[ix]?data)\s+?(.*?$)/\$\g2 \g1 \g3/igSM"
; Replace all "acc" refereces to the accumulator with "a"
$regex "s/\bacc\b/a/igSM"
; (Temporarily) replace jumps/calls that should be caulculated by the assembler with long jumps/calls.
$regex "s/\bjmp\b\s+(\w+)\s*$/ljmp \g1/igSM"
$regex "s/\bcall\b\s+(\w+)\s*$/lcall \g1/igSM"
