/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nordicsemi.nrfUARTv2;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import com.nordicsemi.nrfUARTv2.UartService;
import com.nordicsemi.nrfUARTv2.drawutil.SketchPadView;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import static android.R.id.message;

public class MainActivity extends Activity implements RadioGroup.OnCheckedChangeListener {
    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int UART_PROFILE_READY = 10;
    public static final String TAG = "nRFUART";
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private static final int STATE_OFF = 10;

    public static final int MSG_DRAW_POINT = 31;
    public static final int MSG_RESPOND_GET_DATE = 32;
    public static final int MSG_RESPOND_SET_DATE = 33;
    public static final int MSG_RESPOND_START_GET_DATA = 34;
    public static final int MSG_RESPOND_END_GET_DATA = 35;

    TextView mRemoteRssiVal;
    RadioGroup mRg;
    private int mState = UART_PROFILE_DISCONNECTED;
    private UartService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;
    private ListView messageListView;
    private ArrayAdapter<String> listAdapter;
    private Button btnConnectDisconnect,btnSend;
    private EditText edtMessage;
    private static SketchPadView skpView;

    private Button btnSetTime, btnGetTime, btnStart, btnEnd, btnGetData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        messageListView = (ListView) findViewById(R.id.listMessage);
        listAdapter = new ArrayAdapter<String>(this, R.layout.message_detail);
        messageListView.setAdapter(listAdapter);
        messageListView.setDivider(null);
        btnConnectDisconnect=(Button) findViewById(R.id.btn_select);
        btnSend=(Button) findViewById(R.id.sendButton);
        edtMessage = (EditText) findViewById(R.id.sendText);

        btnGetTime = (Button) findViewById(R.id.btn_gettime);
        btnSetTime = (Button) findViewById(R.id.btn_settime);
        btnStart = (Button) findViewById(R.id.btn_begin);
        btnEnd = (Button) findViewById(R.id.btn_end);
        btnGetData = (Button) findViewById(R.id.btn_get_data);

        skpView = (SketchPadView) findViewById(R.id.skpview);

        service_init();


