package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class NavigatorActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent;
        DatabaseHelper myDB;

        myDB = new DatabaseHelper(this);
        ArrayList<String> theList = new ArrayList<>();
        Cursor data = myDB.getListContents();

        if(data.getCount() == 0){
            intent = new Intent(NavigatorActivity.this, EmptyScanActivity.class);
        }else{
            intent = new Intent(NavigatorActivity.this, ScanListActivity.class);
        }

        startActivity(intent);
        finish();

    }
}
