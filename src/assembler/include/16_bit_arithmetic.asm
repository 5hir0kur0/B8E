; 

; Adds the registers R1 and R0 to the registers 
; R3 and R2 and stores the result in R2, R1 and 
; R0. The register R3 will be cleared in the 
; process to keep continuity to the other 16 bit
; operations.
; The accumulator will be used in the process
; and pushed to the stack and poped after the
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
  JNC no_carry
    mov  r2,  01h
    sjmp finish 
  no_carry:
    mov  r2,  00h
 finish:
  mov  r3, 00h

  pop   a
 RET

; Substracts the registers R1 and R0 from the registers 
; R3 and R2 and stores the result in R3, R2, R1 and R0.
; The accumulator will be used in the process
; and pushed to the stack and poped after the
; operation is done.
; The reseulting number will always be 32 bit exept if a
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
  ; Substract the low byte
  mov   a, r2
  subb  a, r0
  push  a
  ; Substract the high byte
  mov   a, r3
  subb  a, r1

 $if -i subb16_16_bit_result
  mov  r1, a
  pop   a
  mov  r0, a
 $else
  jnc negative_subb
    mov r1, a
    pop  a
    mov r0, a
    mov r3, 00h
    mov r2, 00h
    sjmp finish_subb
  negative_subb:
    mov r3, a
    pop  a
    mov r2, a
    mov r1, 0FFh
    mov r0, 0FFh
 finish_subb:
 $endif
  pop a
 RET