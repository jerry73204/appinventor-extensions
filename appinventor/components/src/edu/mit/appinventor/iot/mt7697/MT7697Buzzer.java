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
 * The component controls a buzzer on MT7697 boards.
 *
 * @author jerry73204@gmail.com (Lin, Hsiang-Jui)
 * @author az6980522@gmail.com (Yuan, Yu-Yuan)
 */
@DesignerComponent(version = 2,
                   description = "The MT7697Buzzer component lets users control a buzzer on MT7697 boards.",
                   category = ComponentCategory.EXTENSION,
                   nonVisible = true,
                   iconName = "aiwebres/mt7697.png")
@SimpleObject(external = true)
public class MT7697Buzzer extends MT7697ExtensionBase {
  // constants
  private static final int TIMER_INTERVAL = 500; // ms
  private static final String LOG_TAG = "MT7697Buzzer";
  private static final String DEFAULT_PIN = "10";
  private static final String[] VALID_PINS = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17"};

  // variable
  private String mPin = DEFAULT_PIN;
  private static int mMode = MODE_BUZZER; // unchanged in this component
  private String mModeCharUuid;
  private String mDataCharUuid;
  private int mFreq;
  private static final String mServiceUuid = PIN_SERVICE_UUID; // unchanged in current implementation

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

  public MT7697Buzzer(Form form) {
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

    // assign values
    mPin = pin;
    mModeCharUuid = PIN_UUID_LOOKUP.get(mPin).mModeCharUuid;
    mDataCharUuid = PIN_UUID_LOOKUP.get(mPin).mDataCharUuid;

    // update mode and data
    if (IsSupported()) {
      bleConnection.ExWriteIntegerValues(mServiceUuid, mModeCharUuid, true, mMode); // write the mode
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
   * Set the sound frequency.
   *
   * __Parameters__:
   *
   *     * frequency (_number_); The sound frequency in Hz.
   *
   * @param The sound frequency in Hz.
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER,
                    defaultValue = "440")
  @SimpleProperty
  public void Frequency(int frequency) {
    // TODO range check
    mFreq = frequency;
  }

  /**
   * Get the sound frequency.
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR,
                  description = "The frequency of buzzer sound.")
  public int Frequency() {
    return mFreq;
  }

  /**
   * Cause the buzzer make sound for a period of time.
   *
   * __Parameters__:
   *
   *     * duration (_number_); The length in time in seconds.
   *
   * @param The length in time in seconds.
   */
  @SimpleFunction
  public void Buzz(int duration) {
    if (duration <= 0) {
      form.dispatchErrorOccurredEvent(this,
                                      "Buzz",
                                      ErrorMessages.ERROR_EXTENSION_ERROR,
                                      ERROR_INVALID_DURATION,
                                      LOG_TAG,
                                      "The duration should be positive integers");
      return;
    }

    if (!IsSupported())
      return;

    int data = (mFreq << 16) | duration;
    bleConnection.ExWriteIntegerValues(mServiceUuid, mDataCharUuid, true, data);
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
}
