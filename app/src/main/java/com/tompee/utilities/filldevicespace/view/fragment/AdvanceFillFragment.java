package com.tompee.utilities.filldevicespace.view.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.tompee.utilities.filldevicespace.R;
import com.tompee.utilities.filldevicespace.controller.storage.StorageUtility;
import com.tompee.utilities.filldevicespace.controller.task.FillDiskTask;
import com.tompee.utilities.filldevicespace.view.MainActivity;

import at.grabner.circleprogress.CircleProgressView;

public class AdvanceFillFragment extends Fragment implements View.OnClickListener,
        FillDiskTask.FillDiskTaskListener {
    private FillDiskTask mFillDiskTask;
    private TextView mFreeView;
    private TextView mFillView;
    private TextView mSpeedView;
    private View mClearFillView;
    private View mSdCardView;
    private FloatingActionButton mStartButton;
    private CircleProgressView mCircleProgressView;
    private CircleProgressView mMegaBytes;
    private CircleProgressView mGigaBytes;
    private ViewSwitcher mViewSwitcher;
    private long mCurrentMbValue;
    private long mCurrentGbValue;
    private long mTotalValue;

    private SharedPreferences mSharedPrefs;

    public static AdvanceFillFragment getInstance() {
        return new AdvanceFillFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSharedPrefs = getContext().getSharedPreferences(MainActivity.
                SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_advance_fill, container, false);
        mStartButton = (FloatingActionButton) view.findViewById(R.id.start);
        mStartButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.
                getColor(getContext(), R.color.colorAccentLight)));
        mStartButton.setImageResource(R.drawable.ic_play_arrow_white);
        mStartButton.setOnClickListener(this);

        mClearFillView = view.findViewById(R.id.clear_fill);
        mClearFillView.setOnClickListener(this);
        mSdCardView = view.findViewById(R.id.sd_card);
        mSdCardView.setOnClickListener(this);
        setSdCardState();

        mFreeView = (TextView) view.findViewById(R.id.free_space);
        mFillView = (TextView) view.findViewById(R.id.fill_space);
        mSpeedView = (TextView) view.findViewById(R.id.speed);
        mCircleProgressView = (CircleProgressView) view.findViewById(R.id.circleView);
        mCircleProgressView.setBarColor(ContextCompat.getColor(getContext(), R.color.colorAccentLight),
                ContextCompat.getColor(getContext(), R.color.colorAccent));

        mMegaBytes = (CircleProgressView) view.findViewById(R.id.megabytes);
        mMegaBytes.setOnProgressChangedListener(new CircleProgressView.OnProgressChangedListener() {
            @Override
            public void onProgressChanged(float value) {
                mCurrentMbValue = (long) ((int) value) * 1048576L;
                computeTotalValue();
            }
        });
        mGigaBytes = (CircleProgressView) view.findViewById(R.id.gigabytes);
        mGigaBytes.setOnProgressChangedListener(new CircleProgressView.OnProgressChangedListener() {
            @Override
            public void onProgressChanged(float value) {
                mCurrentGbValue = (long) ((int) value) * 1073741824L;
                computeTotalValue();
            }
        });
        updateViews(0.00f);
        return view;
    }

    private void updateViews(float speed) {
        if (mFreeView != null) {
            long free = StorageUtility.getAvailableStorageSize(getContext());
            mFreeView.setText(Formatter.formatFileSize(getContext(), free));
            mFillView.setText(Formatter.formatFileSize(getContext(), StorageUtility.
                    getFillSize(getContext())));
            mSpeedView.setText(String.format(getString(R.string.ids_legend_speed_unit), speed));
            long total = StorageUtility.getTotalStorageSize(getContext());
            float percentage = ((float) (total - free) / (float) total);
            mCircleProgressView.setValue(percentage * 100);
        }
    }

    @Override
    public void onClick(View view) {
        MainActivity activity = (MainActivity) getActivity();
        switch (view.getId()) {
            case R.id.start:
                FloatingActionButton fab = (FloatingActionButton) view;
                if (mFillDiskTask == null) {
                    mClearFillView.setEnabled(false);
                    activity.interceptViewPagerTouchEvents(true);
                    mSdCardView.setEnabled(false);
                    fab.setImageResource(R.drawable.ic_stop_white);
                    mFillDiskTask = new FillDiskTask(getContext(), this, mTotalValue);
                    mFillDiskTask.execute();
                } else {
                    fab.setImageResource(R.drawable.ic_play_arrow_white);
                    mFillDiskTask.cancel(true);
                }
                break;
        }
    }

    private void setSdCardState() {
        if (StorageUtility.getRemovableStorage(getContext()) != null) {
            mSdCardView.setEnabled(true);
        } else {
            mSdCardView.setEnabled(false);
            SharedPreferences.Editor editor = mSharedPrefs.edit();
            editor.putBoolean(MainActivity.TAG_SD_CARD, false);
            editor.apply();
        }
        mSdCardView.setBackgroundColor(mSharedPrefs.getBoolean(MainActivity.TAG_SD_CARD, false) ?
                ContextCompat.getColor(getContext(), R.color.tabSelected) : ContextCompat.
                getColor(getContext(), android.R.color.transparent));
    }

    private void computeTotalValue() {
        mTotalValue = mCurrentGbValue + mCurrentMbValue;
        if (mTotalValue > StorageUtility.getAvailableStorageSize(getContext())) {
            mStartButton.setEnabled(false);
        } else {
            mStartButton.setEnabled(true);
        }
    }

    @Override
    public void onFillDiskSpaceComplete() {
        updateViews(0.00f);
        mFillDiskTask = null;
        mClearFillView.setEnabled(true);
        setSdCardState();
        MainActivity activity = (MainActivity) getActivity();
        activity.interceptViewPagerTouchEvents(false);
    }

    @Override
    public void onProgressUpdate(float speed) {
        updateViews(speed);
    }

    @Override
    public void onCancelled() {
        updateViews(0.00f);
        mFillDiskTask = null;
        mClearFillView.setEnabled(true);
        setSdCardState();
        MainActivity activity = (MainActivity) getActivity();
        activity.interceptViewPagerTouchEvents(false);
    }
}
