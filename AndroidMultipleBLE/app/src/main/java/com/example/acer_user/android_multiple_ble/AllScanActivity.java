package com.example.acer_user.android_multiple_ble;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.ListView;

import com.example.acer_user.android_multiple_ble.models.MyListViewAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class AllScanActivity extends AppCompatActivity {

    //DEFINE VARS
    String TAG = "BLEScanActivity";

    BluetoothAdapter mBluetoothAdapter;
    BluetoothGatt mBluetoothGatt;
    BluetoothLeScanner scanner;
    ScanSettings scanSettings;


    private List<String> scannedDeivcesList;
//    private ArrayAdapter<String> adapter;
    private boolean mScanning = false;
    MyListViewAdapter adapter;

    //DEFINE LAYOUT
    ListView devicesList;
    private Vector<String> vName = new Vector<>();
    private Vector<String> vAddress = new Vector<>();

    //THIS METHOD RUNS ON APP LAUNCH
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //Inicialize de devices list
        scannedDeivcesList = new ArrayList<>();
        //Inicialize the list adapter for the listview with params: Context / Layout file / TextView ID in layout file / Devices list

        //Define listview in layout
        devicesList = (ListView) findViewById(R.id.devicesList);
//        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1, scannedDeivcesList);
        adapter = new  MyListViewAdapter(this, R.layout.listview_item, scannedDeivcesList);
        //Set the adapter to the listview
        devicesList.setAdapter(adapter);
        devicesList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        //init Bluetooth adapter
        initBT();
        //Start scan of bluetooth devices
        startLeScan(true);

        //Setup list on device click listener
        setupListClickListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        startLeScan(false);
    }

    private void initBT(){
        final BluetoothManager bluetoothManager =  (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        //Create the scan settings
        ScanSettings.Builder scanSettingsBuilder = new ScanSettings.Builder();
        //Set scan latency mode. Lower latency, faster device detection/more battery and resources consumption
        scanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        //Wrap settings together and save on a settings var (declared globally).
        scanSettings = scanSettingsBuilder.build();
        //Get the BLE scanner from the BT adapter (var declared globally)
        scanner = mBluetoothAdapter.getBluetoothLeScanner();
    }

    private void startLeScan(boolean endis) {
        if (endis) {
            //********************
            //START THE BLE SCAN
            //********************
            //Scanning parameters FILTER / SETTINGS / RESULT CALLBACK. Filter are used to define a particular
            //device to scan for. The Callback is defined above as a method.
            mScanning = true;
            scanner.startScan(null, scanSettings, mScanCallback);
        }else{
            //Stop scan
            mScanning = false;
            scanner.stopScan(mScanCallback);
        }
    }


    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            //Here will be received all the detected BLE devices around. "result" contains the device
            //address and name as a BLEPeripheral, the advertising content as a ScanRecord, the Rx RSSI
            //and the timestamp when received. Type result.get... to see all the available methods you can call.

            //Convert advertising bytes to string for a easier parsing. GetBytes may return a NullPointerException. Treat it right(try/catch).
            String advertisingString = byteArrayToHex(result.getScanRecord().getBytes());
            //Print the advertising String in the LOG with other device info (ADDRESS - RSSI - ADVERTISING - NAME)
            Log.i(TAG, result.getDevice().getAddress()+" - RSSI: "+result.getRssi()+"\t - "+advertisingString+" - "+result.getDevice().getName());

            //Check if scanned device is already in the list by mac address
            boolean contains = false;
            for(int i=0; i<scannedDeivcesList.size(); i++){
                if(scannedDeivcesList.get(i).contains(result.getDevice().getAddress())){
                    //Device already added
                    contains = true;
                    //Replace the device with updated values in that position
                    scannedDeivcesList.set(i, result.getRssi()+"  "+result.getDevice().getName()+ "\n       ("+result.getDevice().getAddress()+")");
                    break;
                }
            }

            if(!contains){
                //Scanned device not found in the list. NEW => add to list
                scannedDeivcesList.add(result.getRssi()+"  "+result.getDevice().getName()+ "\n       ("+result.getDevice().getAddress()+")");
            }

            //After modify the list, notify the adapter that changes have been made so it updates the UI.
            //UI changes must be done in the main thread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyDataSetChanged();
                }
            });

        }
    };

    //Method to convert a byte array to a HEX. string.
    private String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }

    void setupListClickListener(){

        devicesList.setMultiChoiceModeListener(new  AbsListView.MultiChoiceModeListener() {
            @Override
            public void  onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                // TODO  Auto-generated method stub
                final int checkedCount  = devicesList.getCheckedItemCount();
                // Set the  CAB title according to total checked items
                mode.setTitle(checkedCount  + "  Selected");
                // Calls  toggleSelection method from ListViewAdapter Class
                adapter.toggleSelection(position);

                String fullString = scannedDeivcesList.get(position);
                String rssi = fullString.substring(0, fullString.indexOf("  "));
                String name = fullString.substring(fullString.indexOf("  ")+1, fullString.indexOf("\n "));
                String address = fullString.substring(fullString.indexOf("(")+1, fullString.indexOf(")"));
                if(checked){
                    //Get the string from the item clicked
                    vName.add(name);
                    vAddress.add(address);
                    Log.d("TEST",fullString);
                }else{
                    int i = vName.indexOf(name);
                    vName.remove(i);
                    vAddress.remove(i);
                    Log.d("TEST2",fullString);
                }
            }

            @Override
            public boolean  onActionItemClicked(final ActionMode mode, MenuItem item) {
                // TODO  Auto-generated method stub
                switch  (item.getItemId()) {
                    case R.id.selectOK:
                        if(vName.size() < 3){
                            if (mScanning) {
                                startLeScan(false);
                            }

                            if(vName.size() == 1){
                                Intent intent = new Intent(AllScanActivity.this, MainActivity.class);
                                intent.putExtra(MainActivity.EXTRAS_DEVICE_NAME, vName.get(0));
                                intent.putExtra(MainActivity.EXTRAS_DEVICE_ADDRESS, vAddress.get(0));
                                startActivity(intent);
                                finish();
                            }
                            else if(vName.size() == 2){
                                Intent intent = new Intent(AllScanActivity.this, MainActivity.class);
                                intent.putExtra(MainActivity.EXTRAS_DEVICE_NAME, vName.get(0));
                                intent.putExtra(MainActivity.EXTRAS_DEVICE_ADDRESS, vAddress.get(0));
                                intent.putExtra(MainActivity.EXTRAS_DEVICE_NAME2, vName.get(1));
                                intent.putExtra(MainActivity.EXTRAS_DEVICE_ADDRESS2, vAddress.get(1));

                                startActivity(intent);
                                finish();
                            }

                        }
                        return true;

                    default:
                        return false;

                }
            }

            @Override
            public boolean  onPrepareActionMode(ActionMode mode, Menu menu) {
                // TODO  Auto-generated method stub
                return false;
            }

            @Override
            public void  onDestroyActionMode(ActionMode mode) {
                // TODO  Auto-generated method stub
            }

            @Override
            public boolean  onCreateActionMode(ActionMode mode, Menu menu) {
                // TODO  Auto-generated method stub
                mode.getMenuInflater().inflate(R.menu.scanble_menu, menu);
                return true;
            }
        });
    }

    //Connection callback
    BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        //Device connected, start discovering services
                        Log.i(TAG, "DEVICE CONNECTED. DISCOVERING SERVICES...");
                        mBluetoothGatt.discoverServices();
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        //Device disconnected
                        Log.i(TAG, "DEVICE DISCONNECTED");
                    }
                }

                // On discover services method
                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        //Services discovered successfully. Start parsing services and characteristics
                        Log.i(TAG, "SERVICES DISCOVERED. PARSING...");
                        displayGattServices(gatt.getServices());
                    } else {
                        //Failed to discover services
                        Log.i(TAG, "FAILED TO DISCOVER SERVICES");
                    }
                }

                //When reading a characteristic, here you receive the task result and the value
                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        //READ WAS SUCCESSFUL
                        Log.i(TAG, "ON CHARACTERISTIC READ SUCCESSFUL");
                        //Read characteristic value like:
                        //characteristic.getValue();
                        //Which it returns a byte array. Convert it to HEX. string.
                    } else {
                        Log.i(TAG, "ERROR READING CHARACTERISTIC");
                    }
                }

                //When writing, here you can check whether the task was completed successfully or not
                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicWrite(gatt, characteristic, status);
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.i(TAG, "ON CHARACTERISTIC WRITE SUCCESSFUL");
                    } else {
                        Log.i(TAG, "ERROR WRITING CHARACTERISTIC");
                    }
                }

                //In this method you can read the new values from a received notification
                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    Log.i(TAG, "NEW NOTIFICATION RECEIVED");
                    //New notification received. Check the characteristic it comes from and parse to string
                    /*if(characteristic.getUuid().toString().contains("0000fff3")){
                        characteristic.getValue();
                    }*/
                }

                //RSSI values from the connection with the remote device are received here
                @Override
                public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                    Log.i(TAG, "NEW RSSI VALUE RECEIVED");
                    //Read remote RSSI like: mBluetoothGatt.readRemoteRssi();
                    //Here you get the gatt table where the rssi comes from, the rssi value and the
                    //status of the task.
                }
            };

    //Method which parses all services and characteristics from the GATT table.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        //Check if there is any gatt services. If not, return.
        if (gattServices == null) return;

        // Loop through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            Log.i(TAG, "SERVICE FOUND: "+gattService.getUuid().toString());
            //Loop through available characteristics for each service
            for (BluetoothGattCharacteristic gattCharacteristic : gattService.getCharacteristics()) {
                Log.i(TAG, "  CHAR. FOUND: "+gattCharacteristic.getUuid().toString());
            }
        }
    }
}


