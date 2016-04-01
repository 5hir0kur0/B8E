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
; The regexes in this file will replace all matches with ERRORS.
;
; Author: Jannik


; Additive arithmetic operations
$regex "se/\b(addc?|subb)\b\s+(\w+)\s*$/\g1 a, \g2/Missing 'a' as first operand!/igSM"

; Multiplicative arithmetic operations
$regex "se/\b(mul|div)\b\s+$/\g1 ab/Missing 'ab' as operand!/igSM"

; Logical operations
$regex "se/\b\(anl|[ox]rl)\b\s+(\/\w+)\s*$/\g1 c, \g2/Missing 'c' as first operand!/igSM"
$regex "se/\b\(anl|[ox]rl)\b\s+(@?r\d+)\s*$/\g1 a, \g2/Missing 'a' as first operand!/igSM"

; Swapping operations
$regex "se/\b(xchd?)\b\s*(\S+)\s*$/\g1 a, \g2/Missing 'a' as first operand!/igSM"

; Accumulator operations
$regex "se/\b(r[lr]c?|swap|da)\b\s*$/\g1 a/Missing 'a' as operand!/igSM"
