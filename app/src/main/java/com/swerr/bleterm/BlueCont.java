package com.swerr.bleterm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.UUID;


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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("NewApi")
public class BlueCont extends Activity {
    private static String LOG_TAG = "BlueCont";
    private final String PREF_SEND_NEW_LINE = "pref_send_new_line";
    private final String PREF_SEND_HEX = "pref_send_hex";

	private String mDeviceName;
	private String mDeviceAddress;
	private TextView tvstate;
	private EditText tvdata;
	private boolean result;
	private BluetoothLeService mBluetoothLeService;
	
    private BluetoothGattCharacteristic mNotifyCharacteristic;
	
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName,
				IBinder service) {
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service)
					.getService();
			Log.e(LOG_TAG, "初始化蓝牙服务");
			if (!mBluetoothLeService.initialize()) {
				Log.e(LOG_TAG, "无法初始化蓝牙");
				finish();
			}
			result = mBluetoothLeService.connect(mDeviceAddress);
			
			
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBluetoothLeService.disconnect();
			mBluetoothLeService = null;
		}
	};

	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.w(LOG_TAG, "ACTION_GATT_CONNECTED");
				result = true;
				tvstate.setText(R.string.connected);
                startRssiUpdate();
			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED
					.equals(action)) {
                Log.w(LOG_TAG, "ACTION_GATT_DISCONNECTED");
				result = false;
				mBluetoothLeService.close();
				tvstate.setText(R.string.disconnected);
				// clearUI();

			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED
					.equals(action)) {
				// 显示所有的支持服务的特点和用户界面。
				Log.e(LOG_TAG, "ACTION_GATT_SERVICES_DISCOVERED");
				displayGattServices(mBluetoothLeService.getSupportedGattServices());
				
			} else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                byte[] ba = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                Log.w(LOG_TAG, "ACTION_DATA_AVAILABLE: " + Arrays.toString(ba));
                String dataStr = new String(ba);
				DATA = DATA+ dataStr;
				tvdata.setText(DATA);
				tvdata.setSelection(DATA.length());
			}else if(BluetoothLeService.ACTION_RSSI.equals(action)){
				tvrssi.setText(intent.getStringExtra(BluetoothLeService.ACTION_DATA_RSSI));
			}
		}
	};
    private ExpandableListView mGattServicesList;
	private EditText et_send;
	private String DATA;
    protected String serv_uuid = "";
    protected String char_uuid = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.gatt_services_characteristics2);

		Intent intent = getIntent();
		mDeviceName = intent.getStringExtra("name");
		mDeviceAddress = intent.getStringExtra("address");
        serv_uuid = intent.getStringExtra("serv_uuid");
        char_uuid = intent.getStringExtra("char_uuid");

		getActionBar().setTitle(mDeviceName);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		DATA = "";

		tvstate = (TextView) findViewById(R.id.connection_state);
		tvdata = (EditText) findViewById(R.id.data_value);
		tvdata.setMovementMethod(ScrollingMovementMethod.getInstance());
		tvdata.requestFocus();//get the focus
		tvrssi = (TextView) findViewById(R.id.data_rssi);

		
		mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        mGattServicesList.setOnChildClickListener(servicesListClickListner);

        final CheckBox cbSendNl = (CheckBox) findViewById(R.id.cb_nl);
        final CheckBox cbSendHex = (CheckBox) findViewById(R.id.cb_hex);
        try
        {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            boolean sendNewLine = pref.getBoolean(PREF_SEND_NEW_LINE, false);
            cbSendNl.setChecked(sendNewLine);
            cbSendNl.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(BlueCont.this).edit();
                    editor.putBoolean(PREF_SEND_NEW_LINE, cbSendNl.isChecked());
                    editor.apply();
                }
            });
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }

        try
        {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            boolean sendHex = pref.getBoolean(PREF_SEND_HEX, false);
            cbSendHex.setChecked(sendHex);
            cbSendNl.setEnabled(!sendHex);

            cbSendHex.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(BlueCont.this).edit();
                    boolean sendHex = cbSendHex.isChecked();
                    editor.putBoolean(PREF_SEND_HEX, sendHex);
                    editor.apply();
                    cbSendNl.setEnabled(!sendHex);
                }
            });
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }

        et_send = (EditText) findViewById(R.id.et_send);
		Button btsend = (Button) findViewById(R.id.btsend);

        try
        {
            btsend.setOnClickListener(new OnClickListener()
            {

                @Override
                public void onClick(View arg0)
                {
                    // TODO Auto-generated method stub
                    String sendStr = et_send.getText().toString();

                    if(cbSendHex.isChecked())
                    {
                        if(!sendStr.trim().isEmpty())
                        {
                            String tokArray[] = sendStr.split("[^\\p{XDigit}]");
                            LinkedList<Integer> intList = new LinkedList<Integer>();
                            for(String tok : tokArray)
                            {
                                int tokLen = tok.length();
                                for(int i = 0; i < tokLen; i+=2){
                                    int intVal;
                                    if(i + 1 < tokLen)
                                    {
                                        intVal = (Character.digit(tok.charAt(i), 16) << 4) + Character.digit(tok.charAt(i+1), 16);
                                    }
                                    else
                                    {
                                        intVal = Character.digit(tok.charAt(i), 16);
                                    }
                                    intList.add(intVal);
                                }
                            }
                            byte[] sendBytes = new byte[intList.size()];
                            int bPos = 0;
                            for(Integer intVal : intList)
                            {
                                sendBytes[bPos] = (byte)(int)intVal;
                                bPos ++;
                            }
                            mBluetoothLeService.write(mNotifyCharacteristic, sendBytes);
                        }
                    }
                    else
                    {
                        if (cbSendNl.isChecked())
                        {
                            byte[] nl = {0x0d, 0x0a};
                            sendStr = sendStr.concat(new String(nl));
                        }
                        mBluetoothLeService.write(mNotifyCharacteristic, sendStr);
                    }
                }
            });
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }

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
			boolean connRes = mBluetoothLeService.connect(mDeviceAddress);
			Log.e(LOG_TAG, "connect result=" + connRes);

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
            case android.R.id.home:
                if (result) {
                    mBluetoothLeService.disconnect();
                }
                onBackPressed();
                break;
            case R.id.action_cont:
                if(!result)
                {
                    boolean connRes = mBluetoothLeService.connect(mDeviceAddress);
                }
                break;

            case R.id.action_close:
                if (result) {
                    mBluetoothLeService.disconnect();
                    tvstate.setText(R.string.disconnected);
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

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null)
            return;
        String uuid = null;
        BluetoothGattCharacteristic characteristic = null;
        // 循环遍历服务
        for (BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString().toLowerCase(Locale.US);
            Log.w(LOG_TAG, "service uuid : " + uuid);
            if(uuid.equals(serv_uuid))
            {
                characteristic = gattService.getCharacteristic(UUID.fromString(char_uuid));
            }
        }

        if(characteristic != null)
        {
            final int charaProp = characteristic.getProperties();
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0)
            {
                if (mNotifyCharacteristic != null)
                {
                    mBluetoothLeService.setCharacteristicNotification(
                            mNotifyCharacteristic, false);
                    mNotifyCharacteristic = null;
                }
                mBluetoothLeService.readCharacteristic(characteristic);

            }
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0)
            {
                mNotifyCharacteristic = characteristic;
                mBluetoothLeService.setCharacteristicNotification(
                        characteristic, true);
            }
        }
        else
        {
            Log.e(LOG_TAG, "unable to find char_uuid: " + char_uuid);
            if (result) {
                mBluetoothLeService.disconnect();
            }
            showToastText("unable to find char_uuid");

            onBackPressed();
        }
    }
    
    
    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
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

	private void startRssiUpdate()
	{
		flg = true;
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
						//e.printStackTrace();
					}
				}

			}
		}).start();
	}

    private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        tvdata.setText("木有数据");
    }

    private void showToastText(String str)
    {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
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
