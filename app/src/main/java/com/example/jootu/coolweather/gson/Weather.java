package com.example.jootu.coolweather.gson;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Administrator on 2017/7/12.
 */
//对AQI.Basic等类进行引用
public class Weather {
    public String status;//该字段，成功返回OK，失败返回具体的原因
    public Basic basic;
    public AQI aqi;
    public Now now;
    public Forecast forecast;
    public Suggestion suggestion;

    //由于daily_forecast中包含的是一个数组，因此用List集合来引用Forecast类
    @SerializedName("daily_forecast")
    public List<Forecast> forecastList;
}
