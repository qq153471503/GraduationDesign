package com.example.graduationdesign.thread;

import android.nfc.Tag;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.graduationdesign.activity.HomePageActivity;

/**
 * Created by KunGe on 2017/9/7.
 */

public class UpdateTimeThread extends Thread {
    private Handler handler;

    public UpdateTimeThread(Handler handler) {
        this.handler = handler;
    }

    @Override
    public void run() {

        do {
            Log.d(HomePageActivity.TAG, "in Update Time Thread..");
            Message msg = new Message();
            msg.what = HomePageActivity.MSG_UPDATE_TIME;
            handler.sendMessage(msg);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }while (true);
    }
}
