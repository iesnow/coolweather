package com.ie.activity;

import java.util.ArrayList;
import java.util.List;
import com.ie.coolweather.R;
import com.ie.db.CoolWeatherDB;
import com.ie.model.City;
import com.ie.model.County;
import com.ie.model.Province;
import com.ie.util.HttpCallbackListener;
import com.ie.util.HttpUtil;
import com.ie.util.Utility;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ChooseAreaActivity extends Activity{
	public static final int LEVEL_PROVINCE=0;
	public static final int LEVEL_CITY=1;
	public static final int LEVEL_COUNTY=2;
	
	private ProgressDialog progressDialog;
	private TextView titleText;
	private ListView listView;
	private ArrayAdapter<String> adapter;
	private CoolWeatherDB coolWeatherDB;
	private List<String> dataList=new ArrayList<String>();
	//省、市、县列表
	private List<Province> provinceList;
	private List<City> cityList;
	private List<County> countyList;
	//选中的省份、市
	private Province selectedProvince;
	private City selectedCity;
	//当前选中的级别
	private int currentLevel;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.choose_area);
		listView=(ListView) findViewById(R.id.list_view);
		titleText=(TextView) findViewById(R.id.title_text);
		adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,dataList);
		listView.setAdapter(adapter);
		coolWeatherDB=CoolWeatherDB.getInstance(this);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (currentLevel==LEVEL_PROVINCE) {
					selectedProvince=provinceList.get(position);
					queryCities();
				}else if (currentLevel==LEVEL_CITY) {
					selectedCity=cityList.get(position);
					queryCounties();
				}
			}
		});
		queryProvinces();//加载省级数据
	}
	/*
	 * 查询全国所有的省，优先从数据库查询，如果没有查询到就去服务器查询
	 */
	private void queryProvinces(){
		provinceList=coolWeatherDB.loadProvinces();
		if (provinceList.size()>0) {
			dataList.clear();
			for (Province province : provinceList) {
				dataList.add(province.getProvinceName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);//让listview定位到第一个item上
			titleText.setText("中国");
			currentLevel=LEVEL_PROVINCE;
		}else{
			queryFromServer(null,"province");
		}
	}
	/*
	 * 查询所选中的省内所有的市，优先从数据库查询，如果查不到，就去服务器查询
	 */
	private void queryCities(){
		cityList=coolWeatherDB.loadCities(selectedProvince.getId());
		if (cityList.size()>0) {
			dataList.clear();
			for (City city : cityList) {
				dataList.add(city.getCityName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedProvince.getProvinceName());
			currentLevel=LEVEL_CITY;
		}else {
			queryFromServer(selectedProvince.getProvinceCode(),"city");
		}
	}
	/*
	 * 查询所选中的市内的所有的县，优先从数据库查询，如果查不到，就去服务器查询
	 */
	private void queryCounties(){
		countyList=coolWeatherDB.loadCounties(selectedCity.getId());
		if (countyList.size()>0) {
			dataList.clear();
			for (County county : countyList) {
				dataList.add(county.getCountyName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedCity.getCityName());
			currentLevel=LEVEL_COUNTY;
		}else {
			queryFromServer(selectedCity.getCityCode(), "county");
		}
	}
	/*
	 * 根据传入的代号和类型从服务器上查询省市县数据
	 */
	private void queryFromServer(final String code,final String type){
		String address;
		if (!TextUtils.isEmpty(code)) {
			address="http://www.weather.com.cn/data/list3/city"+code+".xml";
		}else {
			address="http://www.weather.com.cn/data/list3/city.xml";
		}
		showProgressDialog();
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
			@Override
			public void onFinish(String response) {
				boolean result=false;
				if ("province".equals(type)) {
					result=Utility.handleProvincesResponse(coolWeatherDB, response);
				}else if ("city".equals(type)) {
					result=Utility.handleCitiesResponse(coolWeatherDB, response, 
							selectedProvince.getId());
				}else if ("county".equals(type)) {
					result=Utility.handleCountiesResponse(coolWeatherDB, response, 
							selectedCity.getId());
				}
				if (result) {
					//通过runOnUiThread()方法回到主线程处理逻辑
					//第二种更新UI的方法
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							closeProgressDialog();
							if ("province".equals(type)) {
								queryProvinces();
							}else if ("city".equals(type)) {
								queryCities();
							}else if ("county".equals(type)) {
								queryCounties();
							}
						}
					});
				}
			}
			@Override
			public void onError(Exception e) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						//通过runOnUiThread()回到主线程处理
						closeProgressDialog();
						Toast.makeText(ChooseAreaActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
					}
				});
			}
		});
	}
	/*
	 * 显示进度对话框
	 */
	private void showProgressDialog(){
		if (progressDialog==null) {
			progressDialog=new ProgressDialog(this);
			progressDialog.setMessage("正在加载......");
			progressDialog.setCancelable(false);
		}
		progressDialog.show();
	}
	/*
	 * 关闭对话框
	 */
	private void closeProgressDialog(){
		if (progressDialog!=null) {
			progressDialog.dismiss();
		}
	}
	/*
	 * 捕获Back按键，根据当前的级别来判断，是返回市、省列表，还是直接退出
	 */
	@Override
	public void onBackPressed() {
		if (currentLevel==LEVEL_COUNTY) {
			queryCities();
		}else if (currentLevel==LEVEL_CITY) {
			queryProvinces();
		}else {
			finish();
		}
	}
}
