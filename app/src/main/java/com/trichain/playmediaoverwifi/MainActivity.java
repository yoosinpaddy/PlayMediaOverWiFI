package com.trichain.playmediaoverwifi;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.text.format.Formatter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.trichain.playmediaoverwifi.databinding.ActivityMainBinding;
import com.trichain.playmediaoverwifi.models.MySongs;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_CODE = 11;
    private Handler handler = new Handler();
    private Socket socket;
    private DataOutputStream outputStream;
    private BufferedReader inputStream;
    private String DeviceName = "Device";
    private static int PORT = 1543;
    ActivityMainBinding b;
    boolean isServer = false;
    boolean isConnected = false;
    private static final String TAG = "MainActivity";
    ArrayList<MySongs> allSongs = new ArrayList<>();

    private boolean searchNetwork() {
        log("Connecting");
//        String range = "192.168.88.";
        if (getIP().contentEquals("0.0.0.0")){
            return false;
        }
        String[] range2 = getIP().split("\\.");
        Log.e(TAG, "searchNetwork: "+range2.length );
        String range3 = getIP().split("\\.")[3];
        String range = getIP().replace(range3,"");
        Log.e(TAG, "searchNetwork: range:"+range );
        for (int i = 1; i <= 20; i++) {
            String ip = range + i;
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(ip, PORT), 200);
                outputStream = new DataOutputStream(socket.getOutputStream());
                inputStream = new BufferedReader(new InputStreamReader(
                        socket.getInputStream()));
                DeviceName += "1";
                Log.i("Server", DeviceName);
                log("Connected");
                Thread.sleep(50);
                return true;
            } catch (ConnectException ce) {
                Log.e(TAG, "searchNetwork: ConnectException:" + ip + "  " + ce.getMessage());
                ce.printStackTrace();
            } catch (Exception e) {
                Log.e(TAG, "searchNetwork: " + ip + "  " + e.getMessage());
            }
        }
        return false;

    }

    private void runNewChatServer() {
        Log.e(TAG, "runNewChatServer: ");
//        createHotSpot();
        ServerSocket serverSocket;
        try {
            socket = new Socket();
            serverSocket = new ServerSocket(PORT);
            log("Waiting for client...");
            isServer = true;
            socket = serverSocket.accept();
            DeviceName += "2";
            log("a new client Connected");
        } catch (IOException e) {
            log(e.getMessage());
            Log.e(TAG, "runNewChatServer: " + e.getMessage());
        }

    }
    public String getIP(){
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        Log.e(TAG, "getIP: "+ip );
        return ip;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = DataBindingUtil.setContentView(this, R.layout.activity_main);
        //create socket
        socket = new Socket();
        //Permit Strict rules
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        initViews();
        startTheNetwork();
        keepCheckingPorts();


    }

    private void keepCheckingPorts() {
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//
//            }
//        })
    }

    private void initViews() {

        b.send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                String Message = b.input.getText().toString() + "\n";
                if (sendMessage(Message)) {
                    Log.e(TAG, "onClick: message sent");
                } else {
                    Log.e(TAG, "onClick: message not sent");
                }
            }
        });
        b.connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                String myIP = b.myIp.getText().toString();
                Pattern IP_ADDRESS
                        = Pattern.compile(
                        "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
                                + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
                                + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                                + "|[1-9][0-9]|[0-9]))");
                Matcher matcher = IP_ADDRESS.matcher(myIP);
                if (matcher.matches()) {
                    // ip is correct
                    Log.e(TAG, "onClick: correct IP");
                    try {
                        socket = new Socket();
                        socket.connect(new InetSocketAddress(myIP, PORT), 100);
                        outputStream = new DataOutputStream(socket.getOutputStream());
                        inputStream = new BufferedReader(new InputStreamReader(
                                socket.getInputStream()));
                        DeviceName += "1";
                        Log.i("Server", DeviceName);
                        log("Connected");
                        if (sendMessage("connected to" + myIP)) {
                            Log.e(TAG, "onClick: message sent");
                        } else {
                            Log.e(TAG, "onClick: message not sent");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "onClick: " + myIP + ":" + e.getMessage());
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Please enter a valid ip", Toast.LENGTH_SHORT).show();
                }
            }
        });

        b.media.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                sendMessage("get media list");
            }
        });
    }

    private boolean sendMessage(String message) {
        Log.e(TAG, "sendMessage: " + message);


        if (outputStream == null) {
            Log.e(TAG, "sendMessageFailed: no output stream");
            return false;
        }
        try {
//            outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.write(message.getBytes());
            log2(message);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    b.input.setText("");
                }
            });
