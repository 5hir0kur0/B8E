; Test file for 8051 assembler
; This file contains every mnemonic/operand combination
; there is.
; The file will be assembled with the B8E assembler and
; the resulting hex file will be compared with the out-
; put of a 'legit' assembler.
;
; Author: Noxgrim

start:
   acall test_mov
   acall test_data_transfer
   lcall test_arithmetic
   lcall test_bit_operations
   ajmp  test_jumps_conditional
   back:
 sjmp start
   jmp @a+dptr

test_mov:
   mov  @r0 ,  #4Dh
   mov  @r1 ,  #4Fh
   mov  @r0 ,    a
   mov  @r1 ,    a
   mov  @r0 ,   56h
   mov    a ,  #20h
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
   mov    c ,   6Eh
   mov dptr ,#6420h
   mov   r0 ,  #69h
   mov   r1 ,  #74h
   mov   r2 ,  #73h
   mov   r3 ,  #20h
   mov   r4 ,  #62h
   mov   r5 ,  #61h
   mov   r6 ,  #7Ah
   mov   r7 ,  #69h
   mov   r0 ,    a
   mov   r1 ,    a
   mov   r2 ,    a
   mov   r3 ,    a
   mov   r4 ,    a
   mov   r5 ,    a
   mov   r6 ,    a
   mov   r7 ,    a
   mov   r0 ,   6Ch
   mov   r1 ,   6Ch
   mov   r2 ,   69h
   mov   r3 ,   6Fh
   mov   r4 ,   6Eh
   mov   r5 ,   20h
   mov   r6 ,   6Fh
   mov   r7 ,   70h
   mov   63h,  #6Fh
   mov   64h,    c
   mov   65h,  @r0
   mov   73h,  @r1
   mov   3Bh,   r0
   mov   20h,   r1
   mov   64h,   r2
   mov   61h,   r3
   mov   7Ah,   r4
   mov   7Ah,   r5
   mov   6Ch,   r6
   mov   69h,   r7
   mov   6Eh,    a
   mov   67h,   21h
 ret

test_data_transfer:
   movc     a,@a+dptr
   movc     a,  @a+pc

   movx @dptr,      a
   movx     a,  @dptr
   movx   @r0,      a
   movx   @r1,      a
   movx     a,    @r0
   movx     a,    @r1

   swap     a

   xch      a,    @r0
   xch      a,    @r1
   xch      a,     r0
   xch      a,     r1
   xch      a,     r2
   xch      a,     r3
   xch      a,     r4
   xch      a,     r5
   xch      a,     r6
   xch      a,     r7
   xch      a,     7Eh

   xchd     a,    @r0
   xchd     a,    @r1
 ret

test_arithmetic:
   add   a,#4Dh
   add   a, 61h
   add   a,@r0
   add   a,@r1
   add   a, r0
   add   a, r1
   add   a, r2
   add   a, r3
   add   a, r4
   add   a, r5
   add   a, r6
   add   a, r7

   addc  a,#74h
   addc  a, 68h
   addc  a,@r0
   addc  a,@r1
   addc  a, r0
   addc  a, r1
   addc  a, r2
   addc  a, r3
   addc  a, r4
   addc  a, r5
   addc  a, r6
   addc  a, r7

   subb  a,#3Fh
   subb  a, 21h
   subb  a,@r0
   subb  a,@r1
   subb  a, r0
   subb  a, r1
   subb  a, r2
   subb  a, r3
   subb  a, r4
   subb  a, r5
   subb  a, r6
   subb  a, r7

   mul  ab

   div  ab
 ret

 test_bit_operations:
    da    a

    setb  c
    setb 41h
    
    clr   a
    clr   c
    clr  6Dh

    cpl   a
    cpl   c
    cpl  65h

    anl  2Ch,  a
    anl  20h,#76h
    anl   a ,#69h
    anl   a , 73h
    anl   a ,@r0
    anl   a ,@r1
    anl   a , r0
    anl   a , r1
    anl   a , r2
    anl   a , r3
    anl   a , r4
    anl   a , r5
    anl   a , r6
    anl   a , r7
    anl   c , 69h
    anl   c ,/74h
    
    orl  20h,  a
    orl  79h,#6Fh
    orl   a ,#75h
    orl   a , 72h
    orl   a ,@r0
    orl   a ,@r1
    orl   a , r0
    orl   a , r1
    orl   a , r2
    orl   a , r3
    orl   a , r4
    orl   a , r5
    orl   a , r6
    orl   a , r7
    orl   c , 20h
    orl   c ,/6Dh
    
    xrl  6Fh,  a
    xrl  6Dh,#6Dh
    xrl   a ,#79h
    xrl   a , 21h
    xrl   a ,@r0
    xrl   a ,@r1
    xrl   a , r0
    xrl   a , r1
    xrl   a , r2
    xrl   a , r3
    xrl   a , r4
    xrl   a , r5
    xrl   a , r6
    xrl   a , r7
    
    rl    a
    rlc   a
    rr    a
    rrc   a
 ret

 test_misc:
    nop
    
    push  42h
    pop   79h

    inc    a
    inc   74h
    inc  @r0
    inc  @r1
    inc   r0
    inc   r2
    inc   r3
    inc   r4
    inc   r5
    inc   r6
    inc   r7
    inc dptr

    dec    a
    dec   65h
    dec  @r0
    dec  @r1
    dec   r0
    dec   r2
    dec   r3
    dec   r4
    dec   r5
    dec   r6
    dec   r7
 reti

 test_jumps_conditional:
       cjne   a ,#48h,c00
   c00:cjne @r0 ,#65h,c01
   c05:cjne @r1 ,#68h,c06
   c03:cjne  r0 ,#61h,c04
   c07:cjne  r1 ,#73h,c08
   c01:cjne  r2 ,#61h,c02
   c08:cjne  r3 ,#21h,c09
   c04:cjne  r4 ,#63h,c05
   c06:cjne  r5 ,#65h,c07
   c02:cjne  r6 ,#64h,c03
   c09:cjne  r7 ,#20h,c0a
   c0a:
       djnz  3Ch,d00
   d03:djnz  r0 ,d04
   d01:djnz  r1 ,d02
   d06:djnz  r2 ,d07
   d05:djnz  r3 ,d06
   d00:djnz  r4 ,d01
   d04:djnz  r5 ,d05
   d02:djnz  r6 ,d03
   d07:djnz  r7 ,d08
   d08:
       jb    40h,b00
   b01:jbc   40h,b02
   b00:jnb   5Fh,b01
   b02:
       jc   j00
   j02:jnc  j03
   j01:jz   j02
   j00:jnz  j01
   j03:
 ljmp back
