package com.example.p2pmessagingapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuView;

import android.Manifest;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Locale;

public class  MainActivity extends AppCompatActivity{

    EditText receivePortEditText, targetPortEditText, messageEditText, targetIPEditText;
    TextView chatText;

    ServerClass serverClass;
    ClientClass clientClass;
    SendReceive sendReceive;
    String fullMessage="";
    int col=1;
    ScrollView sc;

    MenuView.ItemView itOn,itOff;
    WifiManager wifiManager;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    static final int MESSAGE_READ=1;
    static final String TAG = "yourTag";

    Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what)
            {
                case MESSAGE_READ:
                    byte[] readBuff= (byte[]) msg.obj;
                    String tempMsg=new String(readBuff,0,msg.arg1);
                    String[] message = tempMsg.split("321",0);
                    if(tempMsg.equals("123")){
                        sc.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_bright));
                        col=0;
                    }else if(tempMsg.equals("124")){
                        sc.setBackgroundColor(Color.parseColor("#212121"));
                        col =1;
                    }else if(message[0].equals("#")){
                        writeToFile("Text "+ targetIPEditText.getText().toString(), message[1]);
                    }else {
                        fullMessage = fullMessage + "\n Him:" + tempMsg;
                        chatText.setText(fullMessage);
                        sc.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                    break;
            }
            return true;
        }
    });

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if(requestCode==123 && resultCode==RESULT_OK) {
            Uri uri = intent.getData();
            String textInsideTheSelectedFile = uriToString(uri);
            String msgText = "#321" + textInsideTheSelectedFile;
            sendReceive.write(msgText.getBytes());
        }
        if(requestCode==10){
            if (resultCode == RESULT_OK && intent != null) {
                ArrayList<String> result = intent.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                messageEditText.setText(result.get(0));
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //receivePortEditText = findViewById(R.id.receiveEditText);
        //targetPortEditText = findViewById(R.id.targetPortEditText);
        messageEditText = findViewById(R.id.messageEditText);
        targetIPEditText = findViewById(R.id.targetIPEditText);
        chatText = findViewById(R.id.chatText);
        sc = findViewById(R.id.scroll1);
        itOn=findViewById(R.id.it1);
        itOff = findViewById(R.id.it2);
        wifiManager= (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        serverClass = new ServerClass(8888);
        serverClass.start();

        //messageEditText.requestFocus();
        //getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        verifyStoragePermissions();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.it1:
                if(!wifiManager.isWifiEnabled()){
                    wifiManager.setWifiEnabled(true);
                }
                return true;
            case R.id.it2:
                if(wifiManager.isWifiEnabled()){
                    wifiManager.setWifiEnabled(false);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*public void onStartServerClicked(View v){
        String port = receivePortEditText.getText().toString();
        serverClass = new ServerClass(Integer.parseInt(port));
        serverClass.start();
    //}*/

    public void onConnectClicked(View v){
       // String port = targetPortEditText.getText().toString();
        clientClass = new ClientClass(targetIPEditText.getText().toString(), 8888);
        clientClass.start();
    }

    public void onSendClicked(View v){
        String msg=messageEditText.getText().toString();
        fullMessage = fullMessage +"\n Me:"+msg;
        chatText.setText(fullMessage);
        sendReceive.write(msg.getBytes());
        sc.fullScroll(ScrollView.FOCUS_DOWN);
    }

    public void onSaveConversationClicked(View v){
        writeToFile("Conv with"+targetIPEditText.getText().toString(), fullMessage);
    }

    public void onChangeBackground(View v){
        String colorChange="" ;
        //chatText.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_bright));
        if(col==1){
            sc.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_bright));
            col=0;
            colorChange = colorChange+"123";
        }else if(col==0){
            sc.setBackgroundColor(Color.parseColor("#212121"));
            col=1;
            colorChange=colorChange+"124";
        }
        sendReceive.write(colorChange.getBytes());
    }
    public void onsendFileClicked(View v){
        Intent intent = new Intent().setType("text/plain").setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select a TXT file"), 123);
    }

    public void onMicClicked(View v){
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, 10);
        } else {
            Toast.makeText(this, "Your Device Don't Support Speech Input", Toast.LENGTH_SHORT).show();
        }
    }




    public class ServerClass extends Thread{
        Socket socket;
        ServerSocket serverSocket;
        int port;

        public ServerClass(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            try {
                serverSocket=new ServerSocket(port);
                Log.d(TAG, "Waiting for client...");
                socket=serverSocket.accept();
                Log.d(TAG, "Connection established from server");
                sendReceive=new SendReceive(socket);
                sendReceive.start();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "ERROR/n"+e);
            }
        }
    }

    private class SendReceive extends Thread{
        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public SendReceive(Socket skt)
        {
            socket=skt;
            try {
                inputStream=socket.getInputStream();
                outputStream=socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1677700];
            int bytes;
            while (socket!=null)
            {
                try {
                    bytes=inputStream.read(buffer);
                    if(bytes>0)
                    {
                        handler.obtainMessage(MESSAGE_READ,bytes,-1,buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

		public void write(final byte[] bytes) {
            new Thread(new Runnable(){
                @Override
                public void run() {
                    try {
                        outputStream.write(bytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        }
    }

    public class ClientClass extends Thread{
        Socket socket;
        String hostAdd;
        int port;

        public  ClientClass(String hostAddress, int port)
        {            this.port = port;
            this.hostAdd = hostAddress;
        }

        @Override
        public void run() {
            try {

                socket=new Socket(hostAdd, port);
                Log.d(TAG, "Client is connected to server");
                sendReceive=new SendReceive(socket);
                sendReceive.start();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "Can't connect from client/n"+e);
            }
        }
    }


    public void verifyStoragePermissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                requestPermissions(
                        PERMISSIONS_STORAGE,
                        REQUEST_EXTERNAL_STORAGE
                );
            }
        }
    }

    private void writeToFile(String fileName, String data) {
        Long time= System.currentTimeMillis();
        String timeMill = " "+time.toString();
        File defaultDir = Environment.getExternalStorageDirectory();
        File file = new File(defaultDir, fileName+".txt");
        FileOutputStream stream;
        try {
            stream = new FileOutputStream(file, false);
            stream.write(data.getBytes());
            stream.close();
            Toast.makeText(getApplicationContext(),"file saved in: "+file.getPath(),Toast.LENGTH_SHORT).show();
            //showToast("file saved in: "+file.getPath());
        } catch (FileNotFoundException e) {
            Log.d(TAG, e.toString());
        } catch (IOException e) {
            Log.d(TAG, e.toString());
        }
    }

    //file chooser
    private String uriToString(Uri uri){
        BufferedReader reader = null;
        StringBuilder builder = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)));
            String line = "";

            while ((line = reader.readLine()) != null) {
                builder.append("\n"+line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null){
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return builder.toString();
    }

}
