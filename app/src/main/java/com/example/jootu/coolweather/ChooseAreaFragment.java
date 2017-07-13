package com.example.jootu.coolweather;


import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.jootu.coolweather.db.City;
import com.example.jootu.coolweather.db.County;
import com.example.jootu.coolweather.db.Province;
import com.example.jootu.coolweather.util.HttpUtil;
import com.example.jootu.coolweather.util.Utility;
import org.litepal.crud.DataSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * A simple {@link Fragment} subclass.
 */
public class ChooseAreaFragment extends Fragment {

    public static final int LEVEL_PROVINCE=0;
    public static final int LEVEL_CITY=1;
    public static final int LEVEL_COUNTY=2;
    private ProgressDialog progressDialog;
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList=new ArrayList<>();

    //省、市、县列表
    private List<Province> provinceList;
    private List<City> cityList;
    private List<County> countyList;
    //选中的省份，城市，级别
    private Province selectedProvince;
    private City selectedCity;
    //当前选中的级别
    private int currentLevel;




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view =inflater.inflate(R.layout.choose_area,container,false);
        //获取控件的实例
        titleText=(TextView)view.findViewById(R.id.title_text);
        backButton=(Button)view.findViewById(R.id.back_button);
        listView=(ListView)view.findViewById(R.id.list_view);
        //初始化ArrayAdapter，并将它设置为listView的适配器
        adapter=new ArrayAdapter<>(getContext(),android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        return view;
    }

    //给Button和listview设置点击事件
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //由于适配器给了listview，所以点击事件为setOnItemClickListener，而且是AdapterView
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(currentLevel==LEVEL_PROVINCE){//当前选中的级别是省级
                    selectedProvince=provinceList.get(position);
                    queryCities();//加载市级数据
                }else if(currentLevel==LEVEL_CITY){
                    selectedCity=cityList.get(position);
                    queryCounties();
                }else if(currentLevel==LEVEL_COUNTY){
                    String weatherId=countyList.get(position).getWeatherId();

                    //在MainActivity和WeatherActivity中都用到了ChooseAreaFragment，所以需要判断是哪里用的

                    //instanceof:判断某一碎片在MainActivity中还是WeatherActivity
                    if(getActivity() instanceof  MainActivity){
                        Intent intent=new Intent(getActivity(),WeatherActivity.class);
                        intent.putExtra("weather_id",weatherId);//将当前选中的县的天气id传递过去
                        startActivity(intent);
                        getActivity().finish();
                    }else if(getActivity() instanceof WeatherActivity){
                        WeatherActivity activity=(WeatherActivity) getActivity();
                        activity.drawerLayout.closeDrawers();//关闭滑动菜单
                        activity.swipeRefresh.setRefreshing(true);//设置下拉刷新
                        activity.requestWeather(weatherId);//重新请求数据
                    }


                }
            }
        });
        //为backbutton设置点击事件
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentLevel==LEVEL_COUNTY){
                    queryCities();
                }else if(currentLevel==LEVEL_CITY){
                    queryProvinces();
                }
            }
        });
        queryProvinces();//加载省级数据
    }

    //查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器查询
    private void queryProvinces(){
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);//设置当前标题为中国，也就是下面加载的是省份的时候没有返回项
        provinceList= DataSupport.findAll(Province.class);
        if(provinceList.size()>0){//先查数据库
            dataList.clear();
            for(Province province:provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_PROVINCE;
        }else{//数据库没有再查服务器
            String address="http://guolin.tech/api/china";//组装出一个请求地址
            queryFromServer(address,"province");
        }

    }

    //查询选中省内所有的市，优先从数据库查询，如果没有查询到再去服务器查询
    private void queryCities(){
        titleText.setText(selectedProvince.getProvinceName());//获取当前省的名字
        backButton.setVisibility(View.VISIBLE);//将返回键显示出来

        cityList= DataSupport.where("provinceid=?",String.valueOf(selectedProvince.getId())).find(City.class);
        if(cityList.size()>0){//先查数据库
            dataList.clear();
            for(City city:cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_CITY;
        }else{//数据库没有再查服务器
            int provinceCode=selectedProvince.getProvinceCode();
            String address="http://guolin.tech/api/china/"+provinceCode;//组装出一个请求地址
            queryFromServer(address,"city");
        }

    }


    //查询选中市内所有的县，优先从数据库查询，如果没有查询到再去服务器查询
    private void queryCounties(){
        titleText.setText(selectedCity.getCityName());//获取当前市的名字
        backButton.setVisibility(View.VISIBLE);//将返回键显示出来
        countyList= DataSupport.where("cityid=?",String.valueOf(selectedCity.getId())).find(County.class);
        if(countyList.size()>0){//先查数据库
            dataList.clear();
            for(County county:countyList){
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_COUNTY;
        }else{//数据库没有再查服务器
            int provinceCode=selectedProvince.getProvinceCode();
            int cityCode= selectedCity.getCityCode();
            String address="http://guolin.tech/api/china/"+provinceCode+"/"+cityCode;//组装出一个请求地址
            queryFromServer(address,"county");
        }

    }

    //根据传入的地址和类型，从服务器上查询省市县数据
    private void queryFromServer(String address,final String type){
        showProgressDialog();
        //从服务器查询
        HttpUtil.sendOkHttpRequest(address, new Callback() {

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText=response.body().string();
                boolean result=false;
                if("province".equals(type)){
                    result= Utility.handleProvinceResponse(responseText);
                }else if("city".equals(type)){
                    result=Utility.handleCityResponse(responseText,selectedProvince.getId());
                }else if("county".equals(type)){
                    result=Utility.handleCountyResponse(responseText,selectedCity.getId());
                }

                if(result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if("province".equals(type)){
                                queryProvinces();
                            }else if("city".equals(type)){
                                queryCities();
                            }else if("county".equals(type)){
                                queryCounties();
                            }
                        }
                    });
                }
            }
            @Override
            public void onFailure(Call call, IOException e) {
                //通过runOnUiThread（）方法回到主线程处理逻辑
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
    //显示进度对话框
    private void showProgressDialog(){
        if(progressDialog==null){
            progressDialog=new ProgressDialog(getActivity());
            progressDialog.setMessage("load...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    //关闭进度对话框
    private void closeProgressDialog(){
        if(progressDialog!=null){
            progressDialog.dismiss();
        }
    }




}
