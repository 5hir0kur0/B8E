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
; The regexes in this file will replace all matches with WARN.
;
; Author: Jannik


; Additive arithmetic operations
$regex "sw/\b(addc?|subb)\b\s+(\w+)\s*$/\g1 a, \g2/Expected 'a' as first operand!/igSM"

; Multiplicative arithmetic operations
$regex "sw/\b(mul|div)\b\s+$/\g1 ab/Expected 'ab' as operand!/igSM"

; Logical operations
$regex "sw/\b\(anl|[ox]rl)\b\s+(\/\w+)\s*$/\g1 c, \g2/Expected 'c' as first operand!/igSM"
$regex "sw/\b\(anl|[ox]rl)\b\s+(@?r\d+)\s*$/\g1 a, \g2/Expected 'a' as first operand!/igSM"

; Swapping operations
$regex "sw/\b(xchd?)\b\s*(\S+)\s*$/\g1 a, \g2/Expected 'a' as first operand!/igSM"

; Accumulator operations
$regex "sw/\b(r[lr]c?|swap|da)\b\s*$/\g1 a/Expected 'a' as first operand!/igSM"
