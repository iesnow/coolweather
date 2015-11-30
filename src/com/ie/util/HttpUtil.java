package com.ie.util;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

public class HttpUtil {
	/*
	 * 发送请求
	 */
	public static void sendHttpRequest(final String address,
			final HttpCallbackListener listener) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					HttpClient httpClient = new DefaultHttpClient();
					HttpGet httpGet = new HttpGet(address);
					// httpResponse存放请求之后服务器返回的数据
					HttpResponse httpResponse = httpClient.execute(httpGet);
					if (httpResponse.getStatusLine().getStatusCode() == 200) {
						// 请求和响应都成功了
						HttpEntity entity = httpResponse.getEntity();
						// utf-8表示可以有中文
						String response = EntityUtils.toString(entity, "utf-8");
						if (listener!=null) {
							listener.onFinish(response);
						}
					}
				} catch (Exception e) {
					if (listener != null) {
						listener.onError(e);
					}
				}
			}
		}).start();
	}

}
