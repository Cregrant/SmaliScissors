package com.examples.ui;

import com.examples.util.ArrayUtils;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.concurrent.SynchronousQueue;

public class UIInputStream extends InputStream {
    private static SynchronousQueue<String> mInputData = new SynchronousQueue<String>();
    private LinkedList<Character> mChars = new LinkedList<Character>();
    private volatile boolean mWaits;
    private static final String NEW_LINE = System.getProperty("line.separator").toString();
    private boolean mEnd;
    private OnInputWaitsListener mOnInputWaitsListener;

    public interface OnInputWaitsListener {
        void onInputWaits();
    }

    public void addText(String text) {
        if (mWaits) {
            try {
                mInputData.put(text);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int read() {
        if (mEnd) {
            mEnd = false;
            return -1;
        }

        int size = mChars.size();
        if (size == 0) {
            getChars();
            size = mChars.size();
        }

        if (size == 1)
            mEnd = true;

        return mChars.poll();
    }

    private void getChars() {
        try {
            if (mOnInputWaitsListener != null)
                mOnInputWaitsListener.onInputWaits();
            mWaits = true;
            String line = mInputData.take() + NEW_LINE;
            mWaits = false;
            mChars.addAll(ArrayUtils.<Character>toList(line.toCharArray()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setOnInputWaitsListener(OnInputWaitsListener listener) {
        mOnInputWaitsListener = listener;
    }
}
