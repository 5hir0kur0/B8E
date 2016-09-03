; Containes some definitions ment for
; utility and making coding easier and
; faster.
;
; Author: Noxgrim

; Automatically expands
;       mov R_REG_DES, R_REG_SRC
; to
;       mov a, R_REG_SRC
;       mov R_REG_DES, a
$regex "s/\bmov\s+(r\d)\s*?,\s*?(r\d)/mov a,\2\nmov \1,a/iGS" 
