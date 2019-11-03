# MicrobitAlarm

This Program is made to control BBC micro:bit with speaker and servo motor from Bluetooth Wireless Serial Profile Communication.

Including Features :
* Encode MP2/MP3/AC3/WMA/AMAC3/AMAC6/FLAC/ALAC to 31.5KHz Sample Unsigned 8-bit Raw PCM audio
* Transmit the PCM audio to the micro:bit by Bluetooth 
* Setting alarm time

Format :
* `;m <FileSize> <BlockCount>\r\n` Set Music File
* `;d <BlockNum> <Base64Data>` Upload Music File
* `;a <Delta Second>\r\n` Set alarm
* `;n\r\n` On
* `;f\r\n` Off


FFmpeg-android : https://github.com/brarcher/ffmpeg-android-java

BluetoothSerial : https://github.com/harry1453/android-bluetooth-serial
