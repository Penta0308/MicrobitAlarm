#include "MicroBit.h"
#include "MicroBitUARTService.h"
#include "MicrobitAlarm.h"
#include "MicroBitHeapAllocator.h"

MicroBit uBit;
MicroBitDisplay display;


MicroBitPin audio(MICROBIT_ID_IO_P0, MICROBIT_PIN_P0, PIN_CAPABILITY_ALL);
MicroBitPin servo(MICROBIT_ID_IO_P2, MICROBIT_PIN_P2, PIN_CAPABILITY_ALL);



volatile int connected = 0;

volatile int servovalue = 90;

unsigned long alarmmil = -1L;

ManagedString recscr("");

int filesize;
int blockcount;

void onConnected(MicroBitEvent)
{
    display.scroll('C');
    connected = 1;
}

void onDisconnected(MicroBitEvent)
{
    display.scroll('D');
    connected = 0;
}

bool processline(ManagedString line, bool recordmode)
{
    char modchar = line.charAt(0);
    if(modchar == 'e') {
        return false;
    } else if(modchar == 'a') {
        recscr = "";
        alarmmil = atol(line.substring(1, line.length() - 1).toCharArray()) + uBit.systemTime();
        return true;
    } else if(recordmode) {
        recscr = recscr + line;
        return recordmode;
    }
    switch(modchar) {
        case 's':
            servovalue = atoi(line.substring(1, line.length() - 1).toCharArray());
            servo.setServoValue(servovalue, SERVO_RANGE, SERVO_N);
            uBit.sleep(30);
            break;
        case 'm':
            music(line.substring(1, line.length() - 1));
            break;
        case 'p':
            uBit.sleep(atoi(line.substring(1, line.length() - 1).toCharArray()));
            break;
    }
    return 0;
}

void music(ManagedString folder) {}

void alarm()
{
    alarmmil = -1L;
    ManagedString line("");
    for(int c = 0; c < recscr.length(); c++) {
        char charbuf[] = {recscr.charAt(c), '\0'};
        line = line + charbuf;
        if(charbuf[0] == '\n') {
            processline(line, false);
            uBit.sleep(1);
            line = "";
        }
    }
}

int main()
{
    uBit.init();
	
    uBit.messageBus.listen(MICROBIT_ID_BLE, MICROBIT_BLE_EVT_CONNECTED, onConnected);
    uBit.messageBus.listen(MICROBIT_ID_BLE, MICROBIT_BLE_EVT_DISCONNECTED, onDisconnected);
	
    uBit.sleep(30);
	
    servo.setAnalogPeriod(20);
    uBit.sleep(1);
    servo.setServoValue(servovalue, SERVO_RANGE, SERVO_N);
    audio.setAnalogValue(0);
    audio.setAnalogPeriodUs(32);
    uBit.sleep(30);
	
    MicroBitUARTService * uart = new MicroBitUARTService(uBit.ble->Instance(), 64, 32);

    display.scroll('R');

    const ManagedString eom = "\n";
    bool recordmode = false;

    while(true) {
        uBit.sleep(1);
        int gotc;
        ManagedString line("");
        while(connected) {
            if(alarmmil <= uBit.systemTime()) {
                alarm();
            }

            if(uart->rxBufferedSize() != 0) {
                gotc = uart->getc();
                char charbuf[] = {(char)gotc, '\0'};
                uart->send(charbuf);

                line = line + charbuf;
                if(gotc == '\n') {
                    recordmode = processline(line, recordmode);
                    break;
                }
            }
            uBit.sleep(1);
        }
    }
	
    delete uart;
	
    release_fiber();
}
