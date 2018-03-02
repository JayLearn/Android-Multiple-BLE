package com.example.acer_user.android_multiple_ble;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private Button btn;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int REQUEST_CODE_LOCATION_SETTINGS = 2;
    private static final int PERMISSION_REQUEST_COARSE_BL = 2;
    private BluetoothAdapter mBTAdapter;

    //BLE set
    boolean btstate = false, btstate2 = false;
    private final static String TAG = MainActivity.class.getSimpleName();
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRAS_DEVICE_NAME2 = "DEVICE_NAME2";
    public static final String EXTRAS_DEVICE_ADDRESS2 = "DEVICE_ADDRESS2";
    private String mDeviceName, mDeviceName2;
    private String mDeviceAddress, mDeviceAddress2;
    //  private ExpandableListView mGattServicesList;
    private BLEService mBLEService;
    private BLEService2 mBLEService2;
    private BluetoothGattCharacteristic characteristicTX, characteristicTX2;
    private BluetoothGattCharacteristic characteristicRX, characteristicRX2;
    public final static UUID HM_RX_TX = UUID.fromString(BLEGattAttributes.HM_RX_TX);
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // 讓手機螢幕保持直立模式
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        checkLocBT();
        inicializeBluetooth();

        getBLEDevice();

        Intent gattServiceIntent = new Intent(this, BLEService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        Intent gattServiceIntent2 = new Intent(this, BLEService2.class);
        bindService(gattServiceIntent2, mServiceConnection2, BIND_AUTO_CREATE);

        btn = (Button) findViewById(R.id.button);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(btstate || btstate2){
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                    alertDialog.setMessage("Close BLE");
                    alertDialog.setPositiveButton("BLE1", null);
                    alertDialog.setNeutralButton("BLE2", null);
                    alertDialog.setNegativeButton("All", null);
                    final AlertDialog alert = alertDialog.create();
                    alert.show(); // 取代原本的 alertDialog.show(); 必須在 button 存取之前呼叫
                    Button button1 = alert.getButton(AlertDialog.BUTTON_POSITIVE);
                    Button button2 = alert.getButton(AlertDialog.BUTTON_NEUTRAL);
                    Button button3 = alert.getButton(AlertDialog.BUTTON_NEGATIVE);

                    button1.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mBLEService.disconnect();
                            alert.dismiss();
                        }
                    });
                    button2.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mBLEService2.disconnect();
                            alert.dismiss();
                        }
                    });
                    button3.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mBLEService.disconnect();
                            mBLEService2.disconnect();
                            alert.dismiss();
                        }
                    });
                }else{
                    Intent intent = new Intent(MainActivity.this, AllScanActivity.class);
                    startActivity(intent);
                }
            }
        });
    }

    private void getBLEDevice() {
        Intent it = getIntent();
        mDeviceName = it.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = it.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        mDeviceName2 = it.getStringExtra(EXTRAS_DEVICE_NAME2);
        mDeviceAddress2 = it.getStringExtra(EXTRAS_DEVICE_ADDRESS2);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BLEService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BLEService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BLEService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BLEService2.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BLEService2.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BLEService2.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BLEService2.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BLEService.ACTION_GATT_CONNECTED.equals(action)) {
                btstate = true;
                Toast.makeText(MainActivity.this, "Connected to: " + mDeviceName, Toast.LENGTH_SHORT).show();

            } else if (BLEService.ACTION_GATT_DISCONNECTED.equals(action)) {
                btstate = false;
                Toast.makeText(MainActivity.this, "Connection broken! Please reconnect", Toast.LENGTH_SHORT).show();
            } else if (BLEService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBLEService.getSupportedGattServices());
            } else if (BLEService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.d("TEST_DATA", intent.getStringExtra(mBLEService.EXTRA_DATA));
            }

            else if (BLEService2.ACTION_GATT_CONNECTED.equals(action)) {
                btstate2 = true;
                Toast.makeText(MainActivity.this, "Connected to2: " + mDeviceName2, Toast.LENGTH_SHORT).show();

            } else if (BLEService2.ACTION_GATT_DISCONNECTED.equals(action)) {
                btstate2 = false;
                Toast.makeText(MainActivity.this, "Connection broken! Please reconnect2", Toast.LENGTH_SHORT).show();
            } else if (BLEService2.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices2(mBLEService2.getSupportedGattServices());
            } else if (BLEService2.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.d("TEST_DATA", intent.getStringExtra(mBLEService2.EXTRA_DATA));
            }
        }
    };

    /**
        *  First BLE
        */
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBLEService = ((BLEService.LocalBinder) service).getService();
            if (!mBLEService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBLEService.connect(mDeviceAddress);
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBLEService = null;
        }
    };

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, BLEGattAttributes.lookup(uuid, unknownServiceString));

            // If the service exists for HM 10 Serial, say so.
            if(BLEGattAttributes.lookup(uuid, unknownServiceString) == "HM 10 Serial") {
                Log.d("TEST","Yes, serial :-)");
            } else {
                Log.d("TEST","No, serial :-(");
            }
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            // get characteristic when UUID matches RX/TX UUID
            characteristicTX = gattService.getCharacteristic(BLEService.UUID_HM_RX_TX);
            characteristicRX = gattService.getCharacteristic(BLEService.UUID_HM_RX_TX);
        }

        mBLEService.setCharacteristicNotification(characteristicRX,true);
    }

    private void sendData(String p){
        final byte[] bytedata = p.getBytes();
        characteristicTX.setValue(bytedata);
        mBLEService.writeCharacteristic(characteristicTX);
    }

    /**
     *  Second BLE
     */
    private final ServiceConnection mServiceConnection2 = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBLEService2 = ((BLEService2.LocalBinder) service).getService();
            if (!mBLEService2.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBLEService2.connect(mDeviceAddress2);
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBLEService2 = null;
        }
    };

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices2(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, BLEGattAttributes.lookup(uuid, unknownServiceString));

            // If the service exists for HM 10 Serial, say so.
            if(BLEGattAttributes.lookup(uuid, unknownServiceString) == "HM 10 Serial") {
                Log.d("TEST","Yes, serial :-)");
            } else {
                Log.d("TEST","No, serial :-(");
            }
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            // get characteristic when UUID matches RX/TX UUID
            characteristicTX2 = gattService.getCharacteristic(BLEService2.UUID_HM_RX_TX);
            characteristicRX2 = gattService.getCharacteristic(BLEService2.UUID_HM_RX_TX);
        }

        mBLEService2.setCharacteristicNotification(characteristicRX2,true);
    }

    private void sendData2(String p){
        final byte[] bytedata = p.getBytes();
        characteristicTX2.setValue(bytedata);
        mBLEService2.writeCharacteristic(characteristicTX2);
    }

    /**
        *       System
        */

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBLEService != null) {
            final boolean result = mBLEService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
        if (mBLEService2 != null) {
            final boolean result = mBLEService2.connect(mDeviceAddress2);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBLEService = null;
        unbindService(mServiceConnection2);
        mBLEService2 = null;
    }

    /**
        *       permissions
        */
    @TargetApi(23)
    private void checkLocBT(){
        //If Android version is M (6.0 API 23) or newer, check if it has Location permissions
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(this.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                //If Location permissions are not granted for the app, ask user for it! Request response will be received in the onRequestPermissionsResult.
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }
    }

    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
        //Check if permission request response is from Location
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //User granted permissions. Setup the scan settings
                    Log.d("TAG", "coarse location permission granted");
                } else {
                    //User denied Location permissions. Here you could warn the user that without
                    //Location permissions the app is not able to scan for BLE devices and eventually
                    //Close the app
                    finish();
                }
                return;
            }
        }
    }

    private void inicializeBluetooth(){

        //Check if device does support BT by hardware
        if (!getBaseContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            //Toast shows a message on the screen for a LENGTH_SHORT period
            Toast.makeText(this, "BLUETOOTH NOT SUPPORTED!", Toast.LENGTH_SHORT).show();
            finish();
        }
        //Check if device does support BT Low Energy by hardware. Else close the app(finish())!
        if (!getBaseContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            //Toast shows a message on the screen for a LENGTH_SHORT period
            Toast.makeText(this, "BLE NOT SUPPORTED!", Toast.LENGTH_SHORT).show();
            finish();
        }else {
            //If BLE is supported, get the BT adapter. Preparing for use!
            mBTAdapter = BluetoothAdapter.getDefaultAdapter();
            //If getting the adapter returns error, close the app with error message!
            if (mBTAdapter == null) {
                Toast.makeText(this, "ERROR GETTING BLUETOOTH ADAPTER!", Toast.LENGTH_SHORT).show();
                finish();
            }else{
                //Check if BT is enabled! This method requires BT permissions in the manifest.
                if (!mBTAdapter.isEnabled()) {
                    //If it is not enabled, ask user to enable it with default BT enable dialog! BT enable response will be received in the onActivityResult method.
                    Intent enableBTintent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBTintent, PERMISSION_REQUEST_COARSE_BL);
                }
            }
        }

    }
}
