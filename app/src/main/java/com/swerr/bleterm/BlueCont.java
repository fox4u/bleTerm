package com.swerr.bleterm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;






import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

@SuppressLint("NewApi")
public class BlueCont extends Activity {

	private String mDeviceName;
	private String mDeviceAddress;
	private TextView tvaddress;
	private TextView tvstate;
	private EditText tvdata;
	private ExpandableListView elist;
	private boolean result;
	private BluetoothLeService mBluetoothLeService;
	
    private BluetoothGattCharacteristic mNotifyCharacteristic;
	
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
	// 代码管理服务生命周期。
	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName,
				IBinder service) {
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service)
					.getService();
			Log.e("a", "初始化蓝牙服务");
			if (!mBluetoothLeService.initialize()) {
				Log.e("a", "无法初始化蓝牙");
				finish();
			}
			// 自动连接到装置上成功启动初始化。
			result = mBluetoothLeService.connect(mDeviceAddress);
			
			
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBluetoothLeService.disconnect();
			mBluetoothLeService = null;
		}
	};

	// 处理各种事件的服务了。
	// action_gatt_connected连接到服务器：关贸总协定。
	// action_gatt_disconnected：从关贸总协定的服务器断开。
	// action_gatt_services_discovered：关贸总协定的服务发现。
	// action_data_available：从设备接收数据。这可能是由于阅读
	// 或通知操作。
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
				result = true;
				Log.e("a", "来了广播1");
				tvstate.setText("连接");

			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED
					.equals(action)) {
				result = false;
				Log.e("a", "来了广播2");
				mBluetoothLeService.close();
				tvstate.setText("未连接");
				// clearUI();

			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED
					.equals(action)) {
				// 显示所有的支持服务的特点和用户界面。
				Log.e("a", "来了广播3");
				List<BluetoothGattService> supportedGattServices = mBluetoothLeService
						.getSupportedGattServices();
				 displayGattServices(mBluetoothLeService.getSupportedGattServices());
				for(int i=0;i<supportedGattServices.size();i++){
					Log.e("a","1:BluetoothGattService UUID=:"+supportedGattServices.get(i).getUuid());
					List<BluetoothGattCharacteristic> cs = supportedGattServices.get(i).getCharacteristics();
					for(int j=0;j<cs.size();j++){
						Log.e("a","2:   BluetoothGattCharacteristic UUID=:"+cs.get(j).getUuid());
						
					
							List<BluetoothGattDescriptor> ds = cs.get(j).getDescriptors();
							for(int f=0;f<ds.size();f++){
								Log.e("a","3:      BluetoothGattDescriptor UUID=:"+ds.get(f).getUuid());
								
								 byte[] value = ds.get(f).getValue();
								
								 Log.e("a","4:     			value=:"+Arrays.toString(value));
								 Log.e("a","5:     			value=:"+Arrays.toString( ds.get(f).getCharacteristic().getValue()));
							}
					}
				}
				
			} else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
				Log.e("a", "来了广播4--->data:"+intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
//				displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
				i++;
				DATA = ""+DATA+"\n第"+i+"条："+intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
				Log.e("a4", ""+DATA);
				tvdata.setText(""+DATA);
				tvdata.setSelection(DATA.length());
			}else if(BluetoothLeService.ACTION_RSSI.equals(action)){
				Log.e("a", "来了广播5");
				tvrssi.setText("RSSI:"+intent
						.getStringExtra(BluetoothLeService.ACTION_DATA_RSSI));
			}
		}
	};
    private ExpandableListView mGattServicesList;
	private EditText et_send;
	private String DATA;
	private int i;
	private ArrayList<BluetoothGattCharacteristic> charas;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.gatt_services_characteristics2);

		Intent intent = getIntent();
		mDeviceName = intent.getStringExtra("name");
		mDeviceAddress = intent.getStringExtra("andrass");

		Log.e("a", "名字"+mDeviceName+"地址"+mDeviceAddress);
		getActionBar().setTitle(mDeviceName);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		DATA = "";
		i = 0;
		
		tvaddress = (TextView) findViewById(R.id.device_address);
		tvaddress.setText(mDeviceAddress);

		tvstate = (TextView) findViewById(R.id.connection_state);
		tvdata = (EditText) findViewById(R.id.data_value);
		tvdata.setMovementMethod(ScrollingMovementMethod.getInstance()); 
		//tvdata.setSelected(true);
		tvdata.requestFocus();//get the focus
		tvrssi = (TextView) findViewById(R.id.data_rssi);

		
		mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        mGattServicesList.setOnChildClickListener(servicesListClickListner);
		
        et_send = (EditText) findViewById(R.id.et_send);
		Button btsend = (Button) findViewById(R.id.btsend);
		
		btsend.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				byte[]bb= new byte[]{(byte)1,2,3};
				String sendstr = et_send.getText().toString();
				Boolean boolean1 = mBluetoothLeService.write(mNotifyCharacteristic,sendstr);
				Log.e("a", "发送UUID"+mNotifyCharacteristic.getUuid().toString()+"是否发送成功::"+boolean1);
			}
		});
		
		 flg = true;
		Button btrssi=(Button) findViewById(R.id.btrssi);
		btrssi.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				
				new Thread(new Runnable() {
					
					@Override
					public void run() {
						while (flg) {
						// TODO Auto-generated method stub
							try {
								Thread.sleep(1000);	
								flg=mBluetoothLeService.readrssi();	
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								Log.e("a","断网了");
							}
						}
					
					}
				}).start();
			}
		});
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
		Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
		
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		/**
		 * 注册广播
		 */
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
		if (mBluetoothLeService != null) {
			Log.e("a", "来了");
			result = mBluetoothLeService.connect(mDeviceAddress);
			Log.e("a", "连接请求的结果=" + result);

		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch (item.getItemId()) {
		case R.id.action_settings:

			if (result) {
				result = false;
				mBluetoothLeService.disconnect();
			}
			onBackPressed();
			break;
		case R.id.action_cont:
			result = mBluetoothLeService.connect(mDeviceAddress);

			break;

		case R.id.action_close:
			if (result) {
				result = false;
			//	mBluetoothLeService.disconnect();
				Log.e("a", "断开了");
				mBluetoothLeService.close();
				tvstate.setText("连接断开");
			}

			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * 销毁广播接收器
	 */
	@Override
	protected void onPause() {
		super.onPause();
		flg=false;
		unregisterReceiver(mGattUpdateReceiver);
	
	}
	/**
	 * 结束服务
	 */
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();

		unbindService(mServiceConnection);

	}
	
    // 我们是注定的expandablelistview数据结构
	//  在UI。
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null)
        	return;
        String uuid = null;
        String unknownServiceString = "service_UUID";
        String unknownCharaString = "characteristic_UUID";
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // 循环遍历服务
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    "NAME", SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put("UUID", uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // 循环遍历特征
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        "NAME", SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put("UUID", uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }
        
        final BluetoothGattCharacteristic characteristic = charas.get(charas.size()-1);
        final int charaProp = characteristic.getProperties();
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            if (mNotifyCharacteristic != null) {
                mBluetoothLeService.setCharacteristicNotification(
                        mNotifyCharacteristic, false);
                mNotifyCharacteristic = null;
            }
            mBluetoothLeService.readCharacteristic(characteristic);

        }
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            mNotifyCharacteristic = characteristic;
            mBluetoothLeService.setCharacteristicNotification(
                    characteristic, true);
        }

       /* SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {"NAME", "UUID"},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {"NAME", "UUID"},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        mGattServicesList.setAdapter(gattServiceAdapter);*/
    }
    
    
    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                	Log.e("a","点击了");
                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic = mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            if (mNotifyCharacteristic != null) {
                                mBluetoothLeService.setCharacteristicNotification(
                                        mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            mBluetoothLeService.readCharacteristic(characteristic);

                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = characteristic;
                            mBluetoothLeService.setCharacteristicNotification(
                                    characteristic, true);
                        }
                        return true;
                    }
                    return false;
                }
                
    };
	private boolean flg;
	private TextView tvrssi;

    private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        tvdata.setText("木有数据");
    }

	/**
	 * 注册广播
	 * @return
	 */
    
    
	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter
				.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
		intentFilter.addAction(BluetoothLeService.ACTION_RSSI);
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_RSSI);
		return intentFilter;
	}
	
}
