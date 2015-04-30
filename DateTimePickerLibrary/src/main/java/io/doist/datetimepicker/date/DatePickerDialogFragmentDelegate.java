package io.doist.datetimepicker.date;

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

public class DatePickerDialogFragmentDelegate implements DatePicker.OnDateChangedListener {
    private static final String KEY_YEAR = "year";
    private static final String KEY_MONTH_OF_YEAR = "month";
    private static final String KEY_DAY_OF_MONTH = "day";

    private DatePicker mDatePicker;

    private OnDateSetListener mOnDateSetListener;

    public static Bundle createArguments(int year, int monthOfYear, int dayOfMonth) {
        Bundle arguments = new Bundle();
        arguments.putInt(KEY_YEAR, year);
        arguments.putInt(KEY_MONTH_OF_YEAR, monthOfYear);
        arguments.putInt(KEY_DAY_OF_MONTH, dayOfMonth);
        return arguments;
    }

    protected static int resolveDialogTheme(Context context, int resId) {
        if (resId == 0) {
            final TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.datePickerDialogTheme, outValue, true);
            return outValue.resourceId;
        } else {
            return resId;
        }
    }

    @SuppressWarnings("InflateParams")
    @NonNull
    public final Dialog onCreateDialog(Context context, Bundle savedInstanceState, Bundle arguments) {
        View view = LayoutInflater.from(context).inflate(R.layout.date_picker_dialog, null);
        mDatePicker = (DatePicker) view.findViewById(R.id.datePicker);
        if (savedInstanceState == null) {
            int year = arguments.getInt(KEY_YEAR);
            int monthOfYear = arguments.getInt(KEY_MONTH_OF_YEAR);
            int dayOfMonth = arguments.getInt(KEY_DAY_OF_MONTH);
            mDatePicker.init(year, monthOfYear, dayOfMonth, this);
        } else {
            mDatePicker.setOnDateChangedListener(this);
        }

        final AlertDialog dialog = onCreateDialogBuilder(
                context, view, R.style.Theme_AppCompat_Light_Dialog_Alert_DatePicker).create();

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
     * Allows sub-classes to easily customize AlertDialog.
     */
    protected AlertDialog.Builder onCreateDialogBuilder(Context context, View view, int themeResId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, resolveDialogTheme(context, themeResId));
        builder.setView(view);
        builder.setPositiveButton(R.string.done_label, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mOnDateSetListener != null) {
                    mOnDateSetListener.onDateSet(
                            mDatePicker,
                            mDatePicker.getYear(),
                            mDatePicker.getMonth(),
                            mDatePicker.getDayOfMonth());
                }
            }
        });
        return builder;
    }

    @Override
    public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        mDatePicker.init(year, monthOfYear, dayOfMonth, this);
    }

    public void setOnDateSetListener(OnDateSetListener listener) {
        mOnDateSetListener = listener;
    }

    public DatePicker getDatePicker() {
        return mDatePicker;
    }

    public void updateDate(int year, int monthOfYear, int dayOfMonth) {
        mDatePicker.updateDate(year, monthOfYear, dayOfMonth);
    }
}
