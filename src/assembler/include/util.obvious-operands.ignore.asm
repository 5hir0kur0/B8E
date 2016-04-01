; By including this file it is possible to only write
; out the operands that are needed to clearly identify 
; the instruction that should be used.
;
; Example:
;  anl @r1 -> anl a, @r1 
;                       OK
;  and #immediate -> anl a,      #immediate
;                 -> anl direct, #immediate
;                       UNCLEAR
;
; The regexes in this file will replace all matches SILENTLY.
;
; Author: Noxgrim


; Additive arithmetic operations
$regex "s/\b(addc?|subb)\b\s+(\w+)\s*$/\g1 a, \g2/igSM"

; Multiplicative arithmetic operations
$regex "s/\b(mul|div)\b\s+$/\g1 ab/igSM"

; Logical operations
$regex "s/\b\(anl|[ox]rl)\b\s+(\/\w+)\s*$/\g1 c, \g2/igSM"
$regex "s/\b\(anl|[ox]rl)\b\s+(@?r\d+)\s*$/\g1 a, \g2/igSM"

; Swapping operations
$regex "s/\b(xchd?)\b\s*(\S+)\s*$/\g1 a, \g2/igSM"

; Accumulator operations
$regex "s/\b(r[lr]c?|swap|da)\b\s*$/\g1 a/igSM"
