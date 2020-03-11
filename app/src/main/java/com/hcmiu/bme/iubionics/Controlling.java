package com.hcmiu.bme.iubionics;
import androidx.appcompat.app.AppCompatActivity;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;


public class Controlling extends AppCompatActivity {
    final static String demo = "A0";
    final static String muscle = "A2";
    final static String emergencyStop="A5";
    final static String motorSwitch="A3";
    final static String fist="G0";
    final static String palm="G1";
    final static String thumbUp="G2";
    final static String point="G3";
    final static String pinch="G4";
    final static String tripod="G5";

    ImageButton btnFist,btnPalm,btnPinch,btnPoint,btnThumbUp;
    Button btnDemo,btnMuscleControl,btnEStop;
    private BluetoothSocket mBTSocket;
    private int mMaxChars = 50000;
    private BluetoothDevice mDevice;
    private ReadInput mReadThread = null;
    private UUID mDeviceUUID;
    private Button mBtnDisconnect;
    private boolean mIsUserInitiatedDisconnect = false;
    private boolean mIsBluetoothConnected = false;
    private static final String TAG = "iuBionics-Controlling";
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controlling);

        Intent intent = getIntent();
        Bundle b = intent.getExtras();
        mDevice = b.getParcelable(MainActivity.DEVICE_EXTRA);
        mDeviceUUID = UUID.fromString(b.getString(MainActivity.DEVICE_UUID));
        mMaxChars = b.getInt(MainActivity.BUFFER_SIZE);
        Log.d(TAG, "Ready");
        //
        //Button event
        { //event
            btnFist = (ImageButton) findViewById(R.id.fist);
            btnPalm = (ImageButton) findViewById(R.id.palm);
            btnPoint = (ImageButton) findViewById(R.id.point);
            btnThumbUp = (ImageButton) findViewById(R.id.thumbup);
            btnPinch = (ImageButton) findViewById(R.id.pinch);
            btnEStop=(Button)findViewById(R.id.btnStop);
            btnDemo=(Button) findViewById(R.id.btnDemo);
            btnMuscleControl=(Button) findViewById(R.id.btnMuscleControl);
        btnFist.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
               sendCmd(fist);
            }
        });
        btnPalm.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
sendCmd(palm);
            }
        });
        btnPoint.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
              sendCmd(point);
            }
        });
        btnThumbUp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
               sendCmd(thumbUp);
            }
        });
        btnPinch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
               sendCmd(pinch);
            }
        });
        btnDemo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCmd(demo);
            }
        });
        btnMuscleControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCmd(muscle);
            }
        });
        btnEStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
sendCmd(emergencyStop);
            }
        });
        findViewById(R.id.btnMotor).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCmd(motorSwitch);
            }
        });
        findViewById(R.id.btnTripod).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCmd(tripod);
            }
        });

        findViewById(R.id.btnSend).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editText=findViewById(R.id.textSerial);
                    sendCmd(editText.getText().toString());
            }
        });
    }

    }
private void sendCmd(String s){
    try {
        s=s+'\n';
        mBTSocket.getOutputStream().write(s.getBytes());
    } catch (IOException e) {
        e.printStackTrace();
    }
}
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        if (mBTSocket != null && mIsBluetoothConnected) {
            new DisConnectBT().execute();
        }
        Log.d(TAG, "Paused");
        super.onPause();
    }

    @Override
    protected void onResume() {
        if ((mBTSocket == null) || !mIsBluetoothConnected) {
            new ConnectBT().execute();
        }
        super.onResume();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "Stopped");
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
// TODO Auto-generated method stub
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }

    private class ReadInput implements Runnable {

        private boolean bStop = false;
        Handler handler = new Handler();
        private Thread t;

        public ReadInput() {
            t = new Thread(this, "Input Thread");
            t.start();


        }

        public boolean isRunning() {
            return t.isAlive();
        }

        @Override
        public void run() {
            InputStream inputStream;

            try {
                inputStream = mBTSocket.getInputStream();
                while (!bStop) {
                    byte[] buffer = new byte[256];
                    if (inputStream.available() > 0) {
                        inputStream.read(buffer);
                        int i = 0;
                        /*
                         * This is needed because new String(buffer) is taking the entire buffer i.e. 256 chars on Android 2.3.4 http://stackoverflow.com/a/8843462/1287554
                         */
                        for (i = 0; i < buffer.length && buffer[i] != 0; i++) {
                        }
                        final String strInput = new String(buffer, 0, i);
                        handler.post(new Runnable() {
                                                        public void run() {
                                                            EditText editText=findViewById(R.id.textBluetoothReceived);
                                                            editText.append(strInput);
                                                        }
                                                    });
                                /*
                                 * If checked then receive text, better design would probably be to stop thread if unchecked and free resources, but this is a quick fix
                                 */
                    }
                    Thread.sleep(500);
                }
            } catch (IOException e) {
// TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InterruptedException e) {
// TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

        public void stop() {
            bStop = true;
        }

    }

    private class DisConnectBT extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Void doInBackground(Void... params) {//cant inderstand these dotss

            if (mReadThread != null) {
                mReadThread.stop();
                while (mReadThread.isRunning())
                    ; // Wait until it stops
                mReadThread = null;

            }

            try {
                mBTSocket.close();
            } catch (IOException e) {
// TODO Auto-generated catch block
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            mIsBluetoothConnected = false;
            if (mIsUserInitiatedDisconnect) {
                finish();
            }
        }

    }

    private class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean mConnectSuccessful = true;

        @Override
        protected void onPreExecute() {

            progressDialog = ProgressDialog.show(Controlling.this, "Hold on", "Connecting");// http://stackoverflow.com/a/11130220/1287554

        }

        @Override
        protected Void doInBackground(Void... devices) {

            try {
                if (mBTSocket == null || !mIsBluetoothConnected) {
                    mBTSocket = mDevice.createInsecureRfcommSocketToServiceRecord(mDeviceUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    mBTSocket.connect();
                }
            } catch (IOException e) {
// Unable to connect to device`
                // e.printStackTrace();
                mConnectSuccessful = false;


            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            if (!mConnectSuccessful) {
                Toast.makeText(getApplicationContext(), "Could not connect to device.Please turn on your Hardware", Toast.LENGTH_LONG).show();
                finish();
            } else {
                msg("Connected to device");
                mIsBluetoothConnected = true;
                mReadThread = new ReadInput(); // Kick off input reader
            }

            progressDialog.dismiss();
        }

    }
}

