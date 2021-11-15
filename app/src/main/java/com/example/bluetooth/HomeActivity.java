package com.example.bluetooth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "BT-APP";
    private static final int BT_STOP = 4000;
    private static final int BT_LISTENING = 4001;
    private static final int BT_CONNECTING = 4002;
    private static final int BT_CONNECTED = 4003;
    private static final int BT_CONNECTION_FAILED = 4004;
    private static final int BT_MESSAGE_RECEIVED = 4005;
    private ActionBar homeAB;
    BluetoothAdapter bluetoothAdapter = null;
    public static final int REQUEST_ENABLE_BT = 1001;
    public static final int REQUEST_DISCOVER_BT = 1002;
    public static final int RESULT_BT_MAC = 3001;
    Menu homeMenu = null;
    BluetoothDevice selectedDevice = null;
    public static UUID MY_UUID = null;
    private MessageThread messageThread = null;
    private ArrayAdapter<String> chatAdapter;
    private ServerThread serverThread = null;
    private ClientThread clientThread = null;
    private EditText chatEditText;
    private ImageButton sendBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        chatEditText = findViewById(R.id.chatEditText);
        sendBtn = findViewById(R.id.sendButton);

        MY_UUID = UUID.fromString(getResources().getString(R.string.BT_UUID));

        // setting new toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        homeAB = getSupportActionBar();

        // getting bluetooth adapter class
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // checking tha the there is any kind of adapter available in the device
        if (bluetoothAdapter == null)
            Toast.makeText(getApplicationContext(), "Sorry there is no bluetooth device !", Toast.LENGTH_LONG).show();
        else
            Log.d(TAG, "Found Bluetooth adapter in the device");

        // testing chat
        ListView listView = findViewById(R.id.chatListView);
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add("me:\n hi");
        arrayList.add("me:\n hi");
        arrayList.add("me:\n hi");
        arrayList.add("me:\n hi");
        arrayList.add("me:\n hi");
        arrayList.add("me:\n hi");
        arrayList.add("me:\n hi");

        chatAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.test_list_item, arrayList);
        listView.setAdapter(chatAdapter);

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (messageThread != null) {
                    String message = String.valueOf(chatEditText.getText());
                    String sendMessage = bluetoothAdapter.getName() + "\n" + message;
                    messageThread.write(sendMessage.getBytes());
                    chatEditText.setText("");
                    messageAdd("me \n" + message);
                }
            }
        });
    }

    // creating option menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.home_menu, menu);
        homeMenu = menu;
        if (bluetoothAdapter.isEnabled())
            menu.findItem(R.id.bluetooth_enable).setIcon(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_bluetooth_connected));
        return true;
    }
    // adding click listener

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.bluetooth_enable:
                enableBluetooth();
                break;
            case R.id.device_list:
                Intent deviceList = new Intent(getApplicationContext(), DeviceListActivity.class);
                startActivityForResult(deviceList, RESULT_BT_MAC);
                break;
            case R.id.server_start:
                startServerThread();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // checking the bluetooth activity
        switch (requestCode) {
            case REQUEST_DISCOVER_BT:
                if (resultCode == Activity.RESULT_OK)
                    Log.d(TAG, "Bluetooth discover successful");
                else
                    Log.d(TAG, "Bluetooth discover not successful");

                break;
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    Log.d(TAG, "Bluetooth enable successful");
                    homeMenu.findItem(R.id.bluetooth_enable).setIcon(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_bluetooth_connected));
                    discoverEnable();
                } else {
                    Log.d(TAG, "Bluetooth not successful");
                }
                break;
            case RESULT_BT_MAC:
                if (resultCode == Activity.RESULT_OK) {
                    assert data != null;
                    String macAddress = data.getStringExtra("MAC_ADDRESS");
                    selectedDevice = bluetoothAdapter.getRemoteDevice(macAddress);
                    startClientThread();
                }
                break;
        }
    }

    private void discoverEnable() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivityForResult(discoverableIntent, REQUEST_DISCOVER_BT);
    }

    private void enableBluetooth() {
        // if the bluetooth is enable the disable it otherwise enable it
        if (!bluetoothAdapter.isEnabled()) {
            Log.d(TAG, "Try to enable the bluetooth");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            Log.d(TAG, "Disable the bluetooth");
            bluetoothAdapter.disable();
            homeMenu.findItem(R.id.bluetooth_enable).setIcon(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_bluetooth));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothAdapter.cancelDiscovery();
        stopAll();
    }

    private void messageAdd(String message) {
        chatAdapter.add(message);
    }


    // bluetooth thread class that help to connect the devices

    // message handler
    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            switch (msg.what) {
                case BT_STOP:
                    homeAB.setSubtitle(null);
                    stopAll();
                    break;
                case BT_LISTENING:
                    homeAB.setSubtitle("Listening...");
                    break;
                case BT_CONNECTING:
                    homeAB.setSubtitle("Connecting...");
                    break;
                case BT_CONNECTED:
                    homeAB.setSubtitle("Connected");
                    break;
                case BT_CONNECTION_FAILED:
                    homeAB.setSubtitle("Failed");
                    stopAll();
                    break;
                case BT_MESSAGE_RECEIVED:
                    byte[] readBuff = (byte[]) msg.obj;
                    String tempMsg = new String(readBuff, 0, msg.arg1);
                    messageAdd(tempMsg);
                    break;
            }
            return true;
        }
    });

    private void startServerThread() {
        if (serverThread == null) {
            serverThread = new ServerThread();
            serverThread.start();
        } else {
            serverThread.cancel();
            serverThread.interrupt();
            serverThread = null;
        }
    }

    private void startClientThread() {
        if (clientThread == null) {
            clientThread = new ClientThread(selectedDevice);
            clientThread.start();
        } else {
            clientThread.cancel();
            clientThread = null;
        }
    }

    private void stopAll() {
        if (serverThread != null) {
            serverThread.cancel();
            serverThread = null;
        }
        if (clientThread != null) {
            clientThread.cancel();
            clientThread = null;
        }
        if (messageThread != null) {
            messageThread = null;
        }
    }

    // server thread
    private class ServerThread extends Thread {
        private final BluetoothServerSocket mServerSocket;
        private boolean exit = true;
        public ServerThread() {
            BluetoothServerSocket tmp = null;

            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("SOUMEN", MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }

            mServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;

            while (exit) {
                try {
                    Message message = Message.obtain();
                    message.what = BT_LISTENING;
                    handler.sendMessage(message);
                    socket = mServerSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    Message message = Message.obtain();
                    message.what = BT_CONNECTION_FAILED;
                    handler.sendMessage(message);
                    break;
                }
                if (socket != null) {
                    Message message = Message.obtain();
                    message.what = BT_CONNECTED;
                    handler.sendMessage(message);

                    messageThread = new MessageThread(socket);
                    messageThread.start();
                    break;
                }
            }

            try {
                mServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            exit = false;
            Message message = Message.obtain();
            message.what = BT_STOP;
            handler.sendMessage(message);
        }
    }

    // client thread
    private class ClientThread extends Thread {
        private BluetoothDevice mDevice;
        private BluetoothSocket mSocket;

        public ClientThread(BluetoothDevice device) {
            mDevice = device;
            BluetoothSocket tmp = null;

            try {
                tmp = mDevice.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }

            mSocket = tmp;
        }

        public void run() {
            bluetoothAdapter.cancelDiscovery();

            try {
                // TODO: later it will be checked by running the application
                Message message = Message.obtain();
                message.what = BT_CONNECTING;
                handler.sendMessage(message);
                mSocket.connect();
                message.what = BT_CONNECTED;
                handler.sendMessage(message);

                messageThread = new MessageThread(mSocket);
                messageThread.start();

            } catch (IOException e) {
                e.printStackTrace();
                Message message = Message.obtain();
                message.what = BT_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
        }

        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }


    // chat thread
    private class MessageThread extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;
        private boolean exit = true;

        public MessageThread(BluetoothSocket socket) {
            bluetoothSocket = socket;
            InputStream tempIn = null;
            OutputStream tempOut = null;

            try {
                tempIn = bluetoothSocket.getInputStream();
                tempOut = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            inputStream = tempIn;
            outputStream = tempOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = inputStream.read(buffer);
                    handler.obtainMessage(BT_MESSAGE_RECEIVED, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }

            Message message = Message.obtain();
            message.what = BT_STOP;
            handler.sendMessage(message);
        }

        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}