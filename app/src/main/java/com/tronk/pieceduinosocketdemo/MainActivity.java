package com.tronk.pieceduinosocketdemo;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {
    final String TAG = "PieceduinoSocketDemo";
    String IP = "192.168.4.1";
    int PORT = 8090;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        final Button btnSend = (Button) findViewById(R.id.btnSend);
        final Button btnConn = (Button) findViewById(R.id.btnConn);
        btnConn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(btnConn.getText().equals("Connect")) {
                    int ReceiveMsgBufferSize = 20; // From Pieceduino "Hello This is Server"
                    SocketManager.init(IP, PORT, ReceiveMsgBufferSize);
                    SocketManager.setConnectionStateListener(new SocketManager.ConnectionStateListener() {
                        @Override
                        public void onConnect() {
                            btnConn.setText("Disconnect");
                            btnSend.setEnabled(true);
                        }
                        @Override
                        public void onDisconnect() {
                            btnConn.setText("Connect");
                            btnSend.setEnabled(false);
                        }
                    });
                    SocketManager.setOnMessageReceiveListener(new SocketManager.MessageReceiveListener() {
                        @Override
                        public void byteString(String msgFromServer) {
                            Log.d(TAG, "Byte String From Server: " + msgFromServer);
                        }
                        @Override
                        public void dataString(String msgFromServer) {
                            Log.d(TAG, "Data String From Server: " + msgFromServer);
                        }
                    });
                    SocketManager.getInstance().Connect();
                }else{
                    SocketManager.getInstance().Disconnect();
                }
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(SocketManager.getInstance().isConnect())
                    SocketManager.getInstance().SendString("Hello Pieceduino");
            }
        });
    }
}
