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

package com.example.android.bluetoothlegatt;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends ListActivity {
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private boolean mConnectionInProgress;
    private Handler mHandler;
    private boolean mInTestMode=false;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    final String TAG = "BLE_TEST";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "DeviceScanActivity  onCreate");
        getActionBar().setTitle(R.string.title_devices);
        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        dispPairedDevice();
        registerReceiver(adbCmdReceiver, adbCmdReceiverIntentFilter());

        final IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(adbCmdReceiver, filter);

        final IntentFilter bondFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(adbCmdReceiver, bondFilter);

        mConnectionInProgress = false;
        mInTestMode=false;
    }

    public void dispPairedDevice() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        Log.d(TAG, "======== Paired Devices Start ===========");
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d(TAG, deviceHardwareAddress+ "  "+ deviceName);
                Log.d("LS_PAIR", "Paired Devices: "+ deviceHardwareAddress+ "  "+ deviceName);//Special tag used for adb based test
            }
        }
        Log.d(TAG, "======== Paired Devices End ===========");

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "DeviceScanActivity  onDestroy");
        unregisterReceiver(adbCmdReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_dev_test).setActionView(null);
         } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_dev_test).setActionView(
                    R.layout.actionbar_indeterminate_progress);
              }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;

            case R.id.menu_dev_test:
                mInTestMode = item.isChecked();
                if(mInTestMode) {
                    item.setChecked(false);
                    item.setTitle("Test_ON");
                    Log.d(TAG, " DevTest ON");
                } else{
                    item.setChecked(true);
                    item.setTitle("Test_OFF");
                    Log.d(TAG, " DevTest OFF");
                }
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);
        scanLeDevice(true);
        mConnectionInProgress = false;
        mInTestMode=false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;
        int devType = device.getType();
        Log.d(TAG, "========= BluetoothType: "+devType); //1-Classic, 2-LE, 3-Dual
