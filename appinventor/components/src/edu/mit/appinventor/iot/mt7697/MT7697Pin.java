// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2011-2018 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package edu.mit.appinventor.iot.mt7697;

import java.util.List;
import android.os.Handler;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.Form;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.util.ErrorMessages;
import edu.mit.appinventor.ble.BluetoothLE;

import static edu.mit.appinventor.iot.mt7697.Constants.PIN_UUID_LOOKUP;
import static edu.mit.appinventor.iot.mt7697.Constants.PIN_SERVICE_UUID;

/**
 * The component controls a pin I/O on MT7697 boards.
 *
 * @author jerry73204@gmail.com (Lin, Hsiang-Jui)
 * @author az6980522@gmail.com (Yuan, Yu-Yuan)
 */
@DesignerComponent(version = 2,
                   description = "The MT7697Pin component lets users control the pin from their AppInventor apps.",
                   category = ComponentCategory.EXTENSION,
                   nonVisible = true,
                   iconName = "aiwebres/mt7697.png")
@SimpleObject(external = true)
public class MT7697Pin extends MT7697ExtensionBase {
  // constants
  private static final int TIMER_INTERVAL = 500; // ms
  private static final int INIT_INPUT_DATA  = -1;
  private static final int INIT_OUTPUT_DATA =  1;
  private static final String LOG_TAG = "MT7697Pin";
  private static final String DEFAULT_PIN = "2";
  private static final String DEFAULT_MODE = STRING_ANALOG_INPUT;
  private static final String[] VALID_PINS = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17"};

  // variable
  private String mPin = DEFAULT_PIN;
  private int mMode = MODE_UNSET;
  private String mModeCharUuid;
  private String mDataCharUuid;
  private int mData;
  private static final String mServiceUuid = PIN_SERVICE_UUID; // unchanged in current implementation

  final BluetoothLE.BLEResponseHandler<Long> inputUpdateCallback = new BluetoothLE.BLEResponseHandler<Long>() {
    @Override
    public void onReceive(String serviceUUID, String characteristicUUID, List<Long> values) {
      int receivedValue = values.get(0).intValue();
      if (receivedValue < 0)
        return;

      if (mMode == MODE_DIGITAL_INPUT) {
        mData = receivedValue == 0 ? 0 : 1;
        InputUpdated(mData);

      } else if (mMode == MODE_ANALOG_INPUT) {
        mData = receivedValue;
        InputUpdated(mData);
      }
    }
  };

  Handler handler = new Handler();

  Runnable periodicTask = new Runnable() {
    @Override
    public void run() {
      if (IsSupported()) {
        if (mMode != MODE_UNSET) {
          // write mode
          bleConnection.ExWriteIntegerValues(mServiceUuid, mModeCharUuid, true, mMode);

          // write data
          if ( (mMode == MODE_ANALOG_OUTPUT || mMode == MODE_DIGITAL_OUTPUT || mMode == MODE_SERVO) && (mData != INIT_OUTPUT_DATA) )
            bleConnection.ExWriteIntegerValues(mServiceUuid, mDataCharUuid, true, mData);
        }
      }

      handler.postDelayed(this, TIMER_INTERVAL);
    }
  };

  public MT7697Pin(Form form) {
    super(form);

    // initialize properties
    Pin(DEFAULT_PIN);
    Mode(DEFAULT_MODE);

    // start periodic task
    handler.post(periodicTask);
  }

