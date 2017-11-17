#ifndef __MT7697_CONSTANTS_HPP__
#define __MT7697_CONSTANTS_HPP__

#define PIN_UUID_PROFILES_SIZE 18

// The MT7697 compiler creates seperate static linked libraries
// and link to together.
// We need to "extern" every thing to make the compiler happy.

struct pin_uuid_profile
{
    int pin;
    const char *service_uuid;
    const char *mode_char_uuid;
    const char *data_char_uuid;
};

extern const char *DEVICE_NAME;

// extern const char *I2C_SERVICE;
// extern const char *I2C_READ_CHARACTERISTIC;
// extern const char *I2C_WRITE_CHARACTERISTIC;

extern const char *PIN00_SERVICE;
extern const char *PIN00_MODE_CHARACTERISTIC;
extern const char *PIN00_DATA_CHARACTERISTIC;

extern const char *PIN01_SERVICE;
extern const char *PIN01_MODE_CHARACTERISTIC;
extern const char *PIN01_DATA_CHARACTERISTIC;

extern const char *PIN02_SERVICE;
extern const char *PIN02_MODE_CHARACTERISTIC;
extern const char *PIN02_DATA_CHARACTERISTIC;

extern const char *PIN03_SERVICE;
extern const char *PIN03_MODE_CHARACTERISTIC;
extern const char *PIN03_DATA_CHARACTERISTIC;

extern const char *PIN04_SERVICE;
extern const char *PIN04_MODE_CHARACTERISTIC;
extern const char *PIN04_DATA_CHARACTERISTIC;

extern const char *PIN05_SERVICE;
extern const char *PIN05_MODE_CHARACTERISTIC;
extern const char *PIN05_DATA_CHARACTERISTIC;

extern const char *PIN06_SERVICE;
extern const char *PIN06_MODE_CHARACTERISTIC;
extern const char *PIN06_DATA_CHARACTERISTIC;

extern const char *PIN07_SERVICE;
extern const char *PIN07_MODE_CHARACTERISTIC;
extern const char *PIN07_DATA_CHARACTERISTIC;

extern const char *PIN08_SERVICE;
extern const char *PIN08_MODE_CHARACTERISTIC;
extern const char *PIN08_DATA_CHARACTERISTIC;

extern const char *PIN09_SERVICE;
extern const char *PIN09_MODE_CHARACTERISTIC;
extern const char *PIN09_DATA_CHARACTERISTIC;

extern const char *PIN10_SERVICE;
extern const char *PIN10_MODE_CHARACTERISTIC;
extern const char *PIN10_DATA_CHARACTERISTIC;

extern const char *PIN11_SERVICE;
extern const char *PIN11_MODE_CHARACTERISTIC;
extern const char *PIN11_DATA_CHARACTERISTIC;

extern const char *PIN12_SERVICE;
extern const char *PIN12_MODE_CHARACTERISTIC;
extern const char *PIN12_DATA_CHARACTERISTIC;

extern const char *PIN13_SERVICE;
extern const char *PIN13_MODE_CHARACTERISTIC;
extern const char *PIN13_DATA_CHARACTERISTIC;

extern const char *PIN14_SERVICE;
extern const char *PIN14_MODE_CHARACTERISTIC;
extern const char *PIN14_DATA_CHARACTERISTIC;

extern const char *PIN15_SERVICE;
extern const char *PIN15_MODE_CHARACTERISTIC;
extern const char *PIN15_DATA_CHARACTERISTIC;

extern const char *PIN16_SERVICE;
extern const char *PIN16_MODE_CHARACTERISTIC;
extern const char *PIN16_DATA_CHARACTERISTIC;

extern const char *PIN17_SERVICE;
extern const char *PIN17_MODE_CHARACTERISTIC;
extern const char *PIN17_DATA_CHARACTERISTIC;

extern struct pin_uuid_profile PIN_UUID_PROFILES[PIN_UUID_PROFILES_SIZE];

#endif
