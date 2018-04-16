// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2011-2018 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0
// author jerry73204@gmail.com (Lin, Hsiang-Jui)
// author az6980522@gmail.com (Yuan, Yu-Yuan)

#include <LBLEPeriphral.h>
#include <LBLE.h>
#include <Servo.h>

#include "constants.hpp"
#include "lble_setup.hpp"
LBLEPinSetup PIN_SETUP;
Servo servo[SERVO_SIZE];
unsigned long timer;

void setup()
{
    pinMode(BTN_PIN, INPUT);
    pinMode(LED_PIN, OUTPUT);
    PIN_SETUP.begin();
    timer = millis();
}

void loop()
{
    if (digitalRead(BTN_PIN))
    {
        Serial.println("disconnect all!");
        LBLEPeripheral.disconnectAll();
    }
    if (LBLEPeripheral.connected() == 0)
    {
        Serial.println("No connected device");
        for(int pin = 0; pin < SERVO_SIZE; pin++)
            servo[pin].detach();
        while(LBLEPeripheral.connected() == 0)
        {
            digitalWrite(LED_PIN, HIGH);
            delay(100);
            digitalWrite(LED_PIN, LOW);
            delay(100);
        }
        Serial.println("Connected, press USR BTN to disconnect");
        digitalWrite(LED_PIN, LOW);
    }
    else
    {
        for (int idx = 0; idx < PIN_UUID_PROFILES_SIZE; idx += 1)
        {
            auto& lble_ref = PIN_SETUP.PIN_LBLE_PROFILES[idx];
            const int pin = lble_ref.pin;
            char info[64];
            int data;
            int duration;
            int frequency;

            if (lble_ref.mode_char->isWritten())
            {
                lble_ref.mode = lble_ref.mode_char->getValue();
                switch (lble_ref.mode)
                {
                    case MODE_ANALOG_INPUT:
                    case MODE_DIGITAL_INPUT:
                        pinMode(pin, INPUT);
                        servo[pin].detach();
                        break;
                    case MODE_ANALOG_OUTPUT:
                        break;
                    case MODE_DIGITAL_OUTPUT:
                        pinMode(pin, OUTPUT);
                        servo[pin].detach();
                        break;
                    case MODE_SERVO:
                        servo[pin].attach(pin);
                        break;
                }
            }

            switch (lble_ref.mode) 
            {
                case MODE_ANALOG_INPUT:
                    data = analogRead(pin);
                    if ((millis() - timer) >= SEND_PERIOD)
                    {
                        lble_ref.data_char->setValue(data);
                        timer = millis();
                        sprintf(info, "Pin %d in analog input mode, send data %d", pin, data);
                        Serial.println(info);
                    }
                    break;
                case MODE_DIGITAL_INPUT:
                    data = digitalRead(pin);
                    if ((millis() - timer) >= SEND_PERIOD)
                    {
                        lble_ref.data_char->setValue(data);
                        timer = millis();
                        sprintf(info, "pin %d in digital input mode, send data %d", pin, data);
                        Serial.println(info);
                    }
                    break;
                case MODE_ANALOG_OUTPUT:
                    if (lble_ref.data_char->isWritten())
                    {
                        data = lble_ref.data_char->getValue();
                        data *= -1;
                        analogWrite(pin, data);
                        sprintf(info, "Pin %d in analog output mode, receive data %d", pin, data);
                        Serial.println(info);
                    }
                    break;
                case MODE_DIGITAL_OUTPUT:
                    if (lble_ref.data_char->isWritten())
                    {
                        data = lble_ref.data_char->getValue();
                        data *= -1;
                        digitalWrite(pin, (data >= 1) ? HIGH : LOW);
                        sprintf(info, "Pin %d in digital output mode, receive data %d", pin, data);
                        Serial.println(info);
                    }
                    break;
                case MODE_SERVO:
                    if (lble_ref.data_char->isWritten())
                    {
                        data = lble_ref.data_char->getValue();
                        data *= -1;
                        servo[pin].write(data);
                        sprintf(info, "Pin %d in servo mode, receive data %d", pin, data);
                        Serial.println(info);
                    }
                    break;
                case MODE_SONIC:
                    pinMode(pin, OUTPUT);
                    digitalWrite(pin, LOW);
                    delayMicroseconds(2);
                    digitalWrite(pin, HIGH);
                    delayMicroseconds(5);
                    digitalWrite(pin, LOW);
                    pinMode(pin, INPUT);
                    duration = pulseIn(pin, HIGH);
                    data = duration*0.034/2;
                    // Serial.print("Distance: ");
                    // Serial.println(distance);
                    if ((millis() - timer) >= SEND_PERIOD)
                    {
                        lble_ref.data_char->setValue(data);
                        timer = millis();
                        sprintf(info, "Pin %d in ultrasonic mode, send data %d", pin, data);
                        Serial.println(info);
                    }
                    break;
                case MODE_BUZZER:
                    if (lble_ref.data_char->isWritten())
                    {
                        data = lble_ref.data_char->getValue();
                        data *= -1;
                        // first 2 bytes is frequency and the last 2 bytes is duration
                        frequency = data >> 16;
                        duration = data & 0xffff;
                        tone(pin, frequency, duration);
                        sprintf(info, "Pin %d in buzzer mode, receive frequency: %d, duration: %d", pin, frequency, duration);
                        Serial.println(info);
                    }
                    break;
                    
                // default:
                //     sprintf(info, "Pin %d in invalid mode", pin);
                //     Serial.println(info);
                //     break;
            }
        }
    }
}

