package com.sportmate.app.presentation;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.sportmate.app.R;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public final class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}