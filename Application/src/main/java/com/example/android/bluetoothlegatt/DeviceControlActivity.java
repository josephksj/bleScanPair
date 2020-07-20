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
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    //private final static String TAG = DeviceControlActivity.class.getSimpleName();
    final String TAG = "BLE_TEST";

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mConnectionState;
    private TextView mIndDataField;
    private TextView mReadDataField;
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private boolean mPaired = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    private int notificationCount = 0;
    private boolean mNotificationFlag = false;

    private TextView mIndCountTxtView;
    private BluetoothGattCharacteristic mListSelGattCharct = null;
    private TextView mSelCharUuidTextView;
    private Button mStartReadButton;
    private Button mStartWriteButton;
    private EditText mWriteDataEditText;
    private ToggleButton mIndicationToggleButton;       //indication_sel_cb
    private ToggleButton mRdDataDispHexChar;       //read_hex_char_tb
    boolean mRdDatahexDispFlag = true;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };


    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_IND_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_ERROR.equals(action)) {
                mConnected = false;
                onBackPressed();    //Go back to the scanning mode
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_IND_DATA_AVAILABLE.equals(action)) {
                indDisplayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            } else if (BluetoothLeService.ACTION_RD_DATA_AVAILABLE.equals(action)) {
                readDisplayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            } else if ("com.example.android.bluetoothlegatt.TEST_ACTION".equals(action)) {
                Log.i(TAG, "Control Activity.TEST_ACTION");
                String rxText = intent.getStringExtra("com.example.android.bluetoothlegatt.EXTRA_TEXT");
                if(!rxText.isEmpty()) {
                    String delims = "[ ]+";
                    String[] tokens = rxText.split(delims);
                    if (tokens.length <= 0) {
                        return;
                    }
                    if (tokens[0].equals("disconnect")) {
                        // mBluetoothLeService.connect(mDeviceAddress);
                        mBluetoothLeService.disconnect();
                        onBackPressed(); //Issue connect from ScanActivity
                    } else if (tokens[0].equals("indication")) {
                        processIndicationCmd(tokens);
                    }
                }
            }else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                int previousState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);
                Log.d(TAG, "=== CTRLR ACTION_BOND_STATE_CHANGED: state:>>> " + state + ", previous:" + previousState);
                //listener.onDevicePairingEnded();
                // BOND_BONDED = 12
                // BOND_BONDING = 11
                // BOND_NONE = 10
                //if(state == BOND_BONDED) mPaired = true; else mPaired = false;
                invalidateOptionsMenu();
            }
        }
        public void processIndicationCmd(String[] tokens) {
            Log.i(TAG, "Control Activity.Indication");
            for (ArrayList<BluetoothGattCharacteristic> gatCharParent : mGattCharacteristics) {
                Log.i(TAG, "============================");
                for (BluetoothGattCharacteristic gatCharChild : gatCharParent) {
                    String uuid = gatCharChild.getUuid().toString();
                    Log.i(TAG, "-------"+uuid);
                    if((tokens.length == 2) && (tokens[1].equals(uuid))){
                        Log.i(TAG, "Found Characterists: "+uuid);
                        final int charaProp = gatCharChild.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = gatCharChild;
                            mBluetoothLeService.setCharacteristicNotification(gatCharChild, true);
                        }
                        return;
                    }
                }
            }
        }
    };

    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {

                    if (mGattCharacteristics == null)
                        return false;
                    disableWindowSelection();
                    mListSelGattCharct = mGattCharacteristics.get(groupPosition).get(childPosition);
                    final int charaProp = mListSelGattCharct.getProperties();
                    final int maskFlag = BluetoothGattCharacteristic.PROPERTY_READ |
                            BluetoothGattCharacteristic.PROPERTY_WRITE |
                            BluetoothGattCharacteristic.PROPERTY_NOTIFY;
                    if((charaProp&maskFlag) == 0)
                        return true;
                     mSelCharUuidTextView.setText(mListSelGattCharct.getUuid().toString());
                    if((charaProp&BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0) {
                        mIndicationToggleButton.setEnabled(false);
                        mIndicationToggleButton.setBackgroundColor(Color.GRAY);
                        //mIndicationCheckBox.setEnabled(false);
                        //mIndicationCheckBox.setBackgroundColor(Color.GRAY);
                    } else {
                        mIndicationToggleButton.setEnabled(true);
                        mIndicationToggleButton.setBackgroundColor(Color.WHITE);
                        //mIndicationCheckBox.setEnabled(true);
                        //mIndicationCheckBox.setBackgroundColor(Color.WHITE);
                    }
                    if((charaProp&BluetoothGattCharacteristic.PROPERTY_READ) == 0) {
                        mStartReadButton.setText("No Read");
                        mStartReadButton.setBackgroundColor(Color.GRAY);
                    } else {
                        mStartReadButton.setText("Start Read");
                        mStartReadButton.setBackgroundColor(Color.WHITE);
                    }
                    if((charaProp&BluetoothGattCharacteristic.PROPERTY_WRITE) == 0) {
                        mStartWriteButton.setText("No Write");
                        mWriteDataEditText.setBackgroundColor(Color.GRAY);
                        mWriteDataEditText.setFocusable(false); // disable editing
                    } else {
                        mStartWriteButton.setText("Data Write");
                        mWriteDataEditText.setBackgroundColor(Color.WHITE);
                        mWriteDataEditText.setFocusableInTouchMode(true);
                    }
                    return false;
                }
    };

    private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mIndDataField.setText(R.string.no_data);
        mReadDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "ControlActivity OnCreate");
        setContentView(R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);



        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        mGattServicesList.setOnChildClickListener(servicesListClickListner);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mIndDataField = (TextView) findViewById(R.id.indication_value);
        mIndCountTxtView = (TextView) findViewById(R.id.indication_count);
        mReadDataField = (TextView) findViewById(R.id.read_value);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mIndCountTxtView = (TextView) findViewById(R.id.indication_count);
        mIndicationToggleButton = (ToggleButton) findViewById(R.id.indication_sel_tb);
        mSelCharUuidTextView = (TextView) findViewById(R.id.charact_uuid);
        mStartReadButton = (Button) findViewById(R.id.read_start_button);
        mStartWriteButton = (Button) findViewById(R.id.write_start_button);
        mWriteDataEditText = (EditText) findViewById(R.id.write_value);
        mRdDataDispHexChar = (ToggleButton) findViewById(R.id.read_hex_char_tb); //read_hex_char_tb
        disableWindowSelection();

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    void disableWindowSelection() {
        if(mNotifyCharacteristic != null) {
            mBluetoothLeService.setCharacteristicNotification(mListSelGattCharct, false);
            mNotifyCharacteristic = null;
        }
        mIndCountTxtView.setText("0");
        mIndicationToggleButton.setEnabled(false);
        mIndicationToggleButton.setBackgroundColor(Color.GRAY);
        mSelCharUuidTextView.setText("No UUID Selected");
        mIndDataField.setText("No Data");
        mReadDataField.setText("No Data");
        mStartReadButton.setText("No Read");
        mStartWriteButton.setText("No Write");
        mWriteDataEditText.setBackgroundColor(Color.GRAY);
        mWriteDataEditText.setText("");
        mWriteDataEditText.setFocusable(false); // disable editing
        mRdDataDispHexChar.setText("Hex");
        mRdDatahexDispFlag = true;
        //mWriteDataEditText.setFocusableInTouchMode(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "ControlActivity onResume");
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        final IntentFilter bondFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mGattUpdateReceiver, bondFilter);

        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "ControlActivity onPause");
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "ControlActivity onDestroy");
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "ControlActivity onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_pair).setVisible(false);
            menu.findItem(R.id.menu_unpair).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            if(mBluetoothLeService != null) {
                //-------- Check this device is paired -----
                mPaired = mBluetoothLeService.isDevicePaired(mDeviceAddress);
            }
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            if(mPaired) {
                menu.findItem(R.id.menu_pair).setVisible(false);
                menu.findItem(R.id.menu_unpair).setVisible(true);
            } else {
                menu.findItem(R.id.menu_pair).setVisible(true);
                menu.findItem(R.id.menu_unpair).setVisible(false);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "ControlActivity onOptionsItemSelected");
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                processDisconnect();
                return true;
            case R.id.menu_pair:
                mPaired = mBluetoothLeService.pair(mDeviceAddress);
                return true;
            case R.id.menu_unpair:
                mPaired = mBluetoothLeService.unpair(mDeviceAddress);
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void processDisconnect() {
        mBluetoothLeService.disconnect();
        mBluetoothLeService.close();
    }

    public void onToggleButtonClicked(View view) {
        boolean checkedTb = ((ToggleButton)view).isChecked();
        switch(view.getId()) {
            case R.id.indication_sel_tb:
                processIndicationSel(checkedTb);
                break;
            case R.id.read_hex_char_tb:
                if(checkedTb) {
                    mRdDataDispHexChar.setText("Char");
                    mRdDatahexDispFlag = false;
                } else {
                    mRdDataDispHexChar.setText("Hex");
                    mRdDatahexDispFlag = true;
                }
                processIndicationSel(checkedTb);
                break;
        }
    }

    public void onButtonClicked(View view) {
        if(mListSelGattCharct == null)
            return;
        switch(view.getId()) {
            case R.id.read_start_button:
                Log.d(TAG, "ControlActivity read_start_button ");
                mBluetoothLeService.readCharacteristic(mListSelGattCharct);
                break;
            case R.id.write_start_button:
                Log.d(TAG, "ControlActivity write_start_button ");
                String wrMessage = mWriteDataEditText.getText().toString();
                String delims = "[ ]+";
                String[] tokens = wrMessage.split(delims);
                if (tokens.length <= 0) {
                    return;
                }
                 byte[] byteValue = new byte[tokens.length];
                for (int i = 0; i < tokens.length; i++) {
                     byteValue[i] = hexStingToByte(tokens[i]);
                    /*
                    final StringBuilder stringBuilder = new StringBuilder(2);
                    stringBuilder.append(String.format("%02X ", byteValue[i]));
                    Log.d(TAG, "Index: "+i + "  "+stringBuilder.toString());
                    */
                }
                if (mNotifyCharacteristic != null) {
                    mBluetoothLeService.setCharacteristicNotification(
                            mNotifyCharacteristic, false);
                    mNotifyCharacteristic = null;
                }
                mBluetoothLeService.writeCharacteristic(mListSelGattCharct, byteValue);
                break;
         }
    }

     //-- No parsing, just look the first two byte, if valid convert to byte
    //-- String should have valid hex entry, otherwise always return 0 value
    public byte hexStingToByte(String hexStr) {
        byte byteData=0;
        String upperStr;
        if(hexStr.length() < 2)
            return byteData;

        upperStr = hexStr.toUpperCase();
        if((upperStr.charAt(0)<'0') || (upperStr.charAt(0)>'F') ||
                ((upperStr.charAt(0)>'9') && (upperStr.charAt(0)<'A')))
            return byteData;
        if((upperStr.charAt(1)<'0') || (upperStr.charAt(1)>'F') ||
                ((upperStr.charAt(1)>'9') && (upperStr.charAt(1)<'A')))
            return byteData;
        byteData = (byte)((Character.digit(upperStr.charAt(0), 16)<<4) +
                (Character.digit(upperStr.charAt(1), 16)));
        return byteData;
    }


     public void processIndicationSel(boolean enableFlag) {
        if(mListSelGattCharct == null)
            return;
        final int charaProp = mListSelGattCharct.getProperties();
        if((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0)
            return;
        if(enableFlag) {
            mNotifyCharacteristic = mListSelGattCharct;
            mBluetoothLeService.setCharacteristicNotification(
                    mListSelGattCharct, true);
        } else {
            mNotifyCharacteristic = null;
            mBluetoothLeService.setCharacteristicNotification(
                    mListSelGattCharct, false);
        }
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void indDisplayData(String data) {
        if (data != null) {
            notificationCount++;
            mIndCountTxtView.setText(" "+notificationCount);
            mIndDataField.setText(" " + data);
            //mIndDataField.setText(" " +notificationCount + data);
        }
    }
    private void readDisplayData(String data) {
        if (data != null) {
            if(mRdDatahexDispFlag) {
                mReadDataField.setText(" " + data);
            }else {
                String delims = "[ ]+";
                String[] tokens = data.split(delims);
                byte byteValue;
                String charStr = "";
                for (int i = 0; i < tokens.length; i++) {
                    byteValue = hexStingToByte(tokens[i]);
                    charStr += (char) byteValue;
                }
                mReadDataField.setText(" " + charStr);
            }
            //mReadDataField.setText(" " +notificationCount + data);
        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();

            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));

            Log.d(TAG,LIST_NAME+": "+SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            Log.d(TAG,LIST_UUID+": "+uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                String charAttr = "--";
                final int charProp = gattCharacteristic.getProperties();

                if((charProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                    charAttr = charAttr.concat("R-");
                } else {
                    charAttr = charAttr.concat("--");
                }
                if((charProp & BluetoothGattCharacteristic.PERMISSION_WRITE) > 0) {
                    charAttr = charAttr.concat("W-");
                } else {
                    charAttr = charAttr.concat("--");
                }
                if((charProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    charAttr = charAttr.concat("N-");
                } else {
                    charAttr = charAttr.concat("--");
                }

                currentCharaData.put(
                        LIST_NAME, (SampleGattAttributes.lookup(uuid, unknownCharaString))+charAttr);
                Log.d(TAG,"    "+LIST_NAME+": "+SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                Log.d(TAG,"    "+LIST_UUID+": "+uuid);

                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_ERROR);

        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_IND_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_RD_DATA_AVAILABLE);
        intentFilter.addAction("com.example.android.bluetoothlegatt.TEST_ACTION");
        intentFilter.addAction("BluetoothDevice.ACTION_BOND_STATE_CHANGED");
        return intentFilter;
    }
}
