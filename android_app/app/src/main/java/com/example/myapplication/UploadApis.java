package com.example.myapplication;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Url;

public interface UploadApis {
    @Multipart
    @POST("/")
    //Call <JsonElement> uploadImage(@Part MultipartBody.Part part, @Part("somedata") RequestBody requestBody);
    Call<ResponseBody> uploadImage(@Part MultipartBody.Part part, @Part("somedata") RequestBody requestBody);

    @GET("/")
    Call<ResponseBody> downloadFilewithDynamicUrlSync(@Url String FileUrl);
}
