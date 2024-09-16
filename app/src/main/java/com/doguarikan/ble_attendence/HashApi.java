package com.doguarikan.ble_attendence;

import retrofit2.Call;
import retrofit2.http.GET;

public interface HashApi {
    @GET("c/04f3-2bfe-4933-9ca3")
    Call<Hash> get_hash();
}
