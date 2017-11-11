/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;

import io.doist.datetimepicker.R;
import io.doist.datetimepicker.util.DateTimeUtilsCompat;
import io.doist.datetimepicker.util.ViewStateUtils;

/**
 * A delegate for picking up a date (day / month / year).
 */
class DatePickerCalendarDelegate extends DatePicker.AbstractDatePickerDelegate implements DatePickerController {
    private static final int USE_LOCALE = 0;

    private static final int UNINITIALIZED = -1;
    private static final int MONTH_AND_DAY_VIEW = 0;
    private static final int YEAR_VIEW = 1;

    private static final int DEFAULT_START_YEAR = 1900;
    private static final int DEFAULT_END_YEAR = 2100;

    private static final int ANIMATION_DURATION = 300;

    private static final int MONTH_INDEX = 0;
    private static final int DAY_INDEX = 1;
    private static final int YEAR_INDEX = 2;

    private SimpleDateFormat mYearFormat = new SimpleDateFormat("y", Locale.getDefault());
    private SimpleDateFormat mDayFormat = new SimpleDateFormat("d", Locale.getDefault());

    private TextView mDayOfWeekView;

    /** Layout that contains the current month, day, and year. */
    private LinearLayout mMonthDayYearLayout;

    /** Clickable layout that contains the current day and year. */
    private LinearLayout mMonthAndDayLayout;

    private TextView mHeaderMonthTextView;
    private TextView mHeaderDayOfMonthTextView;
    private TextView mHeaderYearTextView;
    private DayPickerView mDayPickerView;
    private YearPickerView mYearPickerView;

    private boolean mIsEnabled = true;

    // Accessibility strings.
    private String mDayPickerDescription;
    private String mSelectDay;
    private String mYearPickerDescription;
    private String mSelectYear;

    private AccessibleDateAnimator mAnimator;

    private DatePicker.OnDateChangedListener mDateChangedListener;

    private int mCurrentView = UNINITIALIZED;

    private Calendar mCurrentDate;
    private Calendar mTempDate;
    private Calendar mMinDate;
    private Calendar mMaxDate;

    private int mFirstDayOfWeek = USE_LOCALE;


    private HashSet<OnDateChangedListener> mListeners = new HashSet<>();

