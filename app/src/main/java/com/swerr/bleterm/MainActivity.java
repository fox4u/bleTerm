package com.swerr.bleterm;





import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.Toast;

@SuppressLint("NewApi")
public class MainActivity extends Activity {
    private static String LOG_TAG = "MainActivity";
    private static final String uuid_template = "0000%04x-0000-1000-8000-00805f9b34fb";
    private static final String addr_uuid_tempplate = "%s -- %04x";

    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		 mHandler = new Handler();
		 
		 if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
	            Toast.makeText(this,"没有蓝牙", Toast.LENGTH_SHORT).show();
	            finish();
	        }
		 
		 // 初始化一个蓝牙适配器。对API 18级以上，可以参考 bluetoothmanager。
	        final BluetoothManager bluetoothManager =
	                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
	        mBluetoothAdapter = bluetoothManager.getAdapter();

	       //  检查是否支持蓝牙的设备。
	        if (mBluetoothAdapter == null) {
	            Toast.makeText(this,"设备不支持", Toast.LENGTH_SHORT).show();
	            finish();
	            return;
	        }
	        
	        
	        Button btstart=(Button) findViewById(R.id.btstart);
	        Button btstop=(Button) findViewById(R.id.btstop);
	        bar = (ProgressBar) findViewById(R.id.bar);
	        
	        
			bar.setVisibility(View.GONE);
	        
	        
	        
	        btlist = (ListView) findViewById(R.id.list);
	        listItem = new ArrayList<HashMap<String, Object>>();  
	        adapter = new SimpleAdapter(this,listItem,android.R.layout.simple_expandable_list_item_2,
	        new String[]{"name","addr_uuid"},new int[]{android.R.id.text1,android.R.id.text2});
   	        btlist.setAdapter(adapter);
   	        
   	        
   	        btlist.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {
					// TODO Auto-generated method stub
                    String address = (String) listItem.get(arg2).get("address");
                    String name = (String) listItem.get(arg2).get("name");
                    int uuid = (Integer) listItem.get(arg2).get("uuid");

					Intent intent=new Intent(getApplicationContext(), BlueCont.class);
			
					intent.putExtra("address",address);
					intent.putExtra("name",name);
                    if (uuid != -1)
                    {
                        String serv_uuid = String.format(Locale.US, uuid_template, uuid);
                        String char_uuid = String.format(Locale.US, uuid_template, uuid + 1);
                        intent.putExtra("serv_uuid",serv_uuid);
                        intent.putExtra("char_uuid",char_uuid);
                        Log.w(LOG_TAG, "uuid:" + uuid + ", char:" + char_uuid);
                    }

                    if (mScanning)
                    {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                        mScanning = false;
                    }

					startActivity(intent);
				}
			});
   	        
	        btstart.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View arg0) {
					// TODO Auto-generated method stub
					scanLeDevice(true);
					Log.e(LOG_TAG, "开始搜寻");
				}
			});
	        
	        btstop.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View arg0) {
					// TODO Auto-generated method stub
					scanLeDevice(false);
					Log.e(LOG_TAG, "停止");
				}
			});
	}
	@Override
	protected void onResume() {
	// TODO Auto-generated method stub
	super.onResume();
	
	//确保蓝牙是在设备上启用。如果当前没有启用蓝牙，
    //火意图显示一个对话框询问用户授予权限以使它。
	      if (!mBluetoothAdapter.isEnabled()) {
	          if (!mBluetoothAdapter.isEnabled()) {
	              Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	              startActivityForResult(enableBtIntent, 1);
	          }
	      }
	//      scanLeDevice(true);
	}
	
	
	 private void scanLeDevice(final boolean enable) {
	        if (enable) {
	            //停止后一个预定义的扫描周期扫描。
	            mHandler.postDelayed(new Runnable() {
	                @Override
	                public void run() {
	                    mScanning = false;
	                    bar.setVisibility(View.GONE);
	                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
	                    invalidateOptionsMenu();
	                }
	            }, 5000);
	        	bar.setVisibility(View.VISIBLE);
	            mScanning = true;
	            mBluetoothAdapter.startLeScan(mLeScanCallback);
                listItem.clear();
	        } else {
				bar.setVisibility(View.GONE);
	            mScanning = false;
	            mBluetoothAdapter.stopLeScan(mLeScanCallback);
	        }
	        
	        invalidateOptionsMenu();
	    }

	  // 扫描装置的回调。
	    private BluetoothAdapter.LeScanCallback mLeScanCallback =
	            new BluetoothAdapter.LeScanCallback() {

	        @Override
	        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
	            runOnUiThread(new Runnable() {
	                @Override
	                public void run() {
                        checkDevice(device, rssi, scanRecord);
	                }
	            });
	        }
	    };
		private ListView btlist;
		private ArrayList<HashMap<String, Object>> listItem;
		private SimpleAdapter adapter;
		private ProgressBar bar;

    private static String ByteArrayToString(byte[] ba)
    {
        StringBuilder hex = new StringBuilder(ba.length * 2);
        for (byte b : ba)
            hex.append(b + " ");

        return hex.toString();
    }

    public static class AdRecord {
        private int iLength;
        private int iType;
        private byte[] rawData;
        public AdRecord(int length, int type, byte[] data) {
            iLength = length;
            iType = type;
            rawData = data;

            Log.i(LOG_TAG, "Length: " + length + " Type : " + type + " Data : " + ByteArrayToString(data));
        }

        // ...

        public static List<AdRecord> parseScanRecord(byte[] scanRecord) {
            List<AdRecord> records = new ArrayList<AdRecord>();

            int index = 0;
            while (index < scanRecord.length) {
                int length = scanRecord[index++];
                //Done once we run out of records
                if (length == 0) break;

                int type = scanRecord[index];
                //Done if our record isn't a valid type
                if (type == 0) break;

                byte[] data = Arrays.copyOfRange(scanRecord, index+1, index+length);

                records.add(new AdRecord(length, type, data));
                //Advance
                index += length;
            }

            return records;
        }

        public int getUUID()
        {
            int ret = -1;
            if(iType == 2)
            {
                ret = ((rawData[0] & 0xFF) | (rawData[1] & 0xFF) << 8) & 0xFFFF;
            }
            return ret;
        }
    }
    private void checkDevice(final BluetoothDevice device, final int rssi, byte[] scanRecord)
    {
        Log.w(LOG_TAG, "check device:" + device.getName() + ", " + rssi);

        List<AdRecord> records = AdRecord.parseScanRecord(scanRecord);
        int uuid = -1;

		for(AdRecord rec:records)
		{
			uuid = rec.getUUID();

			if(uuid != -1)
			{
				break;
			}
		}

		String addr = device.getAddress();

		HashMap<String, Object> map = new HashMap<String, Object>();

		map.put("name", device.getName());
		map.put("address",addr);
		map.put("uuid", uuid);

		if(uuid != -1)
		{
			String addr_uuid = String.format(Locale.US, addr_uuid_tempplate, addr, uuid);
			map.put("addr_uuid", addr_uuid);
		}
		else
        {
			map.put("addr_uuid", addr);
		}

		listItem.add(map);
		adapter.notifyDataSetChanged();

    }
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// 将菜单；这将项目添加到动作条如果真的存在。
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
