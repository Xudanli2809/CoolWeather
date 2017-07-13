package com.example.jootu.coolweather.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Administrator on 2017/7/12.
 */

public class AQI {
    @SerializedName("city")
    public AQICity city;
    public class AQICity{
        public String aqi;
        public String pm25;
    }
}
