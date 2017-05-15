package com.nordicsemi.nrfUARTv2;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import android.widget.Toast;


import com.nordicsemi.nrfUARTv2.drawutil.SketchPadView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.Date;

//import com.nordicsemi.nrfUARTv2.R;


import static com.nordicsemi.nrfUARTv2.R.id.btn_end;
import static com.nordicsemi.nrfUARTv2.R.id.btn_gettime;
import static com.nordicsemi.nrfUARTv2.R.id.btn_select;
import static com.nordicsemi.nrfUARTv2.R.id.btn_settime;
import static com.nordicsemi.nrfUARTv2.R.id.btn_start;


public class MyBtPen extends Activity implements View.OnClickListener{

    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int UART_PROFILE_READY = 10;
    public static final String TAG = "nRFUART";
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private static final int STATE_OFF = 10;

    public static final  int MSG_DRAW_POINT = 31;
    //public static final int PEN_DOWN = 160; //0xa0
    //public static final int PEN_UP = 224; //0xe0

    public static final int PEN_DOWN = 1;
    public static final int PEN_UP = 0;


    public static final int PEN_STATE_DOWN = 0;
    public static final int PEN_STATE_MOVE = 1;
    public static final int PEN_STATE_UP = 2;
    public static final int PEN_STATE_LEAVE = 3;

    public static int penState = PEN_STATE_UP;


