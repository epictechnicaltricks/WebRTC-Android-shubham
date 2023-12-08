package com.shivam.androidwebrtc;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.View;

import com.myhexaville.androidwebrtc.R;
import com.shivam.androidwebrtc.tutorial.CompleteActivityAUDIO;
import com.shivam.androidwebrtc.tutorial.CompleteActivityBOTH;

public class LauncherActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }
    public void openSampleSocketActivity(View view) {
        startActivity(new Intent(this, CompleteActivityAUDIO.class));

    }
}
