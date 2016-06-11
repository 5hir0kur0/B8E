; Default include file for the B8E assembler.
; Some assembler features known from other assemblers
; wont be possible without the inclusion of this file.
;
; The file will be included automatically.
; To disable this, just set the "assembler.include-default-file" 
; setting to 'false'.
;
; Author: Noxgrim

; Convert any pseudo instructions/directives into valid assembler directives
$regex "s/^\s*\b(equ|set|[ix]?data|code|bit|end|include|line|file|org|d[bws]|if|else|elif|endif)\b(\s*?.*?$)/\$\g1\g2/igSM"

; Convert "infix" directives to "prefix" directives
$regex "s/^\s*([\S&&[^;]]+)\s+(equ|set|code|bit|[ix]?data)\s+(.*?)$/\$\g2 \g1 \g3/igSM"

; Make it possible to use ',' in data directives
$regex "cs/\s*,\s*/\s*[\$#\.]\s*d[bws]\s.*?$/ /igSM"

; Make it possible to write '$elseif' instead of '$elif'
$regex "s/^(\s*[\$#\.]\s*)elseif/\g1elif/igSM"

; Replace all "acc" references to the accumulator with "a"
$regex "s/\bacc\b/a/igSM"

; Make it possible to write "a, b" instead of "ab" in multiplicative operations
$regex "s/\b(?:mul|div)\b\s+a\s*,\s*b\s*$/lcall \g1/igSM"

; How to handle and optimize jumps/calls?
$if NOT -s "assembler.optimise-jumps"                  ; Do not optimize jumps
    $regex "s/\bjmp\b\s+(\w+)\s*$/ljmp \g1/igSM"
    $regex "s/\bcall\b\s+(\w+)\s*$/lcall \g1/igSM"
$elif -s "assembler.optimise-jumps.force"              ; Optimize every jump
    $regex "s/\b[als]jmp\b\s+(\w+)\s*$/jmp \g1/igSM"
    $regex "s/\b[al]call\b\s+(\w+)\s*$/call \g1/igSM"
$endif
