package com.example.bluetooth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Set;

public class DeviceListActivity extends AppCompatActivity {
    private static final String TAG = "BT-APP";
    private static final int LOCATION_PERMISSION_CODE = 2001;
    private RecyclerView mPairedRecyclerView, mAvailableRecyclerView;
    private DeviceListAdapter mPairedAdapter, mAvailableAdapter;
    private RecyclerView.LayoutManager mPairedLayoutManager, mAvailableLayoutManager;
    BluetoothAdapter bluetoothAdapter = null;
    private ProgressBar progressBar;
    private FloatingActionButton scanBtn;
    private ArrayList<DeviceItem> pairedDeviceList, availableDeviceList;
    private boolean isBroadcastReceiver = false;
    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @SuppressLint("NotifyDataSetChanged")
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, device.getName());
                    availableDeviceList.add(new DeviceItem(R.drawable.ic_bluetooth, device.getName(), device.getAddress()));
                    mAvailableAdapter.notifyDataSetChanged();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Toast.makeText(getApplicationContext(), "Scan complete", Toast.LENGTH_LONG).show();
                progressBar.setVisibility(View.GONE);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(getApplicationContext(), "Bluetooth is not enable first of all enable the bluetooth", Toast.LENGTH_LONG).show();
            return;
        }
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
        IntentFilter intentFilter1 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, intentFilter1);
        isBroadcastReceiver = true;
        bluetoothAdapter.startDiscovery();
        availableDeviceList = new ArrayList<>();
        // adding scan button that scan the devices
        scanBtn = findViewById(R.id.scan_btn);
        scanBtn.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onClick(View v) {
                availableDeviceList.clear();
                mAvailableAdapter.notifyDataSetChanged();
                progressBar.setVisibility(View.VISIBLE);
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }
                bluetoothAdapter.startDiscovery();
            }
        });

        locationPermission();
        setPairedDevices();
        createRecyclerView();
    }


    // create the recycler view for paired devices and the available devices
    // and it add the arrayList of the paired and available devices
    private void createRecyclerView() {
        mPairedRecyclerView = findViewById(R.id.recycler);
        mPairedRecyclerView.setHasFixedSize(true);
        mPairedLayoutManager = new LinearLayoutManager(getApplicationContext());
        mPairedAdapter = new DeviceListAdapter(pairedDeviceList);
        mPairedRecyclerView.setLayoutManager(mPairedLayoutManager);
        mPairedRecyclerView.setAdapter(mPairedAdapter);
        mPairedAdapter.setOnItemClickListener(new DeviceListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                DeviceItem device = pairedDeviceList.get(position);
                Intent returnIntent = new Intent();
                returnIntent.putExtra("MAC_ADDRESS", device.getDeviceMac());
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }
        });

        mAvailableRecyclerView = findViewById(R.id.recycler2);
        mAvailableRecyclerView.setHasFixedSize(true);
        mAvailableLayoutManager = new LinearLayoutManager(getApplicationContext());
        mAvailableAdapter = new DeviceListAdapter(availableDeviceList);
        mAvailableRecyclerView.setLayoutManager(mAvailableLayoutManager);
        mAvailableRecyclerView.setAdapter(mAvailableAdapter);

        mAvailableAdapter.setOnItemClickListener(new DeviceListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                DeviceItem device = availableDeviceList.get(position);
                Intent returnIntent = new Intent();
                returnIntent.putExtra("MAC_ADDRESS", device.getDeviceMac());
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }
        });
    }

    // it actually set the array list with the paired device set
    private void setPairedDevices() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        pairedDeviceList = new ArrayList<>();
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                pairedDeviceList.add(new DeviceItem(R.drawable.ic_bluetooth, device.getName(), device.getAddress()));
            }
        }
    }

    // check the location permission and request for that
    private void locationPermission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            requestLocationPermission();
    }

    // request the location permission if it is not granted then display for a dialogue box
    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permission needed")
                    .setMessage("This permission is needed because of this and that")
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(DeviceListActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
                        }
                    })
                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create().show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this, "Permission DENIED", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBroadcastReceiver)
            unregisterReceiver(receiver);
        bluetoothAdapter.cancelDiscovery();
    }
}