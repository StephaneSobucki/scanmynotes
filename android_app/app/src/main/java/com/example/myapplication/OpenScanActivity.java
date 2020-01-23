package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import static android.os.Environment.getExternalStoragePublicDirectory;

public class OpenScanActivity extends AppCompatActivity {

    String text;
    TextView textView;
    Button button;
    String pdfPath;
    Uri path;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.open_scan_activity);
        Bundle extras = getIntent().getExtras();
        text = extras.getString("key");
        textView = findViewById(R.id.textView);
        textView.setText(text);
        button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadPdf();
            }
        });
    }

    public void downloadPdf(){
        Retrofit retrofit = NetworkClient.getRetrofit();
        UploadApis uploadApis = retrofit.create(UploadApis.class);
        Call<ResponseBody> call = uploadApis.getStringScalar(text);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, final Response<ResponseBody> response) {
                boolean writtentodisk = writeResponseBodyToDisk(response.body());
                if(writtentodisk){
                    Intent intent = new Intent();
                    intent.setPackage("com.adobe.reader");
                    intent.setDataAndType(path,"application/pdf");
                    startActivity(intent);
                }
            }

            @Override
            public void onFailure(Call call, Throwable t) {
                textView.setText(t.getMessage());
            }

        });
    }

    private boolean writeResponseBodyToDisk(ResponseBody body) {
        try {
            // todo change the file location/name according to your needs
            pdfPath = getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "output.pdf";
            File futureStudioIconFile = new File(pdfPath);
            path = Uri.fromFile(futureStudioIconFile);

            System.out.println(futureStudioIconFile.getParent());

            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                byte[] fileReader = new byte[4096];

                long fileSizeDownloaded = 0;

                inputStream = body.byteStream();
                outputStream = new FileOutputStream(futureStudioIconFile);

                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }

                    outputStream.write(fileReader, 0, read);

                    fileSizeDownloaded += read;

                }

                outputStream.flush();

                return true;
            } catch (IOException e) {
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            return false;
        }
    }
}