    public DatePickerCalendarDelegate(DatePicker delegator, Context context, AttributeSet attrs, int defStyleAttr) {
        super(delegator, context);

        final Locale locale = Locale.getDefault();
        mMinDate = getCalendarForLocale(mMinDate, locale);
        mMaxDate = getCalendarForLocale(mMaxDate, locale);
        mTempDate = getCalendarForLocale(mMaxDate, locale);
        mCurrentDate = getCalendarForLocale(mCurrentDate, locale);

        mMinDate.set(DEFAULT_START_YEAR, 1, 1);
        mMaxDate.set(DEFAULT_END_YEAR, 12, 31);

        final Resources res = mDelegator.getResources();
        final TypedArray a = mContext.obtainStyledAttributes(attrs, R.styleable.DatePicker, defStyleAttr, 0);
        final LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final int layoutResourceId = a.getResourceId(R.styleable.DatePicker_layout, R.layout.date_picker_holo);
        final View mainView = inflater.inflate(layoutResourceId, null);
        mDelegator.addView(mainView);

        mDayOfWeekView = mainView.findViewById(R.id.date_picker_header);

        // Layout that contains the current date and day name header.
        final LinearLayout dateLayout = mainView.findViewById(R.id.day_picker_selector_layout);
        mMonthDayYearLayout = mainView.findViewById(R.id.date_picker_month_day_year_layout);
        mMonthAndDayLayout = mainView.findViewById(R.id.date_picker_month_and_day_layout);
        mMonthAndDayLayout.setOnClickListener(mClickListener);
        mHeaderMonthTextView = mainView.findViewById(R.id.date_picker_month);
        mHeaderDayOfMonthTextView = mainView.findViewById(R.id.date_picker_day);
        mHeaderYearTextView = mainView.findViewById(R.id.date_picker_year);
        mHeaderYearTextView.setOnClickListener(mClickListener);

        // Obtain default highlight color from the theme.
        final int defaultHighlightColor = mHeaderYearTextView.getHighlightColor();

        // Use Theme attributes if possible
        final int dayOfWeekTextAppearanceResId = a.getResourceId(
                R.styleable.DatePicker_dayOfWeekTextAppearance, -1);
        if (dayOfWeekTextAppearanceResId != -1) {
            mDayOfWeekView.setTextAppearance(context, dayOfWeekTextAppearanceResId);
        }

        mDayOfWeekView.setBackground(a.getDrawable(R.styleable.DatePicker_dayOfWeekBackground));

        dateLayout.setBackground(a.getDrawable(R.styleable.DatePicker_headerBackground));

        final int headerSelectedTextColor = a.getColor(
                R.styleable.DatePicker_headerSelectedTextColor, defaultHighlightColor);
        final int monthTextAppearanceResId = a.getResourceId(
                R.styleable.DatePicker_headerMonthTextAppearance, -1);
        if (monthTextAppearanceResId != -1) {
            mHeaderMonthTextView.setTextAppearance(context, monthTextAppearanceResId);
        }
        mHeaderMonthTextView.setTextColor(ViewStateUtils.addStateIfMissing(
                mHeaderMonthTextView.getTextColors(), android.R.attr.state_selected, headerSelectedTextColor));

        final int dayOfMonthTextAppearanceResId = a.getResourceId(
                R.styleable.DatePicker_headerDayOfMonthTextAppearance, -1);
        if (dayOfMonthTextAppearanceResId != -1) {
            mHeaderDayOfMonthTextView.setTextAppearance(context, dayOfMonthTextAppearanceResId);
        }
        mHeaderDayOfMonthTextView.setTextColor(ViewStateUtils.addStateIfMissing(
                mHeaderDayOfMonthTextView.getTextColors(), android.R.attr.state_selected, headerSelectedTextColor));

        final int yearTextAppearanceResId = a.getResourceId(
                R.styleable.DatePicker_headerYearTextAppearance, -1);
        if (yearTextAppearanceResId != -1) {
            mHeaderYearTextView.setTextAppearance(context, yearTextAppearanceResId);
        }
        mHeaderYearTextView.setTextColor(ViewStateUtils.addStateIfMissing(
                mHeaderYearTextView.getTextColors(), android.R.attr.state_selected, headerSelectedTextColor));

        mDayPickerView = new DayPickerView(mContext);
        mDayPickerView.setFirstDayOfWeek(mFirstDayOfWeek);
        mDayPickerView.setMinDate(mMinDate.getTimeInMillis());
        mDayPickerView.setMaxDate(mMaxDate.getTimeInMillis());
        mDayPickerView.setDate(mCurrentDate.getTimeInMillis());
        mDayPickerView.setOnDaySelectedListener(new DayPickerView.OnDaySelectedListener() {
            @Override
            public void onDaySelected(DayPickerView view, Calendar day) {
                mCurrentDate.setTimeInMillis(day.getTimeInMillis());
                onDateChanged(true, true);
            }
        });

        mYearPickerView = new YearPickerView(mContext);
        mYearPickerView.init(this);

        final int yearSelectedCircleColor =
                a.getColor(R.styleable.DatePicker_yearListSelectorColor, defaultHighlightColor);
        mYearPickerView.setYearSelectedCircleColor(yearSelectedCircleColor);

        final ColorStateList calendarTextColor = a.getColorStateList(
                R.styleable.DatePicker_calendarTextColor);
        final int calendarSelectedTextColor = a.getColor(
                R.styleable.DatePicker_calendarSelectedTextColor, defaultHighlightColor);
        mDayPickerView.setCalendarTextColor(ViewStateUtils.addStateIfMissing(
                calendarTextColor, android.R.attr.state_selected, calendarSelectedTextColor));

        mDayPickerDescription = res.getString(R.string.day_picker_description);
        mSelectDay = res.getString(R.string.select_day);
        mYearPickerDescription = res.getString(R.string.year_picker_description);
        mSelectYear = res.getString(R.string.select_year);

        mAnimator = mainView.findViewById(R.id.animator);
        mAnimator.addView(mDayPickerView);
        mAnimator.addView(mYearPickerView);
        mAnimator.setDateMillis(mCurrentDate.getTimeInMillis());

        final Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(ANIMATION_DURATION);
        mAnimator.setInAnimation(animation);

        final Animation animation2 = new AlphaAnimation(1.0f, 0.0f);
        animation2.setDuration(ANIMATION_DURATION);
        mAnimator.setOutAnimation(animation2);

        a.recycle();

        updateDisplay(false);
        setCurrentView(MONTH_AND_DAY_VIEW);
    }