//            outputStream.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "sendMessageFailed: " + e.getMessage());
//            try {
////                outputStream.close();
//            } catch (IOException ex) {
//                ex.printStackTrace();
//            }
            return false;
        }
    }

    private void startTheNetwork() {
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    boolean hasFound = searchNetwork();
                    if (!hasFound) {
                        runNewChatServer();
                    }

                    outputStream = new DataOutputStream(
                            socket.getOutputStream());

                    if (hasFound) {
                        Log.e(TAG, "run: has found");
                        sendMessage("allow connect");
                    } else {
                        Log.e(TAG, "run: has not found");
                    }
                    inputStream = new BufferedReader(new InputStreamReader(
                            socket.getInputStream()));
                    while (true) {
                        String message = inputStream.readLine();
                        if (message != null) {
                            Log.e(TAG, "run: recieved message:" + message);
                            if (message.contentEquals("allow connect")) {
                                if (!isConnected) {
                                    sendMessage("allow connect");
                                }
                                startMediaRequests();
                                isConnected = true;
                            } else {
                                handleMessages(message);
                            }
                            log(message);
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "run: " + e.getMessage());
                    log("Error: IO Exception");
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    }

    private void handleMessages(String message) {
        Log.e(TAG, "handleMessages: " + message);
        if (message.contentEquals("get media list")) {
            if (allSongs.size() == 0) {
                getMediaSongs();
            }
            sendMessage(getMediaArray().toString());
        } else if (isJSONObject(message)) {
            //Contains media files
            Log.e(TAG, "handleMessages: media size:" + allSongs.size());
        } else {
            Log.e(TAG, "handleMessages: " + message);

        }
    }

    private boolean isJSONObject(String message) {
        try {
            JSONObject o = new JSONObject(message);
            o.get("MySongs");
            if (message.contains("MySongs")) {
                allSongs = new ArrayList<>();
                JSONArray arr = o.getJSONArray("MySongs");
                for (int i = 0; i < arr.length(); i++) {
                    allSongs.add(new MySongs(arr.getJSONObject(i)));
                }
                return true;
            } else {
                Log.e(TAG, "isJSONObject: but doesnt contain songs");
                return false;
            }
        } catch (JSONException e) {
            Log.e(TAG, "isJSONObject: " + e.getMessage());
            return false;
        }
    }

    private void getMediaSongs() {
        Log.e(TAG, "getMediaSongs: ");
        allSongs = new ArrayList<>();
        ContentResolver cr = MainActivity.this.getContentResolver();

        //Some audio may be explicitly marked as not being music
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION
        };

        Cursor c = this.managedQuery(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null);

        while (c.moveToNext()) {
            allSongs.add(new MySongs(c.getString(4), c.getString(3), c.getString(0)));
            Log.e(TAG, "getMediaSongs: " + c.getString(4));
        }
        log("Total songs:" + allSongs.size());
    }

    private JSONObject getMediaArray() {
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        for (MySongs m : allSongs) {
            jsonArray.put(m.getJSONObject());
        }
        try {
            jsonObject.put("MySongs", jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    private void startMediaRequests() {
        Log.e(TAG, "startMediaRequests: ");
        if (!isServer) {
            sendMessage("get media list");
        } else {
            getMediaSongs();
        }
    }

    private void log(final String message) {
        handler.post(new Runnable() {
            String DeviceName2 = "";

            @Override
            public void run() {
                if (DeviceName.equals("Device1")) {
                    DeviceName2 = "Device2";
                } else if (DeviceName.equals("Device2")) {
                    DeviceName2 = "Device1";
                } else {
                    DeviceName2 = "UnknowDevice";
                }

                b.text.setText(b.text.getText() + "\n" + DeviceName2 + " :"
                        + message);

            }
        });
    }

    private void log2(final String message) {
        handler.post(new Runnable() {

            @Override
            public void run() {


                b.text.setText(b.text.getText() + "\n" + "you" + " :"
                        + message);

            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createHotSpot() {
        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION)){
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_CODE);
            }else{
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_CODE);
            }
            return;
        }
        manager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {

            @Override
            public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                super.onStarted(reservation);
                Log.d(TAG, "Wifi Hotspot is on now");
            }

            @Override
            public void onStopped() {
                super.onStopped();
                Log.d(TAG, "onStopped: ");
            }

            @Override
            public void onFailed(int reason) {
                super.onFailed(reason);
                Log.d(TAG, "onFailed: ");
            }
        }, new Handler());
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            System.exit(0);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}