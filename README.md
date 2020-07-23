
Android BluetoothLeGatt Sample
==============================

This repo has been migrated to [github.com/android/connectivity][1]. Please check that repo for future updates. Thank you!

[1]: https://github.com/android/connectivity

KSJ cloned it from https://github.com/googlearchive/android-BluetoothLeGatt.git:
To run the commands from command line, use the adb_cmd_parser application, soruce code available as adb_cmd_parser.c
Generate the executable file using command
"gcc -o adb_cmd_parser adb_cmd_parser.c"
Can use this utility to issue the adb commands, the utility gets the logcat message from the phone though the adb interface.
General command format for PC
./adb_cmd_parser <command> <timeoutInSeconds> <numberOfLinesToBeDisplayed> <otherCommandDependentParam>
Commands avaialble
./adb_cmd_parser.exe start_app                          //Start the app
./adb_cmd_parser.exe start_scan    12 16                   //Start the BLE scan, wait for 12 seconds or 16 lines of messages
./adb_cmd_parser.exe stop_scan     1 1                     //Stop the scan, 1 second wait and display one response line
./adb_cmd_parser.exe ls_scan       4 16                    //Display the current scan list, wait for a maximum of 4 seconds or 16 lines of response
./adb_cmd_parser.exe start_pair    4 6 A <DeviceAddress>   //Pair Device with the given address
./adb_cmd_parser.exe start_pair    4 6 N <DeviceNmae>      //Pair Device with the given Name
./adb_cmd_parser.exe drop_pair     4 2 A <DeviceAddress>   //UnPair Device with the given address
./adb_cmd_parser.exe drop_pair     4 2 A <DeviceNmae>      //UnPair Device with the given Name
./adb_cmd_parser.exe ls_pair       2,4                     //List the paired deivices
./adb_cmd_parser.exe start_connect 8,2 A <DeviceAddress>   //Connect to Device with the given address
./adb_cmd_parser.exe start_connect 8,2 N <DeviceNmae>      //Connect to Device with the given Name
./adb_cmd_parser.exe drop_connect  2,2 A <DeviceAddress>   //Disconnect to Device with the given address
./adb_cmd_parser.exe drop_connect  2,2 N <DeviceNmae>      //Disconnect to Device with the given Name
./adb_cmd_parser.exe start_ind     2,2  <UUID>             //Start indication on given UUID
./adb_cmd_parser.exe stop_ind      2,2  <UUID>


adb commands
Command from adb shell used for the testing, should see the response on the adb shell, can filter the message using TAG=BLE_TEST
adb shell "am broadcast -a com.example.android.bluetoothlegatt.TEST_ACTION --es com.example.android.bluetoothlegatt.EXTRA_TEXT 'start_scan'"
adb shell "am broadcast -a com.example.android.bluetoothlegatt.TEST_ACTION --es com.example.android.bluetoothlegatt.EXTRA_TEXT 'stop_scan'"
adb shell "am broadcast -a com.example.android.bluetoothlegatt.TEST_ACTION --es com.example.android.bluetoothlegatt.EXTRA_TEXT 'lsscan'"
adb shell "am broadcast -a com.example.android.bluetoothlegatt.TEST_ACTION --es com.example.android.bluetoothlegatt.EXTRA_TEXT 'lspair'"

adb shell "am broadcast -a com.example.android.bluetoothlegatt.TEST_ACTION --es com.example.android.bluetoothlegatt.EXTRA_TEXT 'connect N <name from the scan result>'"
adb shell "am broadcast -a com.example.android.bluetoothlegatt.TEST_ACTION --es com.example.android.bluetoothlegatt.EXTRA_TEXT 'connect A <addr from the scan result>'"
adb shell "am broadcast -a com.example.android.bluetoothlegatt.TEST_ACTION --es com.example.android.bluetoothlegatt.EXTRA_TEXT 'disconnect'"
adb shell "am broadcast -a com.example.android.bluetoothlegatt.TEST_ACTION --es com.example.android.bluetoothlegatt.EXTRA_TEXT 'indication <UUID>'"
adb shell "am broadcast -a com.example.android.bluetoothlegatt.TEST_ACTION --es com.example.android.bluetoothlegatt.EXTRA_TEXT 'pair N <name from the scan result>'"
adb shell "am broadcast -a com.example.android.bluetoothlegatt.TEST_ACTION --es com.example.android.bluetoothlegatt.EXTRA_TEXT 'pair A <address from the scan result>'"

adb shell "am broadcast -a com.example.android.bluetoothlegatt.TEST_ACTION --es com.example.android.bluetoothlegatt.EXTRA_TEXT 'unpair N <name from the scan result>'"
adb shell "am broadcast -a com.example.android.bluetoothlegatt.TEST_ACTION --es com.example.android.bluetoothlegatt.EXTRA_TEXT 'unpair A <address from the scan result>'"
