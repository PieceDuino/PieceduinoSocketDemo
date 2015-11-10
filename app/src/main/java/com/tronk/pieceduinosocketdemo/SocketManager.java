package com.tronk.pieceduinosocketdemo;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by qoochuang on 2015/4/22.
 */

public class SocketManager {
    final String Tag = "SocketMgr";

    private static SocketManager socketManager;
    private Socket socket;
    private String SERVER_IP;
    private int SERVER_PORT;
    private int BUFFER_SIZE = 128;
    private PrintWriter printWriter;
    private MessageReceiveThread msgReceiveThread;
    Handler socketStateHandler;
    private static MessageReceiveListener msgReceiveListener;
    private static ConnectionStateListener connStateListener;

    public interface MessageReceiveListener{
        void byteString(String msgFromServer);
        void dataString(String msgFromServer);
    }
    public interface ConnectionStateListener{
        void onConnect();
        void onDisconnect();
    }
    public static SocketManager init(String ip, int port) {
        socketManager = new SocketManager();
        socketManager.SERVER_IP = ip;
        socketManager.SERVER_PORT = port;
        socketManager.Connect();
        return socketManager;
    }
    public static SocketManager init(String ip, int port, int BufferSize) {
        socketManager = new SocketManager();
        socketManager.SERVER_IP = ip;
        socketManager.SERVER_PORT = port;
        socketManager.BUFFER_SIZE = BufferSize;
        socketManager.Connect();
        return socketManager;
    }

    public static SocketManager getInstance(){
        return socketManager;
    }
    public static void setOnMessageReceiveListener(MessageReceiveListener listener){
        msgReceiveListener = listener;
    }
    public static void setConnectionStateListener(ConnectionStateListener listener){
        connStateListener = listener;
    }
    public SocketManager(){
        socketStateHandler = new Handler();
    }
    public boolean isConnect(){
        return socket == null? false:socket.isConnected();
    }
    public void Connect(){
        new SocketClientTask().execute();
    }
    public void Disconnect(){
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket = null;
            socketStateHandler.post(new UpdateDisconnectUIThread());
        }
    }
    public void SendString(String str){
        try {
            printWriter.println(str);
            printWriter.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void SendBytes(byte[] val){
        try{
            socket.getOutputStream().write(val);
            socket.getOutputStream().flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public class SocketClientTask extends AsyncTask<Void, Void, Socket> {
        public SocketClientTask(){}
        @Override
        protected Socket doInBackground(Void... params) {
            try {

                socket = new Socket(SERVER_IP, SERVER_PORT);
                printWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                MessageReceiveThread msgReceiveThread = new MessageReceiveThread(socket, socket.getInputStream(), BUFFER_SIZE);
                msgReceiveThread.start();
                socketStateHandler.post(new UpdateConnectUIThread());
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            return socket;
        }
        @Override
        protected void onPostExecute(Socket socket) {}
    }
    private class MessageReceiveThread extends Thread{
        private Socket sock = null;
        private InputStream is = null;
        private BufferedInputStream bufIs = null;
        private int bufSize;
        public MessageReceiveThread(Socket sock, InputStream is, int BufferSize){
            this.sock = sock;
            this.is = is;
            this.bufIs = new BufferedInputStream(this.is);
            this.bufSize = BufferSize;
        }
        public void run() {
            try {
                byte[] buf = new byte[bufSize];
                String byteStr = "";
                String dataStr = "";
                int read = 0;
                // wait the receiving packet until socket close
                while ((read = bufIs.read(buf)) != 0) {
                    if (read != -1) {
                        byteStr = bytesToString(buf);
                        socketStateHandler.post(new UpdateByteStringThread(byteStr));
                        dataStr = new String(buf,0,buf.length);
                        socketStateHandler.post(new UpdateDataStringThread(dataStr));
                    } else {
                        break;
                    }
                }
                sock.close();
            } catch (Exception e) {
                try {
                    // if a problem occurs in socket, It'll close the socket.
                    e.printStackTrace();
                    Log.e(Tag, "insert to catch \n");
                    sock.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            } finally {
                try {
                    if (bufIs != null)
                        bufIs.close();

                } catch (Exception ex) {
                }
            }
            Log.d(Tag, "MessageReceiveThread Run to End!");
            socketStateHandler.post(new UpdateDisconnectUIThread());
        }
    }
    public static String charTohex(char[] buf) {
        String hexstr = "";
        System.out.println(String.valueOf(buf));
        for(int i=0; i<buf.length; i++){
            if(Integer.toHexString((int)buf[i]).length() < 2) {
                hexstr = hexstr + "0" + Integer.toHexString((int) buf[i]) + " ";
                Log.d("charToHex", Integer.toString((int)buf[i]));
            } else {
                hexstr = hexstr + Integer.toHexString((int) buf[i]) + " ";
                Log.d("charToHex", Integer.toString((int)buf[i]));
            }
        }
        return hexstr;
    }

    public String bytesToString(byte[] buf){
        String hexStr = "";
        for(int i=0; i<buf.length; i++){
            int val = buf[i] & 0xFF;
            if((Integer.toHexString(val)).length() <2)
                hexStr = hexStr + "0" + Integer.toHexString(val) + " ";
            else
                hexStr = hexStr + Integer.toHexString(val) + " ";
        }
        return hexStr;
    }

    public static byte[] hexToBytes(String hexString) {
        char[] hex = hexString.toCharArray();
        //轉rawData長度減半
        int length = hex.length / 2;
        byte[] rawData = new byte[length];
        for (int i = 0; i < length; i++) {
            //先將hex資料轉10進位數值
            int high = Character.digit(hex[i * 2], 16);
            int low = Character.digit(hex[i * 2 + 1], 16);
            //將第一個值的二進位值左平移4位,ex: 00001000 => 10000000 (8=>128)
            //然後與第二個值的二進位值作聯集ex: 10000000 | 00001100 => 10001100 (137)
            int value = (high << 4) | low;
            //與FFFFFFFF作補集
            if (value > 127)
                value -= 256;
            //最後轉回byte就OK
            rawData[i] = (byte) value;
        }
        return rawData;
    }
    public static boolean isIP(String addr){
        if(addr.length() < 7 || addr.length() > 15 || "".equals(addr)){
            return false;
        }
        String rexp = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";
        Pattern pat = Pattern.compile(rexp);
        Matcher mat = pat.matcher(addr);
        boolean ipAddress = mat.find();
        return ipAddress;
    }

    class UpdateByteStringThread implements Runnable {
        private String msg;
        public UpdateByteStringThread(String str) {
            this.msg = str;
        }
        @Override
        public void run() {
            if(msgReceiveListener!=null) {
                msgReceiveListener.byteString(this.msg);
            }
        }
    }
    class UpdateDataStringThread implements Runnable{
        private String msg;
        public UpdateDataStringThread(String str){
            this.msg = str;
        }
        @Override
        public void run() {
            if(msgReceiveListener!=null){
                msgReceiveListener.dataString(this.msg);
            }
        }
    }
    class UpdateConnectUIThread implements Runnable{
        @Override
        public void run() {
            if(connStateListener!=null) connStateListener.onConnect();
        }
    }
    class UpdateDisconnectUIThread implements Runnable{
        @Override
        public void run(){
            if(connStateListener!=null) connStateListener.onDisconnect();
        }
    }
}
