package hsm.demo.simplebtcom;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.SimpleTimeZone;
import java.util.UUID;

import javax.crypto.Mac;

public class bt_send implements Runnable {

    Context m_context;
    String TAG="BTComDemo";

    String MacAddress;
    private BluetoothSocket mSocket=null;
    private InputStream mInStream=null;
    private OutputStream mOutStream=null;
    private static final UUID UUID_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothDevice mDevice=null;
    private BluetoothAdapter mBluetoothAdapter=null;

    MainActivity mainActivity;

    public bt_send(MainActivity activity, Context context, String sMac){
        m_context=context;
        MacAddress=sMac;
        mainActivity=activity;
    }

    public void run(){
        final String mac= MacAddress;
        mainActivity.addLog("New thread started: " + Thread.currentThread().getName());
        mainActivity.addLog("DISABLE button");
//        connectAndSend(mac, csimQuery());

        BluetoothDevice device;
        String sMacAddr=mac;
        byte[] sendBytes=csimQuery();
        try {
            // Get local Bluetooth adapter
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            device = mBluetoothAdapter.getRemoteDevice(sMacAddr);
            mainActivity.addLog("BT device found: " + device.getName());
            Log.d(TAG, "BT device found: " + device.getName());

        }catch (Exception e){
            Toast.makeText(m_context,"Invalid BT MAC address", Toast.LENGTH_LONG);
            mainActivity.addLog("Failed to get BT device by Mac address!");
            Log.d(TAG, "Failed to get BT device by Mac address!");
            device=null;
        }

        byte[] buf=new byte[200];
        int maxTry=10;
        int iTry=0;
        if (device != null) {
            addLog("connecting to " + sMacAddr);
            mainActivity.addLog("connecting to " + sMacAddr + " ...");
            try {
                addLog("createInsecureRfcommSocketToServiceRecord");
                mSocket = device.createInsecureRfcommSocketToServiceRecord(UUID_SPP);
                //tmp = device.createRfcommSocketToServiceRecord(UUID_SPP);
                Thread.sleep(500);
                mSocket.connect();
                mainActivity.addLog("BT socket connected");
                // Get the BluetoothSocket input and output streams
                try {
                    mInStream = mSocket.getInputStream();
                    mOutStream = mSocket.getOutputStream();
                    Thread.sleep(500);
                    mOutStream.write(new byte[2000]); //wake printer
                    mOutStream.flush();
                    Thread.sleep(500);
                    mOutStream.write(sendBytes);
                    mOutStream.flush();
                    Thread.sleep(500);
                    Log.d(TAG, "write done");
                    mainActivity.addLog("CSim version requested");
                    String sIn = ""; int iCnt=0;
                    do {
                        mOutStream.write(new byte[]{}); //dummy writes to make Android BT happy
                        int iAvailableBytes = mInStream.available();
                        Log.d(TAG, "mInStream.available()="+ mInStream.available());
                        iCnt = mInStream.read(buf, 0, iAvailableBytes);
                        if(iCnt>0){
                            String s = new String(buf, 0, iCnt);
                            Log.d(TAG, "received: " + s);
                            sIn += s;
                            if (sIn.endsWith("\r\n")) {
                                Log.d(TAG, "receive complete: "+ sIn);
                                mainActivity.addLog("Received: "+ sIn);
                                String[] recvdLines=sIn.split("\r\n");
                                for (String line:recvdLines
                                     ) {
                                    if(line.startsWith("CSIM")){
                                        mainActivity.addLog("Firmware version is " + line);
//                                        mOutStream.write( Charset.forName("UTF-8").encode("10 PRPOS 160,250").array());
//                                        mOutStream.write( Charset.forName("UTF-8").encode("20 FONT \"Swiss 721 BT\"").array());
//                                        mOutStream.write( Charset.forName("UTF-8").encode("30 PRTXT \"" + sIn + "\"").array() );
//                                        mOutStream.write( Charset.forName("UTF-8").encode("40 PF").array() );
//                                        mOutStream.write( Charset.forName("UTF-8").encode("run").array() );
                                    }
                                }
                                if(sIn.startsWith("CSIM")){
                                    mOutStream.write( Charset.forName("UTF-8").encode("10 FONT \"Swiss 721 BT\"").array());
                                    mOutStream.write( Charset.forName("UTF-8").encode("20 PRTXT \"" + sIn + "\"").array() );
                                    mOutStream.write( Charset.forName("UTF-8").encode("30 PF").array() );
                                    mOutStream.write( Charset.forName("UTF-8").encode("run").array() );
                                }
                                break;
                            }
                        }
                        iTry++;
                        Thread.sleep(500);
                    }while(iTry<maxTry || iCnt>0);
                    mInStream.close();
                    mOutStream.close();
                    mSocket.close();
                    mainActivity.addLog("BT socket closed");

                } catch (IOException e) {
                    Log.e(TAG, "temp sockets not created", e);
                    mainActivity.addLog("Failed to connect to BT socket!");
                }

            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
                mainActivity.addLog("socket create failed!");
            } catch (InterruptedException e) {
                Log.e(TAG, "create() failed", e);
                mainActivity.addLog("socket stream interrupted!");
            }
        } else {
            addLog("unknown remote device!");
            mainActivity.addLog("unknown remote device!");
        }
        mainActivity.addLog("ENABLE button");
    }

