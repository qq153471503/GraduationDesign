package com.example.graduationdesign.activity;

import android.app.Activity;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.example.graduationdesign.R;
import com.example.graduationdesign.ctl.ControlFlag;
import com.example.graduationdesign.thread.UpdateTimeThread;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import pl.droidsonroids.gif.GifImageView;

public class HomePageActivity extends Activity {

    public final static int MSG_UPDATE_TIME = 0x01;

    public final static String TAG = "HomePageActivity";
    private TextView textView_Time;
    private TextView textView_ClikedTest;
    private Button buttonSwitchLed;
    private GifImageView gifImageView;
    private MyHandler myHandler = new MyHandler();
    private UpdateTimeThread updateTimeThread ;
    private boolean STATE = false;
    private OutputStream outputStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);
        Log.d(TAG, "onCreat");

        gifImageView  = (GifImageView)findViewById(R.id.id_gifImageView);
        gifImageView.setImageResource(R.drawable.heart);
        textView_Time = (TextView)findViewById(R.id.id_textView_time);
        textView_Time.setTextColor(Color.YELLOW);

        /**
         * 设置点击'点击测试...'响应事件
         */
        textView_ClikedTest = (TextView)findViewById(R.id.id_textView_clickedTest);
        textView_ClikedTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                STATE = !STATE;
                if (STATE){
                    try {
                        ConnectDevicesActivity.connectDevicesActivity.dataOutputStream.write(ControlFlag.HEART_SENSOR_START);
                        new Thread(new Runnable() {
                            int dat = 0;
                            @Override
                            public void run() {
                                try {
                                    for (;;) {
                                        dat = ConnectDevicesActivity.connectDevicesActivity.dataInputStream.read();
                                        final String cnt = String.valueOf(dat);
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                textView_ClikedTest.setText("BMP: " + cnt + "\n点击刷新");
                                                gifImageView.setImageResource(R.drawable.heart);
                                            }
                                        });
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    gifImageView.setImageResource(R.drawable.heart);
                }
                else {
                    gifImageView.setImageResource(R.drawable.heart2);
                    textView_ClikedTest.setText("测试中...");
                    try {
                        ConnectDevicesActivity.connectDevicesActivity.dataOutputStream.write(ControlFlag.HEART_SENSOR_STOP);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        /**
         * 设置按钮响应事件,打开/关闭LED灯
         */
        buttonSwitchLed = (Button)findViewById(R.id.id_button_ledSwitch);
        buttonSwitchLed.setOnClickListener(new View.OnClickListener() {
            private boolean state = false;
            @Override
            public void onClick(View view) {
                state = !state;
                if (state){
                    try {
                        ConnectDevicesActivity.connectDevicesActivity.dataOutputStream.write(ControlFlag.LED_ON);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    buttonSwitchLed.setBackgroundResource(R.drawable.button_on);

                }else {
                    try {
                        ConnectDevicesActivity.connectDevicesActivity.dataOutputStream.write(ControlFlag.LED_OFF);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    buttonSwitchLed.setBackgroundResource(R.drawable.button_off);
                }
            }
        });

        updateTimeThread = new UpdateTimeThread(myHandler);
        updateTimeThread.start();
    }

    class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case HomePageActivity.MSG_UPDATE_TIME:
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy年MM月dd日 \n HH:mm:ss");
                    Date curDate = new Date(System.currentTimeMillis());
                    String str = formatter.format(curDate);
                    textView_Time.setText(str);
                    Log.d(TAG, str);
                    break;
                default:break;
            }
        }
    }
}



