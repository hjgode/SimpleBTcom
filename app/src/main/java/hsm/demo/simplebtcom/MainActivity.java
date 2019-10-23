package hsm.demo.simplebtcom;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    Context m_context =this;
    String TAG="BTComDemo";
    static TextView logText;
    private static final int REQUEST_WRITE = 112;
    private static final int REQUEST_BTADMIN = 113;
    private static final int REQUEST_BT = 114;
    private static final int REQUEST_LOCATION = 115;

    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logText=findViewById(R.id.logText);
        logText.setMovementMethod(new ScrollingMovementMethod());

        final EditText editText=findViewById(R.id.editText);
        button=findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String sMac = editText.getText().toString();

                final bt_send btSend=new bt_send(MainActivity.this , m_context, sMac);
                Thread myThread = new Thread(btSend, "BTSend");
                myThread.start();
//                btSend.run(); //blocks!
            }
        });
    }

    public void addLog(final String sMsg){
        if(logText!=null){
            runOnUiThread(new Thread(new Runnable() {
                @Override
                public void run() {
                    if(sMsg=="DISABLE button" || sMsg=="ENABLE button"){
                        if(sMsg.startsWith("DISABLE"))
                            button.setEnabled(false);
                        else
                            button.setEnabled(true);
                    }else {
                        logText.append("\r\n" + sMsg);
                    }
                }
            }));
        }
    }
    void checkPermissions(){
        if (    m_context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                m_context.checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                m_context.checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                m_context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            requestPermissions(new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_WRITE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_WRITE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Permission granted: WRITE_EXTERNAL_STORAGE");
                    //do here

                } else {
                    Toast.makeText(m_context, "The app was not allowed to write in your storage", Toast.LENGTH_LONG).show();
                }
            }
            case REQUEST_BT: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //do here
                    Log.i(TAG, "Permission granted: BLUETOOTH");
                } else {
                    Toast.makeText(m_context, "The app was not allowed to use Bluetooth", Toast.LENGTH_LONG).show();
                }
            }
            case REQUEST_BTADMIN: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //do here
                    Log.i(TAG, "Permission granted: BLUETOOTH_ADMIN");
                } else {
                    Toast.makeText(m_context, "The app was not allowed to manage Bluetooth", Toast.LENGTH_LONG).show();
                }
            }
            case REQUEST_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //do here
                    Log.i(TAG, "Permission granted: ACCESS_COARSE_LOCATION");
                } else {
                    Toast.makeText(m_context, "The app was not allowed to use Coarse Location", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

}
