package io.doist.datetimepicker.time;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import io.doist.datetimepicker.R;

public class TimePickerDialogFragmentDelegate implements TimePicker.OnTimeChangedListener {
    private static final String KEY_HOUR_OF_DAY = "hour";
    private static final String KEY_MINUTE = "minute";
    private static final String KEY_IS_24_HOUR = "is24Hour";

    private TimePicker mTimePicker;

    private OnTimeSetListener mOnTimeSetListener;

    public static Bundle createArguments(int hourOfDay, int minute, boolean is24Hour) {
        Bundle arguments = new Bundle();
        arguments.putInt(KEY_HOUR_OF_DAY, hourOfDay);
        arguments.putInt(KEY_MINUTE, minute);
        arguments.putBoolean(KEY_IS_24_HOUR, is24Hour);
        return arguments;
    }

    protected static int resolveDialogTheme(Context context, int resid) {
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
    public final Dialog onCreateDialog(Context context, Bundle savedInstanceState, Bundle arguments) {
        View view = LayoutInflater.from(context).inflate(R.layout.time_picker_dialog, null);
        mTimePicker = (TimePicker) view.findViewById(R.id.timePicker);
        if (savedInstanceState == null) {
            int hourOfDay = arguments.getInt(KEY_HOUR_OF_DAY);
            int minute = arguments.getInt(KEY_MINUTE);
            boolean is24Hour = arguments.getBoolean(KEY_IS_24_HOUR);

            mTimePicker = (TimePicker) view.findViewById(R.id.timePicker);
            mTimePicker.setCurrentHour(hourOfDay);
            mTimePicker.setCurrentMinute(minute);
            mTimePicker.setIs24Hour(is24Hour);
        }
        mTimePicker.setOnTimeChangedListener(this);

        final AlertDialog dialog = onCreateDialogBuilder(
                context, view, R.style.Theme_AppCompat_Light_Dialog_Alert_TimePicker).create();

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
     * Allows sub-classes to easily customize AlertDialog.
     */
    protected AlertDialog.Builder onCreateDialogBuilder(Context context, View view, int themeResId) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(context, resolveDialogTheme(context, themeResId));
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
        return builder;
    }

    public void setOnTimeSetListener(OnTimeSetListener listener) {
        mOnTimeSetListener = listener;
    }

    @Override
    public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
        // Do nothing.
    }

    public TimePicker getTimePicker() {
        return mTimePicker;
    }

    public void updateTime(int hourOfDay, int minuteOfHour) {
        mTimePicker.setCurrentHour(hourOfDay);
        mTimePicker.setCurrentMinute(minuteOfHour);
    }
}
