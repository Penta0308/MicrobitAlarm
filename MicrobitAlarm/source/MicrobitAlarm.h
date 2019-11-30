void music(ManagedString);

/*
`m<FolderName>\n` Play Music
`a<Delta Second>\n ~~~ \ne\n` Set alarm(transaction)
`s<Angle>\n` Set Servo Angle : s0 to s180
`p<ms>\n` Pause Millisecond
*/


#define SERVO_RANGE MICROBIT_PIN_DEFAULT_SERVO_RANGE
#define SERVO_N MICROBIT_PIN_DEFAULT_SERVO_CENTER

#define ALARMDEBUG 0