package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DataEntryActivity extends AppCompatActivity {

    DatabaseHelper myDB;

    TextView textView2;
    TextView textView4;
    Button button2;
    ImageView imageView2;
    String value;
    String name;
    String filePath;
    Bitmap photo;
    String date;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.data_entry_activity);
        Bundle extras = getIntent().getExtras();
        textView2 = findViewById(R.id.textView2);
        textView4 = findViewById(R.id.textView4);
        imageView2 = findViewById(R.id.core_image);
        date = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        name = "Scan du " + date;
        textView4.setText(name);
        value = extras.getString("key");
        filePath = extras.getString("filePath");
        photo = BitmapFactory.decodeFile(filePath);
        imageView2.setImageBitmap(photo);
        System.out.println(value);
        textView2.setText(value);
        button2 = findViewById(R.id.checkBox);
        myDB = new DatabaseHelper(this);

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(DataEntryActivity.this, ScanListActivity.class);
                value = textView2.getText().toString();
                name = textView4.getText().toString();
                addData(name,value,filePath,date);
                startActivity(i);
                finish();
            }
        });
    }

    public void addData(String title, String content, String filePath, String date){
        boolean insertData = myDB.addData(title, content, filePath, date);
        if(insertData == true){
            System.out.println("Data added to the database!");
        }
    }
}