    private int mState = UART_PROFILE_DISCONNECTED;
    private UartService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;
    private ListView messageListView;
    private ArrayAdapter<String> listAdapter;
    private Button btnConnectDisconnect;
    private Button btnGet, btnSet, btnStart, btnEnd;
    private EditText edtMessage;
    private static SketchPadView skpView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.newpen);

        initStrokeLib();

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null)
        {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btnConnectDisconnect = (Button) findViewById(btn_select);
        btnGet = (Button) findViewById(btn_gettime);
        btnSet = (Button) findViewById(btn_settime);
        btnStart = (Button) findViewById(btn_start);
        btnEnd = (Button) findViewById(btn_end);

        btnConnectDisconnect.setOnClickListener(this);
        btnGet.setOnClickListener(this);
        btnSet.setOnClickListener(this);
        btnStart.setOnClickListener(this);
        btnEnd.setOnClickListener(this);

        skpView = (SketchPadView) findViewById(R.id.skpview);

        service_init();



        // Example of a call to a native method
        //TextView tv = (TextView) findViewById(R.id.sample_text);
        //tv.setText(stringFromJNI());
    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case btn_select:
            {
                if (!mBtAdapter.isEnabled())
                {
                    Log.i(TAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                }
                else
                {
                    if (btnConnectDisconnect.getText().equals("Connect"))
                    {

                        //Connect button pressed, open DeviceListActivity class, with popup windows that scan for devices

                        Intent newIntent = new Intent(MyBtPen.this, DeviceListActivity.class);
                        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                    }
                    else
                    {
                        //Disconnect button pressed
                        if (mDevice != null)
                        {
                            mService.disconnect();

                        }
                    }
                }
            }
            break;

            case btn_gettime:

                break;

            case btn_settime:

                break;

            case btn_start:

                break;

            case btn_end:

                break;
        }
    }


    private ServiceConnection mServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder)
        {

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName)
        {

        }
    };

    private Handler myHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {

        }
    };

    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            final Intent mIntent = intent;
            //*********************//
            if (action.equals(UartService.ACTION_GATT_CONNECTED))
            {
                runOnUiThread(new Runnable()
                {
                    public void run()
                    {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        LogUtil.d("UART_CONNECT_MSG");
                        btnConnectDisconnect.setText("Disconnect");
                        edtMessage.setEnabled(true);
                        btnSet.setEnabled(true);
                        btnGet.setEnabled(true);
                        btnStart.setEnabled(true);
                        btnEnd.setEnabled(true);
                        //((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName()+ " - ready");
                        listAdapter.add("["+currentDateTimeString+"] Connected to: "+ mDevice.getName());
                        messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
                        mState = UART_PROFILE_CONNECTED;
                    }
                });
            }

            //*********************//
            if (action.equals(UartService.ACTION_GATT_DISCONNECTED))
            {
                runOnUiThread(new Runnable()
                {
                    public void run()
                    {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        LogUtil.d("UART_DISCONNECT_MSG");
                        btnConnectDisconnect.setText("Connect");
                        edtMessage.setEnabled(false);
                        btnSet.setEnabled(false);
                        btnGet.setEnabled(false);
                        btnStart.setEnabled(false);
                        btnEnd.setEnabled(false);
                        //((TextView) findViewById(R.id.deviceName)).setText("Not Connected");
                        listAdapter.add("["+currentDateTimeString+"] Disconnected to: "+ mDevice.getName());
                        mState = UART_PROFILE_DISCONNECTED;
                        mService.close();
                        //setUiState();

                    }
                });
            }


            //*********************//
            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED))
            {
                mService.enableTXNotification();
            }
            //*********************//
            if (action.equals(UartService.ACTION_DATA_AVAILABLE))
            {
                //LogUtil.i("ACTION_DATA_AVAILABLE");
                final byte[] txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA);

                if (null == txValue)
                {
                    LogUtil.i("not valid data!!!!");
                    return;
                }

                new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        //String data = ParserUtils.parse2(txValue);
                        //LogUtil.i("get data: " + data);

                        /*
                        int p[] = new int[20];
                        for (int j = 0; j < txValue.length; j++)
                        {
                            int v = txValue[j] & 0xFF;
                            LogUtil.i("data[" + j + "] =" + v);
                            //btPoints[j/10] = v;
                            p[j] = v;
                            //out[j * 2] = HEX_ARRAY[v >>> 4];
                            //out[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
                        }
                        */

                        //LogUtil.i("send msg!!!");
                        Message msg = Message.obtain();
                        msg.what = MyBtPen.MSG_DRAW_POINT;
                        Bundle bundle = new Bundle();
                        bundle.putByteArray("data", txValue);
                        msg.setData(bundle);
                        myHandler.sendMessage(msg);

                    }
                }).start();

                /*
                 runOnUiThread(new Runnable()
                 {
                     public void run() {
                         try {
                         	String text = new String(txValue, "UTF-8");
                         	String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        	 	listAdapter.add("["+currentDateTimeString+"] RX: "+text);
                        	 	messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);

                         } catch (Exception e) {
                             LogUtil.e(e.toString());
                         }
                     }
                 });
                   */
            }
            //*********************//
            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)){
                showMessage("Device doesn't support UART. Disconnecting");
                mService.disconnect();
            }
        }
    };

    private byte[] InputStreamToByte(InputStream is) throws IOException
    {

        ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
        int ch;
        while ((ch = is.read()) != -1)
        {
            bytestream.write(ch);
        }
        byte imgdata[] = bytestream.toByteArray();
        bytestream.close();
        return imgdata;
    }


    /**
     *  从assets目录中复制整个文件夹内容
     *  @param  context  Context 使用CopyFiles类的Activity
     *  @param  oldPath  String  原文件路径  如：/aa
     *  @param  newPath  String  复制后路径  如：xx:/bb/cc
     */
    public void copyDataFilesFromAssets(String oldPath, String newPath)
    {
        LogUtil.i("oldpath is " + oldPath);
        LogUtil.i("newPath is " + newPath);

        try {
            String fileNames[] = getAssets().list(oldPath);//获取assets目录下的所有文件及目录名
            if (fileNames.length > 0) {//如果是目录
                File file = new File(newPath);
                file.mkdirs();//如果文件夹不存在，则递归
                for (String fileName : fileNames) {
                    copyDataFilesFromAssets(oldPath + "/" + fileName, newPath+"/"+fileName);
                }
            } else {//如果是文件
                InputStream is = getAssets().open(oldPath);
                FileOutputStream fos = new FileOutputStream(new File(newPath));
                byte[] buffer = new byte[1024];
                int byteCount=0;
                while((byteCount=is.read(buffer))!=-1) {//循环从输入流读取 buffer字节
                    fos.write(buffer, 0, byteCount);//将读取的输入流写入到输出流
                }
                fos.flush();//刷新缓冲区
                is.close();
                fos.close();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private  void initStrokeLib()
    {
        //LogUtil.i("in initStrokeLib, befor getDicPath!!!!");
        //getDicPath();
        //LogUtil.i("after getDicPath!!!");
        //String dictFilePath = getFilesDir().getPath()+"/data";
        String dictFilePath = getExternalFilesDir(null).getPath() + "/data/";
        LogUtil.i("dataFilePaht is " + dictFilePath);
        //HW_Stroke_Init("/mnt/sdcard/HW_REC_CN.bin");
        File file = new File(dictFilePath);
        if (!file.exists())
        {
            file.mkdir();
            copyDataFilesFromAssets("HW_REC_CN.bin", String.format("%s/HW_REC_CN.bin", dictFilePath));
        }

        HW_Stroke_Init(dictFilePath + "HW_REC_CN.bin");
    }


    private void service_init()
    {
        Intent bindIntent = new Intent(this, UartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }

    private static IntentFilter makeGattUpdateIntentFilter()
    {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }

    @Override
    public void onStart()
    {
        super.onStart();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        LogUtil.d("onDestroy()");

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            LogUtil.e(ignore.toString());
        }
        unbindService(mServiceConnection);
        mService.stopSelf();
        mService= null;

    }

    @Override
    protected void onStop() {
        LogUtil.d("onStop");
        super.onStop();
    }

    @Override
    protected void onPause() {
        LogUtil.d("onPause");
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        LogUtil.d("onRestart");
    }

    @Override
    public void onResume() {
        super.onResume();
        LogUtil.d("onResume");
        if (!mBtAdapter.isEnabled()) {
            LogUtil.d("onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {

            case REQUEST_SELECT_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

                    LogUtil.d("... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
                    //((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName()+ " - connecting");
                    mService.connect(deviceAddress);


                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();

                } else {
                    // User did not enable Bluetooth or an error occurred
                    LogUtil.d("BT not enabled");
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                LogUtil.e("wrong request code");
                break;
        }
    }


    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onBackPressed()
    {
        if (mState == UART_PROFILE_CONNECTED) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
            showMessage("nRFUART's running in background.\n             Disconnect to exit");
        }
        else
        {
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

    public native int HW_Stroke_Init(String dicpath);
    public native int HW_Stroke_Rotate(short[] points);
    public native int HW_Stroke_Test();

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    //public native String stringFromJNI();

    // Used to load the 'native-lib' library on application startup.
    static {
        //System.loadLibrary("native-lib");
        System.loadLibrary("stroke");
    }
}
