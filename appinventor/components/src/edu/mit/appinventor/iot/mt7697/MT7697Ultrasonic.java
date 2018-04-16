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
 * The component controls an ultrasonic sensor on MT7697 boards.
 *
 * @author jerry73204@gmail.com (Lin, Hsiang-Jui)
 * @author az6980522@gmail.com (Yuan, Yu-Yuan)
 */
@DesignerComponent(version = 2,
                   description = "The MT7697Ultrasonic component lets users control an ultrasonic sensor connected to MT7697 boards.",
                   category = ComponentCategory.EXTENSION,
                   nonVisible = true,
                   iconName = "aiwebres/mt7697.png")
@SimpleObject(external = true)
public class MT7697Ultrasonic extends MT7697ExtensionBase {
  // constants
  private static final int TIMER_INTERVAL = 500; // ms
  private static final String LOG_TAG = "MT7697Ultrasonic";
  private static final String DEFAULT_PIN = "8";
  private static final String DEFAULT_UNIT = "cm";
  private static final String[] VALID_PINS = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17"};

  // variable
  private String mPin = DEFAULT_PIN;
  private final int mMode = MODE_ULTRASONIC; // unchanged in this component
  private String mUnit = DEFAULT_UNIT;
  private String mModeCharUuid;
  private String mDataCharUuid;
  private final String mServiceUuid = PIN_SERVICE_UUID; // unchanged in current implementation

  final BluetoothLE.BLEResponseHandler<Long> inputUpdateCallback = new BluetoothLE.BLEResponseHandler<Long>() {
    @Override
    public void onReceive(String serviceUUID, String characteristicUUID, List<Long> values) {
      int receivedValue = values.get(0).intValue();

      if (receivedValue >= 0)
        InputUpdated((double) receivedValue);
    }
  };

  Handler handler = new Handler();

  Runnable periodicTask = new Runnable() {
    @Override
    public void run() {
      if (!IsSupported())
        return;

      // write mode
      bleConnection.ExWriteIntegerValues(mServiceUuid, mModeCharUuid, true, mMode);

      handler.postDelayed(this, TIMER_INTERVAL);
    }
  };

  public MT7697Ultrasonic(Form form) {
    super(form);

    // initialize properties
    Pin(DEFAULT_PIN);

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
    if (IsSupported())
      bleConnection.ExWriteIntegerValues(mServiceUuid, mModeCharUuid, true, mMode); // write the mode
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
   * Set the unit for distance.
   *
   * __Parameters__:
   *
   *     * unit (_text_); in either "cm" or "inch"
   *
   * @param The target pin number.
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_CHOICES,
                    defaultValue = DEFAULT_UNIT,
                    editorArgs = {"cm", "inch"})
  @SimpleProperty
  public void Unit(String unit) {
    mUnit = unit;
  }

  /**
   * Get the unit for distance.
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR,
                  description = "The pin mode on the MT7697 board that the device is wired in to.")
  public String Unit() {
    return mUnit;
  }

  /**
   * Enable the InputUpdated event.
   */
  @SimpleFunction
  public void RequestInputUpdates() {
    if (IsSupported()) {
      bleConnection.ExRegisterForIntegerValues(mServiceUuid, mDataCharUuid, true, inputUpdateCallback);
    }
  }

  /**
   * Disable the previously requested InputUpdated event.
   */
  @SimpleFunction
  public void StopInputUpdates() {
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
   *     * value (_number_); The distance percieced by the sensor.
   *
   * @param The distance percieced by the sensor.
   */
  @SimpleEvent
  public void InputUpdated(double value) {
    if (mUnit.equals("inch"))
      value *= 0.393701;

    EventDispatcher.dispatchEvent(this, "InputUpdated", value);
  }
}