        btnGetTime.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                sendCommands(getTimeCmdArray());
            }
        });

        btnSetTime.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                sendCommands(setTimeCmdArray());
            }
        });

        btnStart.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                sendCommands(getTrackStartCmd());
            }
        });

        btnEnd.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                sendCommands(getTrackEndCmd());
            }
        });

        btnGetData.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                sendCommands(getTrackData());
            }
        });






        // Handler Disconnect & Connect button
        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBtAdapter.isEnabled()) {
                    Log.i(TAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                }
                else {
                	if (btnConnectDisconnect.getText().equals("Connect")){
                		
                		//Connect button pressed, open DeviceListActivity class, with popup windows that scan for devices
                		
            			Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
            			startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
        			} else {
        				//Disconnect button pressed
        				if (mDevice!=null)
        				{
        					mService.disconnect();
        					
        				}
        			}
                }
            }
        });
        // Handler Send button
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	EditText editText = (EditText) findViewById(R.id.sendText);
            	String message = editText.getText().toString();
            	byte[] value;

				try
                {
                    //send data to service
                    //value = message.getBytes("UTF-8");
                    //value = getCommandArray();
                    value = getTrackStartCmd();

                    for (int j = 0; j < value.length; j++)
                    {
                        LogUtil.i("write byte[" + j +"] " + value[j]);
                    }

                    mService.writeRXCharacteristic(value);
                    //Update the log with time stamp
                    String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                    listAdapter.add("[" + currentDateTimeString + "] TX: " + message);
                    messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
                    edtMessage.setText("");
                    //} catch (UnsupportedEncodingException e) {
                }catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                
            }
        });
     
        // Set initial UI state
        
    }


    private void sendCommands(byte[] value)
    {
        try
        {
            //send data to service
            //value = message.getBytes("UTF-8");
            //value = getCommandArray();
            //value = getTrackStartCmd();
            for (int j = 0; j < value.length; j++)
            {
                LogUtil.i("write byte[" + j +"] " + value[j]);
            }

            mService.writeRXCharacteristic(value);
            //Update the log with time stamp
            String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
            listAdapter.add("[" + currentDateTimeString + "] TX: " + message);
            messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
            edtMessage.setText("");
            //} catch (UnsupportedEncodingException e) {
        }catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private byte[] getTrackStartCmd()
    {
        //short c = 0xb1;
        int lrc;
        byte[] cmd = new byte[20];
        cmd[0] = 0x5;
        cmd[1] = (byte)0xb1;

        lrc = cmd[0] ^ cmd[1];
        cmd[19] = (byte)lrc;
        return cmd;
    }

    private byte[] getTrackData()
    {
        int lrc;
        byte[] cmd = new byte[20];
        cmd[0] = 0x5;
        cmd[1] = (byte)0xb2;

        lrc = cmd[0] ^ cmd[1];
        cmd[19] = (byte)lrc;
        return cmd;
    }

    private byte[] getTrackEndCmd()
    {
        int lrc;
        byte[] cmd = new byte[20];
        cmd[0] = 0x5;
        cmd[1] = (byte)0xb3;

        lrc = cmd[0] ^ cmd[1];
        cmd[19] = (byte)lrc;
        return cmd;
    }

    private byte[] getTimeCmdArray()
    {
        byte [] cmd = new byte[20];
        cmd[0] = 0x5;
        cmd[1] = (byte) 0xa2;
        int lrc = cmd[0] ^ cmd[1];
        cmd[19] = (byte)lrc;
        return cmd;
    }


    public static byte[] shortToByte(short number) {
        int temp = number;
        byte[] b = new byte[2];
        for (int i = 0; i < b.length; i++) {
            b[i] = new Integer(temp & 0xff).byteValue();// 将最低位保存在最低位
            temp = temp >> 8; // 向右移8位
        }
        return b;
    }

    private byte [] setTimeCmdArray()
    {
        byte [] cmd = new byte[20];
        cmd[0] = 0x5;
        cmd[1] = (byte) 0xa1;
        Calendar calendar = Calendar.getInstance();
        short year = (short)calendar.get(Calendar.YEAR);
        byte []tmp = shortToByte(year);
        LogUtil.i("set year to " + year);
        cmd[2] = tmp[0];
        cmd[3] = tmp[1];
        //cmd[2] = (byte)(year >> 0);
        //cmd[3] = (byte)(year >> 8);
        cmd[4] = (byte) calendar.get(Calendar.MONTH);
        cmd[5]= (byte)calendar.get(Calendar.DAY_OF_MONTH);
        cmd[6] = (byte)calendar.get(Calendar.HOUR_OF_DAY);
        cmd[7] = (byte)calendar.get(Calendar.MINUTE);
        cmd[8] = (byte)calendar.get(Calendar.SECOND);
        cmd[9] = 0x0;

        int lrc = cmd[0] ^ cmd[1] ^ cmd[2] ^ cmd[3] ^ cmd[4] ^ cmd[5] ^ cmd[6] ^ cmd[7] ^ cmd[8];
        cmd[19] = (byte)lrc;

        return cmd;
    }

    //UART service connected/disconnected
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
        		mService = ((UartService.LocalBinder) rawBinder).getService();
        		Log.d(TAG, "onServiceConnected mService= " + mService);
        		if (!mService.initialize()) {
                    Log.e(TAG, "Unable to initialize Bluetooth");
                    finish();
                }

        }

        public void onServiceDisconnected(ComponentName classname) {
       ////     mService.disconnect(mDevice);
        		mService = null;
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        
        //Handler events that received from UART service 
        public void handleMessage(Message msg) {
            switch(msg.what)
            {
                case MSG_RESPOND_GET_DATE:
                    LogUtil.i("msg respond for get date");
                    break;

                case MSG_RESPOND_SET_DATE:
                    LogUtil.i("msg respond for set date");
                    break;

                case MSG_RESPOND_START_GET_DATA:
                    LogUtil.i("msg respond for start get data");
                    break;

                case MSG_RESPOND_END_GET_DATA:
                    LogUtil.i("msg respond for end get data");
                    break;
            }
  
        }
    };

    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            final Intent mIntent = intent;
           //*********************//
            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
            	 runOnUiThread(new Runnable() {
                     public void run() {
                         	String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                             Log.d(TAG, "UART_CONNECT_MSG");
                             btnConnectDisconnect.setText("Disconnect");
                             edtMessage.setEnabled(true);

                             btnSend.setEnabled(true);
                             btnSetTime.setEnabled(true);
                             btnGetTime.setEnabled(true);
                             btnStart.setEnabled(true);
                             btnEnd.setEnabled(true);

                             ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName()+ " - ready");
                             listAdapter.add("["+currentDateTimeString+"] Connected to: "+ mDevice.getName());
                        	 	messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
                             mState = UART_PROFILE_CONNECTED;
                     }
            	 });
            }
           
          //*********************//
            if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
            	 runOnUiThread(new Runnable() {
                     public void run() {
                    	 	 String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                             Log.d(TAG, "UART_DISCONNECT_MSG");
                             btnConnectDisconnect.setText("Connect");
                             edtMessage.setEnabled(false);
                             btnSend.setEnabled(false);
                             btnGetTime.setEnabled(false);
                             btnSetTime.setEnabled(false);
                             btnEnd.setEnabled(false);
                             btnStart.setEnabled(false);
                             ((TextView) findViewById(R.id.deviceName)).setText("Not Connected");
                             listAdapter.add("["+currentDateTimeString+"] Disconnected to: "+ mDevice.getName());
                             mState = UART_PROFILE_DISCONNECTED;
                             mService.close();
                            //setUiState();
                         
                     }
                 });
            }
            
          
          //*********************//
            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
             	 mService.enableTXNotification();
            }
          //*********************//
            if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {
              
                 final byte[] txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA);
                    for (int i = 0; i < txValue.length; i++)
                    {
                        LogUtil.i("receive byte[" + i + "] -- " + txValue[i]);
                    }

                //add by fjm
                new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        LogUtil.i("start a new thread !!!!!");
                        processRecivedData(txValue);
                    }
                }).start();

                //add end

                 runOnUiThread(new Runnable() {
                     public void run() {
                         try {
                         	String text = new String(txValue, "UTF-8");
                         	String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        	 	listAdapter.add("["+currentDateTimeString+"] RX: "+text);
                        	 	messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
                        	
                         } catch (Exception e) {
                             Log.e(TAG, e.toString());
                         }
                     }
                 });



             }
           //*********************//
            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)){
            	showMessage("Device doesn't support UART. Disconnecting");
            	mService.disconnect();
            }
            
            
        }
    };

    private void processRecivedData(byte[] data)
    {
        LogUtil.i("data len is " + data.length);
        Message msg = Message.obtain();

        byte start = data[0];
        int id = data[1] > 0 ? data[1]: (data[1] + 256);
        LogUtil.i("start is " + start + " , id is " + Integer.toHexString(id));
        if (start == 0x2 && id == 0xa1)
        {
            LogUtil.i("get respond from server, respond for set time");
            msg.what = MainActivity.MSG_RESPOND_SET_DATE;
        }
        else if (start == 0x2 && id == 0xa2)
        {
            LogUtil.i("get respond from server, respond for get time");
            short year = (short) ((data[2] & 0xff) | ((data[3] & 0xff) << 8));
            LogUtil.i("get respond, Year is " + year);
            LogUtil.i("Month is " + data[4]);
            LogUtil.i("Day si " + data[5]);
            msg.what = MainActivity.MSG_RESPOND_GET_DATE;
        }
        else if (start == 0x2 && id == 0xb1)
        {
            LogUtil.i("get respond from server, respond for begin get data");
            msg.what = MainActivity.MSG_RESPOND_START_GET_DATA;
        }
        else if (start == 0x2 && id == 0xb3)
        {
            LogUtil.i("get respond from server, respond for end get data");
            msg.what = MainActivity.MSG_RESPOND_END_GET_DATA;
        }
        else if (start == 0x10)
        {
            LogUtil.i("get data, date section");
            return;
        }
        else if (start == 0x11)
        {
            LogUtil.i("get data, time section");
            return;
        }



        //msg.what = MainActivity.MSG_DRAW_POINT;
        Bundle bundle = new Bundle();
        bundle.putByteArray("data", data);
        msg.setData(bundle);
        mHandler.sendMessage(msg);

    }

    private void service_init() {
        Intent bindIntent = new Intent(this, UartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
  
        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }
    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
    	 super.onDestroy();
        Log.d(TAG, "onDestroy()");
        
        try {
        	LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        } 
        unbindService(mServiceConnection);
        mService.stopSelf();
        mService= null;
       
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!mBtAdapter.isEnabled()) {
            Log.i(TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
 
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

        case REQUEST_SELECT_DEVICE:
        	//When the DeviceListActivity return, with the selected device address
            if (resultCode == Activity.RESULT_OK && data != null) {
                String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
               
                Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
                ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName()+ " - connecting");
                mService.connect(deviceAddress);
                            

            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();

            } else {
                // User did not enable Bluetooth or an error occurred
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                finish();
            }
            break;
        default:
            Log.e(TAG, "wrong request code");
            break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
       
    }

    
    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
  
    }

    @Override
    public void onBackPressed() {
        if (mState == UART_PROFILE_CONNECTED) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
            showMessage("nRFUART's running in background.\n             Disconnect to exit");
        }
        else {
            new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(R.string.popup_title)
            .setMessage(R.string.popup_message)
            .setPositiveButton(R.string.popup_yes, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
   	                finish();
                }
            })
            .setNegativeButton(R.string.popup_no, null)
            .show();
        }
    }
}
