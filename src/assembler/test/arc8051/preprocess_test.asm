
mov me, me
;$include <default.asm> ; Double inclusion to test multiple unmodifiable regexes
$db "Hello World!"
$dw "Extra Large!"
$ds 3 "Multi"
do:
di equ 42
mi data 21

mi: di: mov mi, di

pi set 84

mov (21 + 21), (pi / 1.9999)
mov ((-2)^0.5), (2^1000000)

mov pi, pi

pi set 168

mov pi, pi

if 42 == (21 + 22)

mov 42, 42

endif

;$include "test.asm"
end

mov mi, mi
