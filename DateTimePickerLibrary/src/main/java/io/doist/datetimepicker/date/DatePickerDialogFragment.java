/*
 * Copyright (C) 2013 The Android Open Source Project
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

package io.doist.datetimepicker.date;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import io.doist.datetimepicker.R;

/**
 * Dialog allowing users to select a date.
 */
public class DatePickerDialogFragment extends DialogFragment implements DatePicker.OnDateChangedListener {
    public static final String TAG = DatePickerDialogFragment.class.getName();

    private static final String KEY_YEAR = "year";
    private static final String KEY_MONTH_OF_YEAR = "month";
    private static final String KEY_DAY_OF_MONTH = "day";

    private DatePicker mDatePicker;

    private OnDateSetListener mOnDateSetListener;

    public DatePickerDialogFragment() { }

    public static DatePickerDialogFragment newInstance(OnDateSetListener listener, int year, int monthOfYear,
                                                       int dayOfMonth) {
        DatePickerDialogFragment fragment = new DatePickerDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(KEY_YEAR, year);
        arguments.putInt(KEY_MONTH_OF_YEAR, monthOfYear);
        arguments.putInt(KEY_DAY_OF_MONTH, dayOfMonth);
        fragment.setArguments(arguments);
        fragment.setOnDateSetListener(listener);
        return fragment;
    }

    public void setOnDateSetListener(OnDateSetListener listener) {
        mOnDateSetListener = listener;
    }

    static int resolveDialogTheme(Context context, int resid) {
        if (resid == 0) {
            final TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.datePickerDialogTheme, outValue, true);
            return outValue.resourceId;
        } else {
            return resid;
        }
    }

    @SuppressWarnings("InflateParams")
    @NonNull
    @Override
    public final Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.date_picker_dialog, null);
        mDatePicker = (DatePicker) view.findViewById(R.id.datePicker);
        if (savedInstanceState == null) {
            Bundle arguments = getArguments();
            int year = arguments.getInt(KEY_YEAR);
            int monthOfYear = arguments.getInt(KEY_MONTH_OF_YEAR);
            int dayOfMonth = arguments.getInt(KEY_DAY_OF_MONTH);
            mDatePicker.init(year, monthOfYear, dayOfMonth, this);
        } else {
            mDatePicker.setOnDateChangedListener(this);
        }

        final AlertDialog dialog = onCreateDialogBuilder(view).create();

        mDatePicker.setValidationCallback(new DatePicker.ValidationCallback() {
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
                mOnDateSetListener.onDateSet(
                        mDatePicker,
                        mDatePicker.getYear(),
                        mDatePicker.getMonth(),
                        mDatePicker.getDayOfMonth());
            }
        });
        // Make sure the dialog width works as WRAP_CONTENT.
        builder.getContext().getTheme().applyStyle(R.style.Theme_Window_NoMinWidth, true);
        return builder;
    }

    @Override
    public void onDateChanged(DatePicker view, int year, int month, int day) {
        mDatePicker.init(year, month, day, this);
    }

    /**
     * Gets the {@link DatePicker} contained in this dialog.
     *
     * @return The calendar view.
     */
    public DatePicker getDatePicker() {
        return mDatePicker;
    }

    /**
     * Sets the current date.
     *
     * @param year The date year.
     * @param monthOfYear The date month.
     * @param dayOfMonth The date day of month.
     */
    public void updateDate(int year, int monthOfYear, int dayOfMonth) {
        mDatePicker.updateDate(year, monthOfYear, dayOfMonth);
    }
}
