#include "MicroBit.h"
#include "MicroBitUARTService.h"
#include "MicrobitAlarm.h"
#include "MicroBitHeapAllocator.h"
//#include "SDFileSystem.h"

MicroBit uBit;
MicroBitDisplay display;
//MicroBitUARTService * uart;

MicroBitPin audio(MICROBIT_ID_IO_P0, MICROBIT_PIN_P0, PIN_CAPABILITY_ALL);
MicroBitPin servo(MICROBIT_ID_IO_P2, MICROBIT_PIN_P2, PIN_CAPABILITY_ALL);

//SDFileSystem sd(MICROBIT_PIN_P15, MICROBIT_PIN_P14, MICROBIT_PIN_P13, MICROBIT_PIN_P16, "sd");

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
    //display.scroll(modchar);

    if(modchar == 'e') {
#if ALARMDEBUG
        display.scroll(recscr);
#endif
        return false;
    } else if(modchar == 'a') {
        recscr = "";
        alarmmil = atol(line.substring(1, line.length() - 1).toCharArray()) + uBit.systemTime();
#if ALARMDEBUG
        display.scroll((int )alarmmil);
#endif
        return true;
    } else if(recordmode) {
#if ALARMDEBUG
        display.scroll(line);
#endif
        recscr = recscr + line;
        return recordmode;
    }    //메인 파서
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

void music(ManagedString folder)
{
    //TODO
//    sd.erase(0, sd.get_erase_size());
//    sd.program(block, 0, 512);
}

void alarm()
{
#if ALARMDEBUG
    display.scroll("A");
    display.scroll(recscr);
#endif
    alarmmil = -1L;
    ManagedString line("");
    for(int c = 0; c < recscr.length(); c++) {
        char charbuf[] = {recscr.charAt(c), '\0'};
        line = line + charbuf;
        if(charbuf[0] == '\n') {
#if ALARMDEBUG
            display.scroll(line);
#endif
            processline(line, false);
            uBit.sleep(1);
            line = "";
        }
    }
}

int main()
{
    uBit.init();

    //if ( 0 != sd.init()) display.scroll("SD ERROR");

    uBit.messageBus.listen(MICROBIT_ID_BLE, MICROBIT_BLE_EVT_CONNECTED, onConnected);
    uBit.messageBus.listen(MICROBIT_ID_BLE, MICROBIT_BLE_EVT_DISCONNECTED, onDisconnected);
    //uBit.messageBus.listen(MICROBIT_ID_BUTTON_A, MICROBIT_BUTTON_EVT_CLICK, onButtonA);

    uBit.sleep(30);
    //servo.setAnalogValue(0);
    servo.setAnalogPeriod(20);
    uBit.sleep(1);
    servo.setServoValue(servovalue, SERVO_RANGE, SERVO_N);
    audio.setAnalogValue(0);
    audio.setAnalogPeriodUs(32);
    uBit.sleep(30);


    //while(1)
    //{
    //    uBit.sleep(30);
    //}

    //display.scroll('P');

    //BLE blemodule = uBit.ble->Instance();

    //display.scroll(uBit.ble->init());

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
                //give Alarm
                alarm();
            }
            //display.scroll(uart->rxBufferedSize());
            if(uart->rxBufferedSize() != 0) {
                gotc = uart->getc();
                char charbuf[] = {(char)gotc, '\0'};
#if ALARMDEBUG
                display.scroll(gotc);
#endif
                uart->send(charbuf);
                //uart->send(uart->readUntil(eom));
                line = line + charbuf;
                if(gotc == '\n') {
                    //display.scroll(line);
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