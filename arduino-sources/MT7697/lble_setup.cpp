#include <LBLEPeriphral.h>
#include <LBLE.h>
#include "constants.hpp"
#include "lble_setup.hpp"
#include "Arduino.h"

std::vector<struct pin_lble_profile> PIN_LBLE_PROFILES;

void setup_lble()
{
    // setup for PIN
    PIN_LBLE_PROFILES.reserve(PIN_UUID_PROFILES_SIZE);
    for (int idx = 0; idx < PIN_UUID_PROFILES_SIZE; ++idx)
    {
        // Note that services and characteristics are registered
        // in the ctor of vector elements
        PIN_LBLE_PROFILES.push_back(&PIN_UUID_PROFILES[idx]);
    }

    Serial.begin(9600);

    LBLE.begin();
    while(!LBLE.ready())
        delay(100);
    Serial.println("BLE ready.");

    Serial.print("MAC address: ");
    Serial.println(LBLE.getDeviceAddress());


    // advertise
    LBLEAdvertisementData advertisement;
    advertisement.configAsConnectableDevice(DEVICE_NAME);
    LBLEPeripheral.setName(DEVICE_NAME);

    LBLEPeripheral.begin();
    LBLEPeripheral.advertise(advertisement);
}
