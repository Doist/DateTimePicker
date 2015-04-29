/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.doist.datetimepicker.time;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import io.doist.datetimepicker.R;


/**
 * A dialog that prompts the user for the time of day using a {@link TimePicker}.
 *
 * <p>See the <a href="{@docRoot}guide/topics/ui/controls/pickers.html">Pickers</a>
 * guide.</p>
 */
public class TimePickerDialogFragmentCompat extends DialogFragment implements TimePicker.OnTimeChangedListener {
    public static final String TAG = TimePickerDialogFragmentCompat.class.getName();

    private static final String KEY_HOUR_OF_DAY = "hour";
    private static final String KEY_MINUTE = "minute";
    private static final String KEY_IS_24_HOUR = "is24Hour";

    private TimePicker mTimePicker;
    private OnTimeSetListener mOnTimeSetListener;

    public TimePickerDialogFragmentCompat() { }

    public static TimePickerDialogFragmentCompat newInstance(OnTimeSetListener listener, int hourOfDay, int minute,
                                                             boolean is24Hour) {
        TimePickerDialogFragmentCompat fragment = new TimePickerDialogFragmentCompat();
        Bundle arguments = new Bundle();
        arguments.putInt(KEY_HOUR_OF_DAY, hourOfDay);
        arguments.putInt(KEY_MINUTE, minute);
        arguments.putBoolean(KEY_IS_24_HOUR, is24Hour);
        fragment.setArguments(arguments);
        fragment.setOnTimeSetListener(listener);
        return fragment;
    }

    public void setOnTimeSetListener(OnTimeSetListener listener) {
        mOnTimeSetListener = listener;
    }

    static int resolveDialogTheme(Context context, int resid) {
        if (resid == 0) {
            final TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.timePickerDialogTheme, outValue, true);
            return outValue.resourceId;
        } else {
            return resid;
        }
    }

    @SuppressWarnings("InflateParams")
    @NonNull
    @Override
    public final Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.time_picker_dialog, null);
        mTimePicker = (TimePicker) view.findViewById(R.id.timePicker);
        if (savedInstanceState == null) {
            Bundle arguments = getArguments();
            int hourOfDay = arguments.getInt(KEY_HOUR_OF_DAY);
            int minute = arguments.getInt(KEY_MINUTE);
            boolean is24Hour = arguments.getBoolean(KEY_IS_24_HOUR);

            mTimePicker = (TimePicker) view.findViewById(R.id.timePicker);
            mTimePicker.setCurrentHour(hourOfDay);
            mTimePicker.setCurrentMinute(minute);
            mTimePicker.setIs24Hour(is24Hour);
        }
        mTimePicker.setOnTimeChangedListener(this);

        final AlertDialog dialog = onCreateDialogBuilder(view).create();

        mTimePicker.setValidationCallback(new TimePicker.ValidationCallback() {
            @Override
            public void onValidationChanged(boolean valid) {
                final Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                if (positive != null) {
                    positive.setEnabled(valid);
                }
            }
        });

        return dialog;
    }

    /**
     * Allows sub-classes to customize AlertDialog.
     */
    protected AlertDialog.Builder onCreateDialogBuilder(View view) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(getActivity(), resolveDialogTheme(getActivity(), getTheme()));
        builder.setView(view);
        builder.setPositiveButton(R.string.done_label, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mOnTimeSetListener != null) {
                    mOnTimeSetListener.onTimeSet(
                            mTimePicker,
                            mTimePicker.getCurrentHour(),
                            mTimePicker.getCurrentMinute());
                }
            }
        });
        // Make sure the dialog width works as WRAP_CONTENT.
        builder.getContext().getTheme().applyStyle(R.style.Theme_Window_NoMinWidth, true);
        return builder;
    }

    @Override
    public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
        // Do nothing.
    }

    /**
     * Sets the current time.
     *
     * @param hourOfDay The current hour within the day.
     * @param minuteOfHour The current minute within the hour.
     */
    public void updateTime(int hourOfDay, int minuteOfHour) {
        mTimePicker.setCurrentHour(hourOfDay);
        mTimePicker.setCurrentMinute(minuteOfHour);
    }
}