    /**
     * Gets a calendar for locale bootstrapped with the value of a given calendar.
     *
     * @param oldCalendar The old calendar.
     * @param locale The locale.
     */
    private Calendar getCalendarForLocale(Calendar oldCalendar, Locale locale) {
        if (oldCalendar == null) {
            return Calendar.getInstance(locale);
        } else {
            final long currentTimeMillis = oldCalendar.getTimeInMillis();
            Calendar newCalendar = Calendar.getInstance(locale);
            newCalendar.setTimeInMillis(currentTimeMillis);
            return newCalendar;
        }
    }

    /**
     * Compute the array representing the order of Month / Day / Year views in their layout.
     * Will be used for I18N purpose as the order of them depends on the Locale.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private int[] getMonthDayYearIndexes() {
        int[] result = new int[3];

        final String bestDateTimePattern = getBestDatePattern(mCurrentLocale);
        final String filteredPattern = bestDateTimePattern.replaceAll("'.*?'", "");

        final int dayIndex = filteredPattern.indexOf('d');
        final int monthMIndex = filteredPattern.indexOf("M");
        final int monthIndex = (monthMIndex != -1) ? monthMIndex : filteredPattern.indexOf("L");
        final int yearIndex = filteredPattern.indexOf("y");

        if (yearIndex < monthIndex) {
            result[YEAR_INDEX] = 0;

            if (monthIndex < dayIndex) {
                result[MONTH_INDEX] = 1;
                result[DAY_INDEX] = 2;
            } else {
                result[MONTH_INDEX] = 2;
                result[DAY_INDEX] = 1;
            }
        } else {
            result[YEAR_INDEX] = 2;

            if (monthIndex < dayIndex) {
                result[MONTH_INDEX] = 0;
                result[DAY_INDEX] = 1;
            } else {
                result[MONTH_INDEX] = 1;
                result[DAY_INDEX] = 0;
            }
        }
        return result;
    }

    private void updateDisplay(boolean announce) {
        if (mDayOfWeekView != null) {
            mDayOfWeekView.setText(mCurrentDate.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG,
                    Locale.getDefault()));
        }

        // Compute indices of Month, Day and Year views
        final int[] viewIndices = getMonthDayYearIndexes();
        // Position the Year and MonthAndDay views within the header.
        mMonthDayYearLayout.removeAllViews();
        if (viewIndices[YEAR_INDEX] == 0) {
            mMonthDayYearLayout.addView(mHeaderYearTextView);
            mMonthDayYearLayout.addView(mMonthAndDayLayout);
        } else {
            mMonthDayYearLayout.addView(mMonthAndDayLayout);
            mMonthDayYearLayout.addView(mHeaderYearTextView);
        }

        // Position Day and Month views within the MonthAndDay view.
        mMonthAndDayLayout.removeAllViews();
        if (viewIndices[MONTH_INDEX] > viewIndices[DAY_INDEX]) {
            mMonthAndDayLayout.addView(mHeaderDayOfMonthTextView);
            mMonthAndDayLayout.addView(mHeaderMonthTextView);
        } else {
            mMonthAndDayLayout.addView(mHeaderMonthTextView);
            mMonthAndDayLayout.addView(mHeaderDayOfMonthTextView);
        }

        mHeaderMonthTextView.setText(mCurrentDate.getDisplayName(Calendar.MONTH, Calendar.SHORT,
                Locale.getDefault()).toUpperCase(Locale.getDefault()));
        mHeaderDayOfMonthTextView.setText(mDayFormat.format(mCurrentDate.getTime()));
        mHeaderYearTextView.setText(mYearFormat.format(mCurrentDate.getTime()));

        // Accessibility.
        long millis = mCurrentDate.getTimeInMillis();
        mAnimator.setDateMillis(millis);
        int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_YEAR;
        String monthAndDayText = DateUtils.formatDateTime(mContext, millis, flags);
        mMonthAndDayLayout.setContentDescription(monthAndDayText);

        if (announce) {
            flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR;
            String fullDateText = DateUtils.formatDateTime(mContext, millis, flags);
            mAnimator.announceForAccessibility(fullDateText);
        }
    }

    private void setCurrentView(final int viewIndex) {
        long millis = mCurrentDate.getTimeInMillis();

        switch (viewIndex) {
            case MONTH_AND_DAY_VIEW:
                mDayPickerView.setDate(getSelectedDay().getTimeInMillis());
                if (mCurrentView != viewIndex) {
                    mMonthAndDayLayout.setSelected(true);
                    mHeaderYearTextView.setSelected(false);
                    mAnimator.setDisplayedChild(MONTH_AND_DAY_VIEW);
                    mCurrentView = viewIndex;
                }

                final int flags = DateUtils.FORMAT_SHOW_DATE;
                final String dayString = DateUtils.formatDateTime(mContext, millis, flags);
                mAnimator.setContentDescription(mDayPickerDescription + ": " + dayString);
                mAnimator.announceForAccessibility(mSelectDay);
                break;
            case YEAR_VIEW:
                mYearPickerView.onDateChanged();
                if (mCurrentView != viewIndex) {
                    mMonthAndDayLayout.setSelected(false);
                    mHeaderYearTextView.setSelected(true);
                    mAnimator.setDisplayedChild(YEAR_VIEW);
                    mCurrentView = viewIndex;
                }

                final CharSequence yearString = mYearFormat.format(millis);
                mAnimator.setContentDescription(mYearPickerDescription + ": " + yearString);
                mAnimator.announceForAccessibility(mSelectYear);
                break;
        }
    }

    @Override
    public void init(int year, int monthOfYear, int dayOfMonth, DatePicker.OnDateChangedListener listener) {
        mCurrentDate.set(Calendar.YEAR, year);
        mCurrentDate.set(Calendar.MONTH, monthOfYear);
        mCurrentDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

        mDateChangedListener = listener;

        onDateChanged(false, false);
    }

    @Override
    public void setOnDateChangedListener(DatePicker.OnDateChangedListener listener) {
        mDateChangedListener = listener;
    }

    @Override
    public void updateDate(int year, int month, int dayOfMonth) {
        mCurrentDate.set(Calendar.YEAR, year);
        mCurrentDate.set(Calendar.MONTH, month);
        mCurrentDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

        onDateChanged(false, true);
    }

    private void onDateChanged(boolean fromUser, boolean callbackToClient) {
        if (callbackToClient && mDateChangedListener != null) {
            final int year = mCurrentDate.get(Calendar.YEAR);
            final int monthOfYear = mCurrentDate.get(Calendar.MONTH);
            final int dayOfMonth = mCurrentDate.get(Calendar.DAY_OF_MONTH);
            mDateChangedListener.onDateChanged(mDelegator, year, monthOfYear, dayOfMonth);
        }

        for (OnDateChangedListener listener : mListeners) {
            listener.onDateChanged();
        }

        mDayPickerView.setDate(getSelectedDay().getTimeInMillis());

        updateDisplay(fromUser);

        if (fromUser) {
            tryVibrate();
        }
    }

    @Override
    public int getYear() {
        return mCurrentDate.get(Calendar.YEAR);
    }

    @Override
    public int getMonth() {
        return mCurrentDate.get(Calendar.MONTH);
    }

    @Override
    public int getDayOfMonth() {
        return mCurrentDate.get(Calendar.DAY_OF_MONTH);
    }

    @Override
    public void setMinDate(long minDate) {
        mTempDate.setTimeInMillis(minDate);
        if (mTempDate.get(Calendar.YEAR) == mMinDate.get(Calendar.YEAR)
                && mTempDate.get(Calendar.DAY_OF_YEAR) != mMinDate.get(Calendar.DAY_OF_YEAR)) {
            return;
        }
        if (mCurrentDate.before(mTempDate)) {
            mCurrentDate.setTimeInMillis(minDate);
            onDateChanged(false, true);
        }
        mMinDate.setTimeInMillis(minDate);
        mDayPickerView.setMinDate(minDate);
        mYearPickerView.setRange(mMinDate, mMaxDate);
    }

    @Override
    public Calendar getMinDate() {
        return mMinDate;
    }

    @Override
    public void setMaxDate(long maxDate) {
        mTempDate.setTimeInMillis(maxDate);
        if (mTempDate.get(Calendar.YEAR) == mMaxDate.get(Calendar.YEAR)
                && mTempDate.get(Calendar.DAY_OF_YEAR) != mMaxDate.get(Calendar.DAY_OF_YEAR)) {
            return;
        }
        if (mCurrentDate.after(mTempDate)) {
            mCurrentDate.setTimeInMillis(maxDate);
            onDateChanged(false, true);
        }
        mMaxDate.setTimeInMillis(maxDate);
        mDayPickerView.setMaxDate(maxDate);
        mYearPickerView.setRange(mMinDate, mMaxDate);
    }

    @Override
    public Calendar getMaxDate() {
        return mMaxDate;
    }

    @Override
    public void setFirstDayOfWeek(int firstDayOfWeek) {
        mFirstDayOfWeek = firstDayOfWeek;

        mDayPickerView.setFirstDayOfWeek(firstDayOfWeek);
    }

    @Override
    public int getFirstDayOfWeek() {
        if (mFirstDayOfWeek != USE_LOCALE) {
            return mFirstDayOfWeek;
        }
        return mCurrentDate.getFirstDayOfWeek();
    }

    @Override
    public void setEnabled(boolean enabled) {
        mMonthAndDayLayout.setEnabled(enabled);
        mHeaderYearTextView.setEnabled(enabled);
        mAnimator.setEnabled(enabled);
        mIsEnabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return mIsEnabled;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        mYearFormat = new SimpleDateFormat("y", newConfig.locale);
        mDayFormat = new SimpleDateFormat("d", newConfig.locale);
    }

    @Override
    public Parcelable onSaveInstanceState(Parcelable superState) {
        final int year = mCurrentDate.get(Calendar.YEAR);
        final int month = mCurrentDate.get(Calendar.MONTH);
        final int day = mCurrentDate.get(Calendar.DAY_OF_MONTH);

        int listPosition = -1;
        int listPositionOffset = -1;

        if (mCurrentView == MONTH_AND_DAY_VIEW) {
            listPosition = mDayPickerView.getMostVisiblePosition();
        } else if (mCurrentView == YEAR_VIEW) {
            listPosition = mYearPickerView.getFirstVisiblePosition();
            listPositionOffset = mYearPickerView.getFirstPositionOffset();
        }

        return new SavedState(superState, year, month, day, mMinDate.getTimeInMillis(),
                mMaxDate.getTimeInMillis(), mCurrentView, listPosition, listPositionOffset);
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;

        mCurrentDate.set(ss.getSelectedYear(), ss.getSelectedMonth(), ss.getSelectedDay());
        mCurrentView = ss.getCurrentView();
        mMinDate.setTimeInMillis(ss.getMinDate());
        mMaxDate.setTimeInMillis(ss.getMaxDate());

        updateDisplay(false);
        setCurrentView(mCurrentView);

        final int listPosition = ss.getListPosition();
        if (listPosition != -1) {
            if (mCurrentView == MONTH_AND_DAY_VIEW) {
                mDayPickerView.postSetSelection(listPosition);
            } else if (mCurrentView == YEAR_VIEW) {
                mYearPickerView.postSetSelectionFromTop(listPosition, ss.getListPositionOffset());
            }
        }
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        onPopulateAccessibilityEvent(event);
        return true;
    }

    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        event.getText().add(mCurrentDate.getTime().toString());
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        event.setClassName(DatePicker.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        info.setClassName(DatePicker.class.getName());
    }

    @Override
    public void onYearSelected(int year) {
        adjustDayInMonthIfNeeded(mCurrentDate.get(Calendar.MONTH), year);
        mCurrentDate.set(Calendar.YEAR, year);
        onDateChanged(true, true);

        // Auto-advance to month and day view.
        setCurrentView(MONTH_AND_DAY_VIEW);
    }

    // If the newly selected month / year does not contain the currently selected day number,
    // change the selected day number to the last day of the selected month or year.
    //      e.g. Switching from Mar to Apr when Mar 31 is selected -> Apr 30
    //      e.g. Switching from 2012 to 2013 when Feb 29, 2012 is selected -> Feb 28, 2013
    private void adjustDayInMonthIfNeeded(int month, int year) {
        int day = mCurrentDate.get(Calendar.DAY_OF_MONTH);
        int daysInMonth = getDaysInMonth(month, year);
        if (day > daysInMonth) {
            mCurrentDate.set(Calendar.DAY_OF_MONTH, daysInMonth);
        }
    }

    public static int getDaysInMonth(int month, int year) {
        switch (month) {
            case Calendar.JANUARY:
            case Calendar.MARCH:
            case Calendar.MAY:
            case Calendar.JULY:
            case Calendar.AUGUST:
            case Calendar.OCTOBER:
            case Calendar.DECEMBER:
                return 31;
            case Calendar.APRIL:
            case Calendar.JUNE:
            case Calendar.SEPTEMBER:
            case Calendar.NOVEMBER:
                return 30;
            case Calendar.FEBRUARY:
                return (year % 4 == 0) ? 29 : 28;
            default:
                throw new IllegalArgumentException("Invalid Month");
        }
    }

    @Override
    public void registerOnDateChangedListener(OnDateChangedListener listener) {
        mListeners.add(listener);
    }

    @Override
    public Calendar getSelectedDay() {
        return mCurrentDate;
    }

    @Override
    public void tryVibrate() {
        mDelegator.tryVibrate();
    }

    /**
     * Class for managing state storing/restoring.
     */
    private static class SavedState extends View.BaseSavedState {

