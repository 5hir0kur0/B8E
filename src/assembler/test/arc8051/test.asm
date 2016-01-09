; Test file for 8051 assembler
; This file contains every mnemonic/operand combination
; there is.
; The file will be assembled with the B8E assembler and
; the resulting hex file will be compared with the out-
; put of a 'legit' assembler.
;
; Author: Jannik

start:
   acall test_mov
   ; TODO: Add all other mnemonics
 ljmp start

test_mov:
   mov  @r0 ,  #4Dh
   mov  @r1 ,  #4Fh
   mov  @r0 ,    a
   mov  @r1 ,    a
   mov  @r0 ,   56h
   mov  @r2 ,   20h
   mov    a ,  #68h
   mov    a ,  @r0
   mov    a ,  @r1
   mov    a ,   r0
   mov    a ,   r1
   mov    a ,   r2
   mov    a ,   r3
   mov    a ,   r4
   mov    a ,   r5
   mov    a ,   r6
   mov    a ,   r7
   mov    a ,   61h
   mov    c ,   73h
   mov dptr ,#206Dh
   mov   r0 ,  #61h
   mov   r1 ,  #6Eh
   mov   r2 ,  #79h
   mov   r3 ,  #20h
   mov   r4 ,  #6Fh
   mov   r5 ,  #70h
   mov   r6 ,  #63h
   mov   r7 ,  #6Fh
   mov   r0 ,    a
   mov   r1 ,    a
   mov   r2 ,    a
   mov   r3 ,    a
   mov   r4 ,    a
   mov   r5 ,    a
   mov   r6 ,    a
   mov   r7 ,    a
   mov   r0 ,   64h
   mov   r1 ,   65h
   mov   r2 ,   73h
   mov   r3 ,   3Bh
   mov   r4 ,   20h
   mov   r5 ,   74h
   mov   r6 ,   68h
   mov   r7 ,   61h
   mov   74h,  #20h
   mov   69h,    c
   mov   73h,  @r0
   mov   20h,  @r1
   mov   68h,   r0
   mov   75h,   r1
   mov   72h,   r2
   mov   74h,   r3
   mov   69h,   r4
   mov   6Eh,   r5
   mov   67h,   r6
   mov   20h,   r7
   mov   6Dh,    a
   mov   65h,   21h
 ret
