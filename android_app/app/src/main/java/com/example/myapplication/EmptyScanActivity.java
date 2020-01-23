package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import retrofit2.Response;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;

import static android.os.Environment.getExternalStoragePublicDirectory;

public class EmptyScanActivity extends AppCompatActivity {
    String filePath;

    Bitmap photo;
    File photoFile = null;

    ImageButton btnTakePic;
    ImageButton btnOpenGallery;
    TextView textView;
    TextView textView2;
    ImageView imageView2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.empty_scan_activity);
        if (Build.VERSION.SDK_INT >= 23){
            requestPermissions(new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.ACCESS_NETWORK_STATE}, 2);
        }
        btnTakePic = findViewById(R.id.btnTakePic);
        btnTakePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchPictureTakerAction();
            }
        });
        btnOpenGallery = findViewById(R.id.btnOpenGallery);
        btnOpenGallery.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                dispatchGalleryOpenerAction();
            }
        });
        textView = findViewById(R.id.text);
        textView2 = findViewById(R.id.text2);
        textView2.setText("  Lancez une numérisation à partir de votre \n appareil photo ou de vos photos importées");
        textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
        textView.setText("Vous n'avez aucune numérisation.");
        imageView2 = findViewById(R.id.core_image);
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
            textView.setText(cropError.toString());
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
                Uri photoURI = FileProvider.getUriForFile(EmptyScanActivity.this, "com.thecodecity.cameraandroid.fileprovider", photoFile);
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
        call.enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Call<JsonElement> call, final Response<JsonElement> response) {
                JsonObject jsonObject = new Gson().fromJson(response.body().toString(), JsonObject.class);
                String result = jsonObject.get("Predicted Class").toString();

                Intent i = new Intent(EmptyScanActivity.this, DataEntryActivity.class);
                i.putExtra("key",result);
                i.putExtra("filePath",filePath);
                startActivity(i);
                finish();
            }

            @Override
            public void onFailure(Call call, Throwable t) {
                textView.setText(t.getMessage());
            }

        });
    }
}
