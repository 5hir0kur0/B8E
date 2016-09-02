; This file includes some subroutines to calculate
; the basic arithmetic operations with 16 bit
; operands.
; Most of them are just modified routines from the
; 8052.com website.
;
; Authors: Webmasters of 8052.com (?), Jorg Rockstroh,
;          Noxgrim

; Adds the registers R1 and R0 to the registers
; R3 and R2 and stores the result in R2, R1 and
; R0. The register R3 will be cleared in the
; process to keep continuity to the other 16 bit
; operations.
; The accumulator will be used in the process
; and pushed to the stack and popped after the
; operation is done.
;
;   FF^3 FF^2 FF^1 FF^0
;              R1   R0
; +            R3   R2
; ---------------------
; =  R3   R2   R1   R0
;
; Usage:
;    25 bytes of code memory
;     2 bytes of stack
; Thanks to 8052.com for providing the tutorial.
ADD_16:
  push  a
  ; Calculate the low byte
  mov   a, r0
  add   a, r2
  push  a     ; Save result for later
  ; Calculate the high byte
  mov   a, r1
  addc  a, r3
  ; Prepare output
  mov  r1,  a
  pop   a
  mov  r0, a
  JNC add16_no_carry
    mov  r2,  01h
    sjmp add16_finish 
  add16_no_carry:
    mov  r2,  00h
 add16_finish:
  mov  r3, 00h

  pop   a
 RET


; Subtracts the registers R1 and R0 from the registers 
; R3 and R2 and stores the result in R3, R2, R1 and R0.
; The accumulator will be used in the process
; and pushed to the stack and popped after the
; operation is done.
; The resulting number will always be 32 bit except if a
; symbol called 'subb16_16_bit_result' is set to a non
; 0 value.
;
;   FF^3 FF^2 FF^1 FF^0
;              R1   R0
; -            R3   R2
; ---------------------
; =  R3   R2   R1   R0
;
; Usage:
;    32 bytes of code memory
;     2 bytes of stack
; Thanks to 8052.com for providing the tutorial.
SUBB_16:
  push  a

  clr   c
  ; Subtract the low byte
  mov   a, r2
  subb  a, r0
  push  a
  ; Subtract the high byte
  mov   a, r3
  subb  a, r1

 $if -i subb16_16_bit_result
  mov  r1, a
  pop   a
  mov  r0, a
 $else
  jnc subb16_negative
    mov r1, a
    pop  a
    mov r0, a
    mov r3, 00h
    mov r2, 00h
    sjmp subb16_finish
  subb16_negative:
    mov r3, a
    pop  a
    mov r2, a
    mov r1, 0FFh
    mov r0, 0FFh
 subb16_finish:
 $endif
  pop a
 RET


; Multiply the registers R1 and R0 by the registers
; R3 and R2 and stores the result in R3, R2, R1 and R0.
; The accumulator, and the B register as well as all
; R-registers will be used in the process
; and pushed to the stack and popped after the
; operation is done.
;
;   FF^3 FF^2 FF^1 FF^0
;              R1   R0
; *            R3   R2
; ---------------------
; =  R3   R2   R1   R0
;
; Usage:
;    -- bytes of code memory
;     6 bytes of stack
; Thanks to 8052.com for providing the tutorial.
MUL16:

 ; Push needed registers on the stack
  push a
  push b
  mov  a, r4
  push a
  mov  a, r5
  push a
  mov  a, r6
  push a
  mov  a, r7
  push a

 ; Multiply R2 by R0
  mov  a, r2
  mov  b, r0
  mul ab
  mov r5, b
  mov r4, a

  ; Multiply R2 by R1
  mov   a,  r2
  mov   b,  r1
  mul  ab
  add   a,  r5
  mov  r5,   a
  mov   a,   b
  addc  a, #00h
  mov  r6,   a
  mov   a, #00h
  addc  a, #00h
  mov  r7,   a

  ; Multiply R3 by R0
  mov   a,  r3
  mov   b,  r0
  mul  ab
  add   a,  r5
  mov  r5,   a
  mov   a,   b
  addc  a,  r6
  mov  r6,   a
  mov   a, #00h
  addc  a,  r7
  mov  r7,   a

  ; Multiply R3 by R1
  mov   a,  r3
  mov   b,  r1
  mul  ab
  add   a,  r6
  mov  r6,   a
  mov   a,   b
  addc  a,  r7
  mov  r7,   a

  ; Move result into result registers
  mov r0, r4
  mov r1, r5
  mov r2, r6
  mov r3, r7

 ; Pop the values result from the stack
  pop a
  mov r7, a
  pop a
  mov r6, a
  pop a
  mov r5, a
  pop a
  mov r4, a
  pop b
  pop a
 RET


$END
; Divide the registers R1 and R0 by the registers
; R3 and R2 and stores the result in R3, R2, R1 and R0.
; The accumulator, and the B register as well as most
; R-registers (from 0 to 5) will be used in the process
; and pushed to the stack and popped after the
; operation is done.
;
;   FF^3 FF^2 FF^1 FF^0
;              R1   R0
; /            R3   R2
; ---------------------
; %            R1   R0
; =            R3   R2
;
; Usage:
;    -- bytes of code memory
;     4 bytes of stack
; Thanks to 8052.com for providing the tutorial.
DIV16:
  ; Push needed registers on the stack
   push a
   push b
   mov  a, r4
   push a
   mov  a, r5
   push a

   ; Initialize
   clr  c
   mov r4, #00h
   mov r5, #00h
   mov  b, #00h
   ; Calculate
 div16_sub1:
   inc  b
   mov  a, r2
   rlc  a
   mov r2,  a
   mov  a, r3
   rlc  a
   mov r3,  a
   jnc div16_sub1
 div16_sub2:
   mov   a ,  r3
   rrc   a
   mov  r3 ,   a
   mov   a ,  r2
   rrc   a
   mov  r2 ,   a
   clr   c
   mov  07h,  r1
   mov  06h,  r0
   mov   a ,  r0
   subb  a ,  r2
   mov  r0 ,   a
   mov   a ,  r1
   subb  a ,  r3
   mov  r1 ,   a
   jnc div16_sub3
     mov   r1 , 07h
     mov   r0 , 06h
 div16_sub3:
   cpl  c
   mov  a, r4
   rlc  a
   mov r4,  a
   mov  a, r5
   rlc  a
   mov r5,  a
   djnz b,  div16_sub2
   ; Clean up
   mov r3, 05h
   mov r2, 04h

  ; Pop the values result from the stack
   pop a
   mov r5, a
   pop a
   mov r4, a
   pop b
   pop a
 RET

