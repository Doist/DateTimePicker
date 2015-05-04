package io.doist.datetimepicker.sample;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.Calendar;

import io.doist.datetimepicker.date.DatePicker;
import io.doist.datetimepicker.fragment.DatePickerDialogFragmentCompat;
import io.doist.datetimepicker.date.OnDateSetListener;
import io.doist.datetimepicker.time.OnTimeSetListener;
import io.doist.datetimepicker.time.TimePicker;
import io.doist.datetimepicker.fragment.TimePickerDialogFragmentCompat;


public class MainActivity extends AppCompatActivity implements OnDateSetListener, OnTimeSetListener {
    private Calendar mCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        DatePickerDialogFragmentCompat datePickerDialog =
                (DatePickerDialogFragmentCompat) getSupportFragmentManager().findFragmentByTag(
                        DatePickerDialogFragmentCompat.TAG);
        if (datePickerDialog != null) {
            datePickerDialog.setOnDateSetListener(this);
        }

        TimePickerDialogFragmentCompat timePickerDialog =
                (TimePickerDialogFragmentCompat) getSupportFragmentManager().findFragmentByTag(
                        TimePickerDialogFragmentCompat.TAG);
        if (timePickerDialog != null) {
            timePickerDialog.setOnTimeSetListener(this);
        }
    }

    public void showDatePicker(View v) {
        DatePickerDialogFragmentCompat.newInstance(
                this,
                mCalendar.get(Calendar.YEAR),
                mCalendar.get(Calendar.MONTH),
                mCalendar.get(Calendar.DAY_OF_MONTH)).show(getSupportFragmentManager(), DatePickerDialogFragmentCompat.TAG);
    }

    public void showTimePicker(View v) {
        TimePickerDialogFragmentCompat.newInstance(
                this,
                mCalendar.get(Calendar.HOUR_OF_DAY),
                mCalendar.get(Calendar.MINUTE),
                false).show(getSupportFragmentManager(), TimePickerDialogFragmentCompat.TAG);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        Toast.makeText(this, year + "/" + (monthOfYear + 1) + "/" + dayOfMonth, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        Toast.makeText(this, hourOfDay + ":" + minute, Toast.LENGTH_SHORT).show();
    }
}
