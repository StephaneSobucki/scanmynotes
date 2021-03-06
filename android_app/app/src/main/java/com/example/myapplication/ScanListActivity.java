package com.example.myapplication;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import static android.os.Environment.getExternalStoragePublicDirectory;

public class ScanListActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    DatabaseHelper myDB;
    String filePath;
    String scannedFilePath;

    Bitmap photo;
    File photoFile = null;

    ImageButton btnTakePic;
    ImageButton btnOpenGallery;
    ProgressBar progressBar;
    ListAdapter listAdapter;
    ArrayList<String> theList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_list_activity);

        btnTakePic = findViewById(R.id.btnTakePic);
        btnTakePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchPictureTakerAction();
            }
        });
        btnOpenGallery = findViewById(R.id.btnOpenGallery);
        progressBar = findViewById(R.id.progressBar3);
        progressBar.setVisibility(View.GONE);
        btnOpenGallery.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                dispatchGalleryOpenerAction();
            }
        });

        final ListView listView = (ListView) findViewById(R.id.listview);
        listView.setOnItemClickListener(this);

        myDB = new DatabaseHelper(this);

        theList = new ArrayList<>();
        Cursor data = myDB.getListContents();

        if(data.getCount() == 0){
            System.out.println("Database is empty");
        }else{
            while(data.moveToNext()){
                theList.add(data.getString(1));
            }
        }
        listAdapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,theList);
        listView.setAdapter(listAdapter);

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, final long id) {
                final int which_item = position;
                new AlertDialog.Builder(ScanListActivity.this)
                        .setIcon(android.R.drawable.ic_delete)
                        .setTitle("Etes-vous sur ?")
                        .setMessage("Voulez-vous supprimer ce scan ?")
                        .setPositiveButton("Oui", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String currentTitle = theList.get(which_item);
                                myDB.deleteData(theList.get(which_item));
                                Cursor data = myDB.getListContents();

                                if(data.getCount() == 0){
                                    theList.clear();
                                    Intent intent = new Intent(ScanListActivity.this,NavigatorActivity.class);
                                    startActivity(intent);
                                    finish();
                                }else{
                                    theList.clear();
                                    while(data.moveToNext()){
                                        if(!data.getString(1).equals(currentTitle)) {
                                            theList.add(data.getString(1));
                                        }
                                    }
                                }
                                ((BaseAdapter) listAdapter).notifyDataSetChanged();
                            }
                        })
                        .setNegativeButton("Non",null)
                        .show();
                return true;
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Cursor data = myDB.getDataAt(theList.get(position));
        data.moveToFirst();
        Intent i = new Intent(ScanListActivity.this, OpenScanActivity.class);
        String scannedText;
        scannedText = data.getString(data.getColumnIndex("ITEM2"));
        scannedFilePath = data.getString(data.getColumnIndex("ITEM3"));
        i.putExtra("key",scannedText);
        i.putExtra("filePath",scannedFilePath);
        startActivity(i);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && requestCode == 1){
            Uri uri = Uri.parse("file:" + filePath);
            openCropActivity(uri,uri);
        }
        else if (resultCode == RESULT_OK && requestCode == 2){
            filePath = getPathFromURI(data.getData());
            Uri uri = Uri.parse("file:" + filePath);
            openCropActivity(uri,uri);
        }
        else if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP){
            Uri resultUri = UCrop.getOutput(data);
            filePath = resultUri.toString().split("///",2)[1];
            photo = BitmapFactory.decodeFile(filePath);
            uploadImage();
        }
        else if (resultCode == UCrop.RESULT_ERROR){
            Throwable cropError = UCrop.getError(data);
        }
    }

    private void openCropActivity(Uri sourceUri, Uri destinationUri){
        UCrop.Options options = new UCrop.Options();
        options.setFreeStyleCropEnabled(true);
        options.setCropFrameColor(ContextCompat.getColor(this, R.color.colorAccent));
        UCrop.of(sourceUri, destinationUri)
                .start(this);
    }

    public String getPathFromURI(Uri contentUri) {
        String res = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
        }
        cursor.close();
        return res;
    }

    private void dispatchGalleryOpenerAction(){
        Intent openGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(openGallery,2);
    }
    private void dispatchPictureTakerAction() {
        Intent takePic = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePic.resolveActivity(getPackageManager()) != null) {
            photoFile = createPhotoFile();
            if (photoFile != null) {
                filePath = photoFile.getAbsolutePath();
                Uri photoURI = FileProvider.getUriForFile(ScanListActivity.this, "com.thecodecity.cameraandroid.fileprovider", photoFile);
                takePic.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePic, 1);
            }
        }
    }

    //method to store photo
    private File createPhotoFile() {
        String name = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        //File storageDir = getExternalFilesDir(); //Readable by app only
        File storageDir = getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = null;
        try {
            image = File.createTempFile(name, ".jpg", storageDir);
        }
        catch (IOException e){
            Log.d("mylog","Excep :" + e.toString());
        }
        return image;
    }

    private void uploadImage() {
        File file = new File(filePath);

        MultipartBody.Part filePart = MultipartBody.Part.createFormData("newimage", file.getName(), RequestBody.create(MediaType.parse("image/*"), file));
        RequestBody description = RequestBody.create(MediaType.parse("text/plain"), "This is a new image");
        Retrofit retrofit = NetworkClient.getRetrofit();
        UploadApis uploadApis = retrofit.create(UploadApis.class);
        Call<JsonElement> call = uploadApis.uploadImage(filePart, description);
        progressBar.setVisibility(View.VISIBLE);
        call.enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Call<JsonElement> call, final Response<JsonElement> response) {
                JsonObject jsonObject = new Gson().fromJson(response.body().toString(), JsonObject.class);
                String result = jsonObject.get("Predicted Class").toString();

                Intent i = new Intent(ScanListActivity.this, DataEntryActivity.class);
                i.putExtra("key",result);
                i.putExtra("filePath",filePath);
                startActivity(i);
                progressBar.setVisibility(View.GONE);
                finish();
            }

            @Override
            public void onFailure(Call call, Throwable t) {
                System.out.println(t.getMessage().toString());
            }

        });
    }
}


