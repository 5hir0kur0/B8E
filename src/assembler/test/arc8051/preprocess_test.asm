
mov me, me
$include <default.asm> ; Double inclusion to test multiple unmodifiable regexes
$db "Hello World!"
$dw "Extra Large!"
$ds 3 "Multi"
do:
di equ 42
mi data 21

mi: di: mov mi, di

pi set 84

mov (21 + 21), (pi / 1.9999)

mov pi, pi

pi set 168

mov pi, pi

;$include "test.asm"
end

mov mi, mi