MEM equ 20h

MOV A, #01h

START: CJNE A, #01h, LES_OTHER_VERGLEICHS
       SETB MEM
       LES_OTHER_VERGLEICHS: CJNE A, #80h, SHIFT
                             CLR MEM

       SHIFT: JNB MEM, SHIFT_RIGHT
              RL A ; Rotate A left
              LJMP OUT
              SHIFT_RIGHT: RR A ; Rotate A right

       OUT: LCALL LOOP ; LCALL FTW!
            MOV P0, A
            RL A
            MOV P1, A
            RL A
            MOV P2, A
            RL A
            MOV P3, A
            MOV A, P0

       JMP START

ENDLBL: LJMP ENDLBL

LOOP: MOV R7, #02h
SUBL: DJNZ R7, SUBL
      RET
END
