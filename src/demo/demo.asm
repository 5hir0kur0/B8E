MOV A, #01h

START: CJNE A, #01h, LES_OTHER_VERGLEICHS
       SETB 20h
       LES_OTHER_VERGLEICHS: CJNE A, #80h, SHIFT
                             CLR 20h

       SHIFT: JNB 20h, SHIFT_RIGHT
              RL A ; Rotate A left
              LJMP OUT
              SHIFT_RIGHT: RR A ; Rotate A right

       OUT: LCALL LOOP ; LCALL FTW!
            MOV 80h, A  ; P0
            RL A
            MOV 90h, A  ; P1
            RL A
            MOV 0A0h, A ; P2
            RL A
            MOV 0B0h, A ; P3
            MOV A, 80h  ; P0

       LJMP START

ENDLBL: LJMP ENDLBL

LOOP: MOV R7, #02h
SUBL: DJNZ R7, SUBL
      RET