/*
        Boolean isBonded = false;
        isBonded = device.createBond();
        if(isBonded) {
            Log.d(TAG, "========= createBond Retrun OK");
        } else {
            Log.d(TAG, "========= createBond Retrun FAILED");
        }
*/
       // pairDevice(device);

        final Intent newIntent;
        if(mInTestMode) {
            newIntent = new Intent(this, DeviceTestActivity.class);
        } else {
            newIntent = new Intent(this, DeviceControlActivity.class);
        }
        newIntent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        newIntent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        startActivity(newIntent);
     }
    private void pairDevice_1(BluetoothDevice device) {

        String ACTION_PAIRING_REQUEST = "android.bluetooth.device.action.PAIRING_REQUEST";
        Intent intent = new Intent(ACTION_PAIRING_REQUEST);
        String EXTRA_DEVICE = "android.bluetooth.device.extra.DEVICE";
        intent.putExtra(EXTRA_DEVICE, device);
        String EXTRA_PAIRING_VARIANT = "android.bluetooth.device.extra.PAIRING_VARIANT";
        int PAIRING_VARIANT_PIN = 0;
        intent.putExtra(EXTRA_PAIRING_VARIANT, PAIRING_VARIANT_PIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
    private void pairDevice(BluetoothDevice device) {
        //device.createBond();
        try {
            Log.d(TAG, "Start Pairing...");
            Method m = device.getClass()
                    .getMethod("createBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
            Log.d(TAG, "Pairing finished.");
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }
    private void unpairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass()
                    .getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void scanLeDevice(final boolean enable) {
        Log.d(TAG, "In scanLeDevice KSJ");
         if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                    displayScanResults();
                    Log.d("START_SCAN", "Scan Finished"); //Special tag used for adb based test
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
             Log.d("START_SCAN", "Scan Started"); //Special tag used for adb based test
          } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
             Log.d("STOP_SCAN", "Scan Stopped"); //Special tag used for adb based test
        }
        invalidateOptionsMenu();
    }

    private void displayScanResults() {
        Log.d("displayScanResults", "Joseph Kizhakkeparampil");
         int count = mLeDeviceListAdapter.getCount();
        for(int i=0; i<count; i++) {
             BluetoothDevice curDev = mLeDeviceListAdapter.getDevice(i);
            if(curDev.getName() != null) {
                Log.d(TAG, "Device-" + i + " " + curDev.getAddress() + " " + curDev.getName());
                //Special tag used for adb based test
                Log.d("LS_SCAN", "Device-" + i + " " + curDev.getAddress() + " " + curDev.getName());
            } else {
                Log.d(TAG, "Device-" + i + " " + curDev.getAddress() + " Unknown Device");
                //Special tag used for adb based test
                Log.d("LS_SCAN", "Device-" + i + " " + curDev.getAddress() + " Unknown Device");
            }
        }
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLeDeviceListAdapter.addDevice(device);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    private final BroadcastReceiver adbCmdReceiver = new BroadcastReceiver() {
         @Override
        public void onReceive(Context context, Intent intent) {
            //final String action = intent.getAction();
            Log.i(TAG, "KSJ In onReceive for Intent");
             String action = intent.getAction();
            if("com.example.android.bluetoothlegatt.TEST_ACTION".equals(action)){
                String rxText = intent.getStringExtra("com.example.android.bluetoothlegatt.EXTRA_TEXT");
                Log.i(TAG, "RxedCmdText: "+rxText);
                if("start_scan".equals(rxText)) {
                    scanLeDevice(true);
                } else if("stop_scan".equals(rxText)) {
                    scanLeDevice(false);
                } else if(!rxText.isEmpty()){
                    String delims = "[ ]+";
                    String[] tokens = rxText.split(delims);
                    //for (int i = 0; i < tokens.length; i++)
                    //    Log.i(TAG, tokens[i]);
                    if(tokens[0].equals("connect")) {
                        connectDevice(tokens);
                    } else if(tokens[0].equals("disconnect")) {
                        disconnectDevice(tokens);
                    } else if(tokens[0].equals("pair")) {
                        pairDeviceWithName(tokens, true);
                    }else if(tokens[0].equals("unpair")) {
                        pairDeviceWithName(tokens, false);
                    }else if(tokens[0].equals("lspair")) {
                        dispPairedDevice();
                    }else if(tokens[0].equals("lsscan")) {
                        displayScanResults();
                    }
                }
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                int previousState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);
                Log.d(TAG, "<<<ACTION_BOND_STATE_CHANGED: state:>>> " + state + ", previous:" + previousState);
                if(state == BluetoothDevice.BOND_BONDING) {
                    Log.i("START_PAIR", "Pairing BONDING"); //Special tag for adb based test
                } else if(state == BluetoothDevice.BOND_BONDED) {
                    Log.i("START_PAIR", "Pairing BONDED"); //Special tag for adb based test
                } else if(state == BluetoothDevice.BOND_NONE) {
                    Log.i("START_PAIR", "Pairing DROPPED"); //Special tag for adb based test
                }
                //listener.onDevicePairingEnded();
                // BOND_BONDED = 12
                // BOND_BONDING = 11
                // BOND_NONE = 10
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                Log.d(TAG, "<<<ACTION_ACL_CONNECTED:  >>> " );
                //Log.i("START_CONNECT", "Connected "); //Tag for adb based test
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                Log.d(TAG, "<<<ACTION_ACL_DISCONNECTED:  >>> " );
                //Log.i("START_CONNECT", "Disonnected "); //Tag for adb based test
            }else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                Log.d(TAG, "<<<ACTION_ACL_DISCONNECT_REQUESTED:  >>> " );
            }else if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                int pairingKey = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, -1);
                Log.d(TAG, "<<<ACTION_PAIRING_REQUEST:  >>> " + pairingKey); // 0-Pin, 2-PassKey
            }
        }
    };

    private void pairDeviceWithName(String[] tokens, boolean flagPairing) {
        String logMsg;
        if(flagPairing) {
            logMsg = "Pairing ";
        } else {
            logMsg = "Pairing ";
        }
        if(tokens.length < 3) {
            Log.i(TAG, logMsg + "Invalid param (pair A/N <address>/<name>)");
            Log.i("START_PAIR", "Pairing Invalid Param"); //Special tag for adb based test
            return;
        }
        String address;
        if("N".equals(tokens[1])) {
            address = getAddressFromName(tokens[2]);
            if(address.isEmpty()) {
                Log.i(TAG, logMsg + "ScanList missing: "+tokens[2]);
                Log.i("START_PAIR", "Pairing Invalid Param"); //Special tag for adb based test
                return;
            }
        } else if("A".equals(tokens[1])) {
            address = tokens[2];
        } else {
            Log.i(TAG, logMsg + "Invalid param (pair A/N <address>/<name>)");
            Log.i("START_PAIR", "Pairing Invalid Param"); //Special tag for adb based test
            return;
        }
        Log.i(TAG, logMsg + "DeviceWithAddress: "+ address);
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.i(TAG, logMsg + "Device Not Detected: "+tokens[1]);
            Log.i("START_PAIR", "Device Not Found"); //Special tag for adb based test
            return;
        }
        if(flagPairing) {
            pairDevice(device);
            Log.i("START_PAIR", "Pairing Started"); //Special tag for adb based test
        } else {
            unpairDevice(device);
        }
    }

    private void connectDevice(String[] tokens) {
        if(mConnectionInProgress) {
            Log.i(TAG, "Connected or Connection In Progress");
            Log.i("START_CONNECT", "Failed Already Connected"); //Tag for adb based test
            return;
        }
        if(tokens.length != 3) {
            Log.i(TAG, "\"Invalid param (pair A/N <address>/<name>)");
            Log.i("START_CONNECT", "Invalid Cmd Parameter"); //Tag for adb based test
            return;
        }
        String address;
        String devName = "Unkonwn";

        if("N".equals(tokens[1])) {
            address = getAddressFromName(tokens[2]);
            if(address.isEmpty()) {
                Log.i(TAG, "ScanList missing: "+tokens[2]);
                Log.i("START_CONNECT", "Invalid Cmd Missing Addr/Name"); //Tag for adb based test
                return;
            }
            devName = tokens[2];
         } else if("A".equals(tokens[1])) {
            address = tokens[2];
         } else {
            Log.i(TAG, "Invalid param (pair A/N <address>/<name>)");
            Log.i("START_CONNECT", "Invalid param (pair A/N <address>/<name>"); //Tag for adb based test
            return;
        }
        Log.i(TAG, "Connecting to: "+address + " (DevName: " + devName + ")");
        Log.i("START_CONNECT", "Start Connecting: "+address + " (DevName: " + devName + ")"); //Tag for adb based test

        final Intent intentController = new Intent(DeviceScanActivity.this, DeviceControlActivity.class);
        intentController.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, devName);
        intentController.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, address);
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        startActivity(intentController);
        mConnectionInProgress = true;
    }

    private void disconnectDevice(String[] tokens) {
        if(!mConnectionInProgress) {
            Log.i(TAG, "Not Connected or Not In Progress");
            return;
        }

    }

    private static IntentFilter adbCmdReceiverIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        //intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        //intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        //intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        //intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction("com.example.android.bluetoothlegatt.TEST_ACTION");
        return intentFilter;
    }

    private String getAddressFromName(String name) {
       if(name.isEmpty()){
            return "";
        }
        int count = mLeDeviceListAdapter.getCount();
        for(int i=0; i<count; i++) {
            if(name.equals(mLeDeviceListAdapter.getDevice(i).getName())) {
                return mLeDeviceListAdapter.getDevice(i).getAddress();
            }
        }
        return "";
     }

}
