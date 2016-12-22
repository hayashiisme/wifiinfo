package jp.androidstudio.a04;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
	}

	@Override
	protected void onResume(){
		super.onResume();
		WifiManager manager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = manager.getConnectionInfo();
		WifiConfiguration conf = getWifiConfiguration(manager, info);
		List<Map<String, String>> list
			= getIpSettings(conf, info, getWifiSettings(manager, info, conf));
		SimpleAdapter adapter
			= new SimpleAdapter(this, list, android.R.layout.simple_list_item_2,
													new String[]{"Item", "Value"},
													new int[]{android.R.id.text1, android.R.id.text2});
		ListView listView = (ListView)findViewById(R.id.listView);
		listView.setAdapter(adapter);
	}

	private List<Map<String, String>> getWifiSettings
		(WifiManager manager, WifiInfo info, WifiConfiguration conf){
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		if(conf != null){
			list.add(addRow("SSID", conf.SSID));
			int rssi = info.getRssi();
			int level = manager.calculateSignalLevel(rssi, 5);
			list.add(addRow("RSSI",	String.format("%d (Level: %d/4)", rssi, level)));
			list.add(addRow("Link Speed",
											String.format("%d %s", info.getLinkSpeed(), WifiInfo.LINK_SPEED_UNITS)));
			list.add(addRow("Frequency",
											String.format("%d %s", info.getFrequency(), WifiInfo.FREQUENCY_UNITS)));
			String km = "(unknown)";
			if(conf.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.IEEE8021X)) km = "IEEE8021X";
			if(conf.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE)) km = "NONE";
			if(conf.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_EAP)) km = "WPA_EAP";
			if(conf.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK)) km = "WPA_PSK";
			list.add(addRow("Security", km));
			list.add(addRow("Hidden", (info.getHiddenSSID() ? "TRUE" : "FALSE")));
		}
		return list;
	}

	private List<Map<String, String>> getIpSettings
		(WifiConfiguration conf, WifiInfo info, List<Map<String, String>> list){
		//IPアドレス (WifiInfoから取る)
		int ip = info.getIpAddress();
		String ips = String.format
			("%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
		list.add(addRow("IP Address", ips));
		//IpConfigurationと参照フィールドを取得
		try{
			Class<?>[] parameterClasses = null;
			Method mGetIpConfiguration
				= conf.getClass().getDeclaredMethod("getIpConfiguration", parameterClasses);
			Object oIpConfiguration = mGetIpConfiguration.invoke(conf, (Object[])null);
			if(oIpConfiguration == null) return list;
			Field fProxySettings = oIpConfiguration.getClass().getDeclaredField("proxySettings");
			list.add(addRow("Proxy", fProxySettings.get(oIpConfiguration).toString()));
			Field fIpAssignment = oIpConfiguration.getClass().getDeclaredField("ipAssignment");
			String ipAssignment = fIpAssignment.get(oIpConfiguration).toString();
			list.add(addRow("IP Assginment", ipAssignment));
			if(ipAssignment.equals("STATIC")){		//STATICなら設定を表示する
				Field fStaticIpConfiguration
					= oIpConfiguration.getClass().getDeclaredField("staticIpConfiguration");
				list.add(addRow("Static IP Configuration",
												fStaticIpConfiguration.get(oIpConfiguration).toString()));
				
			}
		}catch(ReflectiveOperationException e){e.printStackTrace();}
		return list;
	}

	private Map<String, String> addRow(String item, String value){
		Map<String, String> row = new HashMap<String, String>();
		row.put("Item", item);
		row.put("Value", value);
		return row;
	}
	
	private WifiConfiguration getWifiConfiguration(WifiManager wm, WifiInfo wi){
		WifiConfiguration wc = null;
		List<WifiConfiguration> lwc = wm.getConfiguredNetworks();
		for(WifiConfiguration conf : lwc){
			if(conf.networkId == wi.getNetworkId()){
				wc = conf;
				break;
			}
		}
		return wc;
	}



	
}
