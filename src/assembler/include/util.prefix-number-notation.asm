; By including this file it is possible to use
; prefix notation instead of suffix notation on
; numbers.
;
; Possible prefixes:
;
; Prefix | Suffix    | Numeral System
; -------|-----------|----------------
; '0b'   | 'b'       | binary
; '0'    | 'o' / 'q' | octal
;  no    | 'd' / no  | decimal
; '0x'   | 'h'       | hexadecimal
;
; Note: Prefixes will override suffixes so
;           0b012h
;       will be processed as a binary number.
;
; To highlight prefixed numbers properly new
; pattern must be added to the syntax theme.
;
; Author: Noxgrim


; Replace octal numbers
; Substitute octal numbers first to prevent the
; substitution of previously replaced numbers. 
$regex "s/\b0(\d\w*)\b/$1o/igSM"

; Replace binary numbers
$regex "s/\b0b(\w+)\b/$1b/igSM"

; Replace decimal numbers
$regex "s/\b(?:0d)?(\d\w*)\b/$1d/igSM"

; Replace hexadecimal numbers
; Note: The prefix '0x' is natively supported by
;       the assembler.
$regex "s/\b0x(\w+)\b/$1h/igSM"