    final byte[] csimQuery(){
        String sBuf = ""; // "!U1\r\nVERSION\r\n"; //print CSim version
        sBuf+= "QUIT\r\n";                      // quit CSim emulation
        sBuf+="PRINT VERSION$(0)\r\n";          // FP to send version back
        //sBuf+="run \"setlanguage CSim\r\n";   //hangs app
        sBuf+="reboot\r\n";                     // reboot to get CSim running again
        Charset charset = Charset.forName("UTF-8");
        ByteBuffer byteBuffer = charset.encode(sBuf); //charset.encode(sBuf);
        return byteBuffer.array();
    }

    void connectAndSend(final String sMacAddr, final byte[] sendBytes){

        BluetoothDevice device;
        try {
            // Get local Bluetooth adapter
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            device = mBluetoothAdapter.getRemoteDevice(sMacAddr);
            mainActivity.addLog("BT device found: " + device.getName());
            Log.d(TAG, "BT device found: " + device.getName());

        }catch (Exception e){
            Toast.makeText(m_context,"Invalid BT MAC address", Toast.LENGTH_LONG);
            mainActivity.addLog("Failed to get BT device by Mac address!");
            Log.d(TAG, "Failed to get BT device by Mac address!");
            device=null;
        }

        byte[] buf=new byte[200];
        int maxTry=10;
        int iTry=0;
        if (device != null) {
            addLog("connecting to " + sMacAddr);
            mainActivity.addLog("connecting to " + sMacAddr + " ...");
            try {
                addLog("createInsecureRfcommSocketToServiceRecord");
                mSocket = device.createInsecureRfcommSocketToServiceRecord(UUID_SPP);
                //tmp = device.createRfcommSocketToServiceRecord(UUID_SPP);
                mSocket.connect();
                mainActivity.addLog("BT socket connected");
                // Get the BluetoothSocket input and output streams
                try {
                    mInStream = mSocket.getInputStream();
                    mOutStream = mSocket.getOutputStream();
                    mOutStream.write(sendBytes);
                    mOutStream.flush();
                    Log.d(TAG, "write done");
                    mainActivity.addLog("CSim version requested");
                    String sIn = ""; int iCnt=0;
                    do {
                        int iAvailableBytes = mInStream.available();
                        Log.d(TAG, "mInStream.available()?"+ mInStream.available());
                        iCnt = mInStream.read(buf, 0, iAvailableBytes);
                        if(iCnt>0){
                            String s = new String(buf, 0, iCnt);
                            Log.d(TAG, "received: " + s);
                            sIn += s;
                            if (sIn.endsWith("\r\n")) {
                                Log.d(TAG, "receive complete: "+ sIn);
                                mainActivity.addLog("Received: "+ sIn);
                                break;
                            }
                        }
                        iTry++;
                        Thread.sleep(1000);
                    }while(iTry<maxTry || iCnt>0);
                    mInStream.close();
                    mOutStream.close();
                    mSocket.close();
                    mainActivity.addLog("BT socket closed");

                } catch (IOException e) {
                    Log.e(TAG, "temp sockets not created", e);
                    mainActivity.addLog("Failed to connect to BT socket!");
                }

            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
                mainActivity.addLog("socket create failed!");
            } catch (InterruptedException e) {
                Log.e(TAG, "create() failed", e);
                mainActivity.addLog("socket stream interrupted!");
            }
            // This is a blocking call and will only return on a
            // successful connection or an exception

        } else {
            addLog("unknown remote device!");
            mainActivity.addLog("unknown remote device!");
        }
        mainActivity.addLog("ENABLE button");

    }
    void addLog(String s){
        Log.d(TAG, s);
    }


}
