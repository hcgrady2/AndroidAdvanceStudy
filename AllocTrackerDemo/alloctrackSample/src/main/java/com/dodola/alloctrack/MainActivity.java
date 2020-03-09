package com.dodola.alloctrack;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private AllocTracker tracker = new AllocTracker();

    private Button dumpLogBtn;
    private static final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 100;
    private File externalReportPath;


    private static final String TAG = "alloctracker";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_EXTERNAL_STORAGE_REQUEST_CODE);

            Log.i(TAG, "onCreate: 没有获取到授权");
        } else {
            Log.i(TAG, "onCreate: 有授权");
            initExternalReportPath();
        }
        tracker.initForArt(BuildConfig.VERSION_CODE, 500);//从 start 开始触发到5000的数据就 dump 到文件中

        dumpLogBtn = findViewById(R.id.dump_log);
        findViewById(R.id.btn_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //正式开始记录的，主要看这个记录
                tracker.startAllocationTracker();

                dumpLogBtn.setEnabled(true);
            }
        });


        findViewById(R.id.btn_stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tracker.stopAllocationTracker();
                dumpLogBtn.setEnabled(false);

            }
        });


        dumpLogBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        tracker.dumpAllocationDataInLog();
                    }
                }).start();
            }
        });


        findViewById(R.id.gen_obj).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                Log.i(TAG, "onClick: 开始生成对象");
                for (int i = 0; i < 600; i++) {
                   Message msg = new Message();
                    msg.what = i;
                 //   Button button = new Button(MainActivity.this);

                }

                Log.i(TAG, "onClick: 生成对象完毕");
            }
        });
    }


    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        initExternalReportPath();
    }

    private void initExternalReportPath() {

        Log.i(TAG, "initExternalReportPath: 开始初始化目录");

        externalReportPath = new File(Environment.getExternalStorageDirectory(), "crashDump");

        if (!externalReportPath.exists()) {
            externalReportPath.mkdirs();
        }

        Log.i(TAG, "initExternalReportPath: 开始设置目录");
        tracker.setSaveDataDirectory(externalReportPath.getAbsolutePath());

        Log.i(TAG, "initExternalReportPath: 获取授权之后，设置完毕");

    }
}
