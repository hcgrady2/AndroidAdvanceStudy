package com.hc.duplicatedbitmapanalyze;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 100;
    private File externalReportPath;

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
        } else {
            initExternalReportPath();
        }

        Bitmap bitmap1 = BitmapFactory.decodeResource(getResources(), R.mipmap.hc);
        Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(), R.mipmap.hc);

        ((ImageView)findViewById(R.id.iv_1)).setImageBitmap(bitmap1);
        ((ImageView)findViewById(R.id.iv_2)).setImageBitmap(bitmap2);

        findViewById(R.id.btn_dump).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dump();
            }
        });
    }

    public void dump() {
        // 手动触发GC后获取hprof文件
        Runtime.getRuntime().gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new AssertionError();
        }
        System.runFinalization();
        try {
            File file = new File(externalReportPath.getAbsolutePath(),"dump.hprof");
            if(!file.exists()){
                file.createNewFile();
            }
            // 生成Hprof文件
            Debug.dumpHprofData(file.getAbsolutePath());
            Toast.makeText(this, "path: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        initExternalReportPath();
    }

    private void initExternalReportPath() {
        externalReportPath = new File(Environment.getExternalStorageDirectory(), "bitmapAnalyzer");
        if (!externalReportPath.exists()) {
            externalReportPath.mkdirs();
        }

    }
}