        private final int mSelectedYear;
        private final int mSelectedMonth;
        private final int mSelectedDay;
        private final long mMinDate;
        private final long mMaxDate;
        private final int mCurrentView;
        private final int mListPosition;
        private final int mListPositionOffset;

        /**
         * Constructor called from {@link DatePicker#onSaveInstanceState()}
         */
        private SavedState(Parcelable superState, int year, int month, int day,
                long minDate, long maxDate, int currentView, int listPosition,
                int listPositionOffset) {
            super(superState);
            mSelectedYear = year;
            mSelectedMonth = month;
            mSelectedDay = day;
            mMinDate = minDate;
            mMaxDate = maxDate;
            mCurrentView = currentView;
            mListPosition = listPosition;
            mListPositionOffset = listPositionOffset;
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);
            mSelectedYear = in.readInt();
            mSelectedMonth = in.readInt();
            mSelectedDay = in.readInt();
            mMinDate = in.readLong();
            mMaxDate = in.readLong();
            mCurrentView = in.readInt();
            mListPosition = in.readInt();
            mListPositionOffset = in.readInt();
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mSelectedYear);
            dest.writeInt(mSelectedMonth);
            dest.writeInt(mSelectedDay);
            dest.writeLong(mMinDate);
            dest.writeLong(mMaxDate);
            dest.writeInt(mCurrentView);
            dest.writeInt(mListPosition);
            dest.writeInt(mListPositionOffset);
        }

        public int getSelectedDay() {
            return mSelectedDay;
        }

        public int getSelectedMonth() {
            return mSelectedMonth;
        }

        public int getSelectedYear() {
            return mSelectedYear;
        }

        public long getMinDate() {
            return mMinDate;
        }

        public long getMaxDate() {
            return mMaxDate;
        }

        public int getCurrentView() {
            return mCurrentView;
        }

        public int getListPosition() {
            return mListPosition;
        }

        public int getListPositionOffset() {
            return mListPositionOffset;
        }

        @SuppressWarnings("all")
        // suppress unused and hiding
        public static final Parcelable.Creator<SavedState> CREATOR = new Creator<SavedState>() {

            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    private final View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.date_picker_month_and_day_layout) {
                setCurrentView(MONTH_AND_DAY_VIEW);
            } else if (id == R.id.date_picker_year) {
                setCurrentView(YEAR_VIEW);
            }
            tryVibrate();
        }
    };

    private static String getBestDatePattern(Locale locale) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return DateFormat.getBestDateTimePattern(locale, "yMMMd");
        } else {
            return DateTimeUtilsCompat.getBestDatePattern(locale);
        }
    }
}