  /**
   * Set the target pin by pin number. To set this property in blocky editor, assign text value
   * in either one of "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16" or "17".
   *
   * __Parameters__:
   *
   *     * pin (_text_); The target pin number.
   *
   * @param The target pin number.
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_CHOICES,
                    defaultValue = DEFAULT_PIN,
                    editorArgs = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17"})
  @SimpleProperty
  public void Pin(String pin) {
    // duplicated with editorArgs, but we have to make compiler happy
    boolean isValidPin = false;

    // sanity check
    for (int ind = 0; ind < VALID_PINS.length; ind += 1) {
      if (VALID_PINS[ind].equals(pin)) {
        isValidPin = true;
        break;
      }
    }

    if (!isValidPin) {
      form.dispatchErrorOccurredEvent(this,
                                      "Pin",
                                      ErrorMessages.ERROR_EXTENSION_ERROR,
                                      ERROR_INVALID_PIN_ARGUMENT,
                                      LOG_TAG,
                                      "Invalid pin value");
      return;
    }

    // try to unregister callbacks
    if (IsSupported())
      bleConnection.ExUnregisterForValues(mServiceUuid, mDataCharUuid, inputUpdateCallback);

    // assign values
    mPin = pin;
    mModeCharUuid = PIN_UUID_LOOKUP.get(mPin).mModeCharUuid;
    mDataCharUuid = PIN_UUID_LOOKUP.get(mPin).mDataCharUuid;

    // update mode and data
    if (IsSupported()) {
      bleConnection.ExWriteIntegerValues(mServiceUuid, mModeCharUuid, true, mMode); // write the mode

      if ( (mMode == MODE_ANALOG_OUTPUT || mMode == MODE_DIGITAL_OUTPUT) && (mData <= 0) )
        bleConnection.ExWriteIntegerValues(mServiceUuid, mDataCharUuid, true, mData);
    }
  }

  /**
   * Get the target pin number.
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR,
                  description = "The pin mode on the MT7697 board that the device is wired in to.")
  public String Pin() {
    return mPin;
  }

  /**
   * Set the target pin mode. To set this property in blocky editor, assign text value
   * in either one of "analog input", "analog output", "ditital input", "ditigal output",
   * or "servo".
   *
   * __Parameters__:
   *
   *     * mode (_text_); The mode description set on the target pin.
   *
   * @param The mode description set on the target pin.
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_CHOICES,
                    defaultValue = DEFAULT_MODE,
                    editorArgs = { STRING_ANALOG_INPUT, STRING_ANALOG_OUTPUT, STRING_DIGITAL_INPUT, STRING_DIGITAL_OUTPUT, STRING_SERVO })
  @SimpleProperty
  public void Mode(String mode) {
    if (mode.equals(STRING_ANALOG_INPUT)) {
      mMode = MODE_ANALOG_INPUT;
      mData = INIT_INPUT_DATA;
    } else if (mode.equals(STRING_ANALOG_OUTPUT)) {
      mMode = MODE_ANALOG_OUTPUT;
      mData = INIT_OUTPUT_DATA;
    } else if (mode.equals(STRING_DIGITAL_INPUT)) {
      mMode = MODE_DIGITAL_INPUT;
      mData = INIT_INPUT_DATA;
    } else if (mode.equals(STRING_DIGITAL_OUTPUT)) {
      mMode = MODE_DIGITAL_OUTPUT;
      mData = INIT_OUTPUT_DATA;
    } else if (mode.equals(STRING_SERVO)) {
      mMode = MODE_SERVO;
      mData = INIT_OUTPUT_DATA;
    } else {
      form.dispatchErrorOccurredEvent(this,
                                      "Mode",
                                      ErrorMessages.ERROR_EXTENSION_ERROR,
                                      ERROR_INVALID_MODE_ARGUMENT,
                                      LOG_TAG,
                                      "Invalid mode value");
      return;
    }

    if (IsSupported())
      bleConnection.ExWriteIntegerValues(mServiceUuid, mModeCharUuid, true, mMode);
  }

  /**
   * Get the target pin mode.
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR,
                  description = "The pin mode on the MT7697 board that the device is wired in to.")
  public String Mode() {
    switch (mMode) {
    case MODE_ANALOG_INPUT:
      return STRING_ANALOG_INPUT;

    case MODE_ANALOG_OUTPUT:
      return STRING_ANALOG_OUTPUT;

    case MODE_DIGITAL_INPUT:
      return STRING_DIGITAL_INPUT;

    case MODE_DIGITAL_OUTPUT:
      return STRING_DIGITAL_OUTPUT;

    case MODE_SERVO:
      return STRING_SERVO;

    default:
      throw new IllegalArgumentException();
    }
  }

  /**
   * Set the output intensity of a pin on MT7697. In analog output mode, the argument should be
   * non-negative and not exceed 255, otherwise it will be trimmed. In digital output mode, zero
   * and non-zero argument are respectively treated as LOW and HIGH outputs. In servo mode, the
   * argument should be in range from 0 to 180.
   *
   * __Parameters__:
   *
   *     * value (_number_); The output intensity of the target pin.
   *
   * @param The output intensity of the target pin.
   */
  @SimpleFunction
  public void Write(int value) {
    if (!IsSupported())
      return;

    if (mMode == MODE_ANALOG_OUTPUT || mMode == MODE_DIGITAL_OUTPUT) {
      // trim value
      value = value >= 0 ? value : 0;
      value = value <= 255 ? value : 255;
      mData = -value;
    } else if (mMode == MODE_SERVO) {
      value = value >= 0 ? value : 0;
      value = value <= 180 ? value : 180;
      mData = -value;
    } else {
      form.dispatchErrorOccurredEvent(this,
                                      "Write",
                                      ErrorMessages.ERROR_EXTENSION_ERROR,
                                      ERROR_INVALID_STATE,
                                      LOG_TAG,
                                      "Cannot call Write() on non-output or non-servo mode");
    }
  }

  /**
   * Obtain the most recent reading from the pin. On success, the <a href="#ProximityReceived"><code>InputUpdated</code></a> event will be triggered.
   */
  @SimpleFunction
  public void Read() {
    if ( IsSupported() && (mMode == MODE_ANALOG_INPUT || mMode == MODE_DIGITAL_INPUT) ) {
      bleConnection.ExReadIntegerValues(mServiceUuid, mDataCharUuid, true, inputUpdateCallback);
    }
  }


  /**
   * Enable the InputUpdated event.
   */
  @SimpleFunction
  public void RequestInputUpdate() {
    if ( IsSupported() && (mMode == MODE_ANALOG_INPUT || mMode == MODE_DIGITAL_INPUT) ) {
      bleConnection.ExRegisterForIntegerValues(mServiceUuid, mDataCharUuid, true, inputUpdateCallback);
    }
  }

  /**
   * Disable the previously requested InputUpdated event.
   */
  @SimpleFunction
  public void StopInputUpdate() {
    if (IsSupported()) {
      bleConnection.ExUnregisterForValues(mServiceUuid, mDataCharUuid, inputUpdateCallback);
    }
  }

  /**
   * Tests whether the Bluetooth low energy device is broadcasting support for the service.
   */
  @Override
  @SimpleFunction
  public boolean IsSupported() {
    return mMode != MODE_UNSET &&
      mPin != null &&
      bleConnection != null &&
      mModeCharUuid != null &&
      mDataCharUuid != null &&
      bleConnection.isCharacteristicPublished(mServiceUuid, mModeCharUuid) &&
      bleConnection.isCharacteristicPublished(mServiceUuid, mDataCharUuid);
  }

  /**
   * The InputUpdated event is triggered when a value is received from the input pin of MT7697.
   *
   * __Parameters__:
   *
   *     * value (_number_); The intensity which is read from the input pin.
   *
   * @param The intensity which is read from the input pin.
   */
  @SimpleEvent
  public void InputUpdated(final int value) {
    EventDispatcher.dispatchEvent(this, "InputUpdated", value);
  }
}
