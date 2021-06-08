package com.hcmiu.bme.iubionics;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.Legend.LegendForm;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.LimitLine.LimitLabelPosition;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IFillFormatter;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider;
import com.github.mikephil.charting.interfaces.datasets.IDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.Utils;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.List;


public class PlotingActivity extends AppCompatActivity implements OnSeekBarChangeListener, OnChartValueSelectedListener {
    private static final int PERMISSION_STORAGE = 0;
    static final int STATE_LISTENING = 1;
    static final int STATE_CONNECTING = 2;
    static final int STATE_CONNECTED = 3;
    static final int STATE_CONNECTION_FAILED = 4;
    static final int STATE_MESSAGE_RECEIVED = 5;
//    private LineChart chart;
   // private SeekBar seekBarX, seekBarY;
//    private TextView tvX, tvY;
    private BluetoothSocket mBTSocket;
    private int mMaxChars = 50000;
    private BluetoothDevice mDevice;
    private PlotingActivity.ReadInput mReadThread = null;
    private UUID mDeviceUUID;
    private Button mBtnDisconnect;
    private Button  btnZoomOut, btnExport;
    private boolean mIsUserInitiatedDisconnect = false;
    private boolean mIsBluetoothConnected = false;
    private static final String TAG = "Plotting";
    private ProgressDialog progressDialog;
    public Handler handler;
    public StringBuilder recDataString = new StringBuilder();
    private LineChart[] charts = new LineChart[5];
    private String[] channelLabel = {"Channel 1", "Channel 2", "Channel 3", "Channel 4", "Channel 5"};

    public int count = 0;
    private float timestamp = 1;
    final int numberOfCharsInPack = 36;
    private ArrayList<Entry> values_c1 = new ArrayList<>();
    private ArrayList<Entry> values_c2 = new ArrayList<>();
    private ArrayList<Entry> values_c3 = new ArrayList<>();
    private ArrayList<Entry> values_c4 = new ArrayList<>();
    private ArrayList<Entry> values_c5 = new ArrayList<>();
    private List[] values = {values_c1, values_c2, values_c3, values_c4, values_c5};


    @SuppressLint("LongLogTag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_ploting);
        Intent intent = getIntent();
        Bundle b = intent.getExtras();
        mDevice = b.getParcelable(MainActivity.DEVICE_EXTRA);
        mDeviceUUID = UUID.fromString(b.getString(MainActivity.DEVICE_UUID));
        mMaxChars = b.getInt(MainActivity.BUFFER_SIZE);
        Log.d(TAG, "Ready");
        setTitle("Plotting");
//        tvX = findViewById(R.id.tvXMax);
//        tvY = findViewById(R.id.tvYMax);
        btnZoomOut = findViewById(R.id.btn_zoomout);
        btnExport = findViewById(R.id.btn_export);

        charts[0] = findViewById(R.id.chart1);
        charts[1] = findViewById(R.id.chart2);
        charts[2] = findViewById(R.id.chart3);
        charts[3] = findViewById(R.id.chart4);
        charts[4] = findViewById(R.id.chart5);

        for (int i = 0; i < 5; i++) {   // // Chart Style // //
//            charts[0] = findViewById(R.id.chart1);
            // background color
            charts[i].setBackgroundColor(Color.WHITE);
            // disable description text
            charts[i].getDescription().setEnabled(false);
            // enable touch gestures
            charts[i].setTouchEnabled(true);
            // set listeners
            charts[i].setOnChartValueSelectedListener(this);
            charts[i].setDrawGridBackground(false);

//            // create marker to display box when values are selected
//            MyMarkerView mv = new MyMarkerView(this, R.layout.custom_marker_view);
//
//            // Set the marker to the chart
//            mv.setChartView(chart);
//            chart.setMarker(mv);

            // enable scaling and dragging
            charts[i].setDragEnabled(true);
            charts[i].setScaleEnabled(true);
            // chart.setScaleXEnabled(true);
            // chart.setScaleYEnabled(true);

            // force pinch zoom along both axis
            charts[i].setPinchZoom(true);
            charts[i].setVisibleXRangeMaximum(10);

            XAxis xAxis;
            {   // // X-Axis Style // //
                xAxis = charts[i].getXAxis();
                xAxis.setDrawGridLines(false);
                xAxis.setAvoidFirstLastClipping(true);
                xAxis.setEnabled(false);
                xAxis.setDrawLabels(false);
                // vertical grid lines
//              xAxis.enableGridDashedLine(10f, 10f, 0f);
//              xAxis.setAxisMaximum(0);
//              xAxis.setPosition(XAxis.XAxisPosition.TOP);

            }

            YAxis yAxis;
            {   // // Y-Axis Style // //
                yAxis = charts[i].getAxisLeft();
                // disable dual axis (only use LEFT axis)
                charts[i].getAxisRight().setEnabled(false);
                yAxis.setDrawGridLines(false);
                // horizontal grid lines
//                yAxis.enableGridDashedLine(10f, 10f, 0f);
                // axis range
//                yAxis.setAxisMaximum(999999f);
                yAxis.setAxisMinimum(0f);
                yAxis.setDrawLabels(false);
                yAxis.setEnabled(false);
            }
            // add data
            //setData(0, 0);

            values[i].add(new Entry(0,0));
            setData(charts[i],values[i], i);
            // draw points over time
            //chart.animateX(1500);

            // get the legend (only possible after setting data)
            Legend l = charts[i].getLegend();
            // draw legend entries as lines
            l.setForm(LegendForm.LINE);
            l.setTextColor(Color.RED);
            l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
            l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
            l.setOrientation(Legend.LegendOrientation.VERTICAL);
            l.setDrawInside(true);
            l.setEnabled(false);
        }

        btnZoomOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List <Entry> allList;
                for (int i = 0; i < 5; i++) {
                    allList = values[i];
                    setData(charts[i], allList, i);
                }
            }
        });

        btnExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    ArrayList<String> dataToBeStored = new ArrayList<>();
                    dataToBeStored.add("Counts\tChannel 1\tChannel 2\tChannel 3\tChannel 4\tChannel 5");
                    int numberOfDatapoints = values_c1.size();
                    for ( int k = 0; k < numberOfDatapoints; k++)
                    {
/*                        Log.d("Value",entryPoint.toString());
                        Log.d("X",String.valueOf(entryPoint.getX()));
                        Log.d("Y",String.valueOf(entryPoint.getY()));*/
                        String storedString = (String.valueOf(values_c1.get(k).getX()) + "\t" + String.valueOf(values_c1.get(k).getY())
                                + "\t" + String.valueOf(values_c2.get(k).getY()) + "\t" + String.valueOf(values_c3.get(k).getY()) + "\t" + String.valueOf(values_c4.get(k).getY())
                                + "\t" + String.valueOf(values_c5.get(k).getY()));
//                        Log.d("Data to be stored", storedString);
                        dataToBeStored.add(storedString);
                    }

//                    final File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Bluetooth Plotting 2/");
                    int code = getApplicationContext().getPackageManager().checkPermission(
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            getApplicationContext().getPackageName());
                    if (code == PackageManager.PERMISSION_GRANTED) {
                        final File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Serial plotter");
                        if (!dir.exists()) {
                            if (!dir.mkdirs()) {
//                                Log.e("TAG", "could not create the directories");
                                Toast.makeText(getBaseContext(), "Storage directory CANNOT be created. Please allow storage permission in Settings/ Apps", Toast.LENGTH_LONG).show();
                            }
                        }

                        String storageDirectory = dir.toString();
//                        Log.d("Directory", storageDirectory);
                        final File myFile = new File(dir, "Output" + ".txt");
                        if (!myFile.exists()) {
                            myFile.createNewFile();
                        }

                        FileWriter writer = new FileWriter(myFile);
                        for (String str : dataToBeStored) {
                            writer.write(str + "\n");
                        }
                        writer.close();
                        Toast.makeText(getBaseContext(), "Data has been exported to: Internal storage/ Serial plotter/ Output.txt", Toast.LENGTH_LONG).show();
//                        Log.d("File Directory", myFile.getAbsolutePath());
                    }
                    else
                    {
//                        Log.d("Permission","NOT GRANTED");
                        Toast.makeText(getBaseContext(), "Storage directory CANNOT be created. Please allow storage permission in Settings/ Apps", Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        handler = new Handler(Looper.getMainLooper())
        {
            @Override
            public void handleMessage(Message msg)
            {
                switch (msg.what)
                {
                    case STATE_MESSAGE_RECEIVED:
                        String readMessage = (String) msg.obj;
                        recDataString.append(readMessage);
//                        Log.d("recDataString",String.valueOf(recDataString));
                        int endOfLineIndex = recDataString.indexOf("E");
//                        Log.d("endOf",String.valueOf(endOfLineIndex));
                        if (endOfLineIndex >= 0) { // Found the ending character E
                            int strLength = recDataString.length();
                            int idxFirstS = recDataString.indexOf("S"); // Find the first index of "S"
                            int idxLastE = recDataString.lastIndexOf("E"); // Find the last index of E
                            int numberDataPoint = (idxLastE - idxFirstS + 1)/numberOfCharsInPack;
                            int value;
                            for (int i = 0; i < numberDataPoint; i++)
                            {
                                int idxSplitStart = (i*numberOfCharsInPack) + idxFirstS + 1;
                                int idxSplitEnd = idxSplitStart + (numberOfCharsInPack - 2);
                                String data = recDataString.substring(idxSplitStart,idxSplitEnd);
//                                Log.d("Data substring",String.valueOf(recDataString));
                                if (!(data.contains("S")||data.contains("E")))
                                {
                                    try {
                                        String [] tokens = data.split(",");
                                        for (int j = 0; j < 5; j++) {
//                                            Log.d("Tokens", tokens[j]);
                                            value = Integer.parseInt(tokens[j]);
//                                            Log.d("Data", String.valueOf(value));
                                            values[j].add(new Entry(timestamp, value));
//                                            Log.d("Convert",values[j].toString());
                                        }
                                        timestamp++;
                                    } catch (Exception e) {
                                        Toast.makeText(getBaseContext(),"Invalid data format",Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                            if (idxFirstS == 0)
                            {
                                recDataString.delete(idxFirstS,idxLastE+1);
//                                Log.d("Data left",String.valueOf(recDataString)); // check if any chars left in the string buffer
                            }
                            else
                            {
                                recDataString.delete(0,idxLastE+1);
//                                Log.d("Data left",String.valueOf(recDataString)); // check if any chars left in the string buffer
                            }
                            for (int channel = 0; channel < 5; channel++) {
                                List<Entry> dataToUpdate;
                                if (values[channel].size() > 1000) {
                                    int idxEnd = values[channel].size();
                                    int idxStart = idxEnd - 1000;
                                    dataToUpdate = values[channel].subList(idxStart, idxEnd);
                                } else {
                                    dataToUpdate = values[channel];
                                }
                                setData(charts[channel], dataToUpdate, channel);
//                            values.removeAll(values);
                            }
                        }
                        break;
                }
            }
        };
    }

    private void setData(LineChart chart, List  <Entry> value, int channel) {
        //Log.d("Set data","start");
        //values.add(new Entry(x, y));
        LineDataSet set1;
        LineData chartdata = chart.getData();
        if (chartdata != null)// && chartdata.getDataSetCount() > 0)
        {
            set1 = (LineDataSet) chartdata.getDataSetByIndex(0);
            set1.setEntries(value);
//            Log.d("Dataset size",String.valueOf(set1.getEntryCount()));
            //chartdata.addDataSet(dataSet);
            /*XAxis xl = chart.getXAxis();
            xl.setDrawGridLines(true);
            xl.setGranularityEnabled(true);
            xl.setGranularity(2f);*/
 //           XAxis xAxis = chart.getXAxis();
 //           xAxis.resetAxisMaximum();
//            xAxis.setValueFormatter(new IndexAxisValueFormatter(timeStamp));
            set1.notifyDataSetChanged();
            chartdata.notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.setVisibleXRangeMaximum(2500);
            chart.moveViewToX(chartdata.getEntryCount());
            //chart.invalidate();
            //Log.d("Setdata","Finish");
        }
        else {
            // create a dataset and give it a type
            //set1 = new LineDataSet(values, "DataSet 1");
//            set1 = new LineDataSet(value,"Channel 1");
            set1 = new LineDataSet(value, channelLabel[channel]);
            set1.setDrawIcons(false);

            // draw dashed line
            //set1.enableDashedLine(10f, 5f, 0f);
            set1.setDrawCircles(false);
            set1.setDrawValues(false);
            // black lines and points
            set1.setColor(Color.RED);

            //set1.setCircleColor(Color.BLACK);

            // line thickness and point size
            set1.setLineWidth(2f);
            //set1.setCircleRadius(3f);

            // draw points as solid circles
            //set1.setDrawCircleHole(false);

            // customize legend entry
            set1.setFormLineWidth(2f);
            set1.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
            set1.setFormSize(15f);

            // text size of values
            //set1.setValueTextSize(3f);

            // draw selection line as dashed
            set1.enableDashedHighlightLine(10f, 5f, 0f);
            set1.setHighlightLineWidth(3f);
            // set the filled area
            set1.setDrawFilled(false);
/*            set1.setFillFormatter(new IFillFormatter() {
                @Override
                public float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider) {
                    return chart.getAxisLeft().getAxisMinimum();
                    }
                });*/
            // set color of filled area

            set1.setFillColor(Color.BLACK);
            ArrayList<ILineDataSet> dataSets = new ArrayList<>();
            dataSets.add(set1); // add the data sets

            // create a data object with the data sets
            LineData data = new LineData(dataSets);

            // set data
            chart.setData(data);
            Legend legend = chart.getLegend();
            legend.setTextSize(12f);
            //Log.d("update chart","finish");
        }
    }



    protected float getRandom(float range, float start) {
        return (float) (Math.random() * range) + start;
    }
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        //tvX.setText(String.valueOf(seekBarX.getProgress()));
        //tvY.setText(String.valueOf(seekBarY.getProgress()));

//        setData(seekBarX.getProgress(), seekBarY.getProgress());

        // redraw
        //chart.invalidate();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_STORAGE) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveToGallery();
            } else {
                Toast.makeText(getApplicationContext(), "Saving FAILED!", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }
    protected void requestStoragePermission(View view) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Snackbar.make(view, "Write permission is required to save image to gallery", Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ActivityCompat.requestPermissions(PlotingActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_STORAGE);
                        }
                    }).show();
        } else {
            Toast.makeText(getApplicationContext(), "Permission Required!", Toast.LENGTH_SHORT)
                    .show();
            ActivityCompat.requestPermissions(PlotingActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_STORAGE);
        }
    }

    protected void saveToGallery(Chart chart, String name) {
        if (chart.saveToGallery(name + "_" + System.currentTimeMillis(), 70))
            Toast.makeText(getApplicationContext(), "Saving SUCCESSFUL!",
                    Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(getApplicationContext(), "Saving FAILED!", Toast.LENGTH_SHORT)
                    .show();
    }
    protected void saveToGallery() {
        saveToGallery(charts[0], "Ploting");
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
            new PlotingActivity.DisConnectBT().execute();
        }
        Log.d(TAG, "Paused");
        super.onPause();
    }

    @Override
    protected void onResume() {
        if ((mBTSocket == null) || !mIsBluetoothConnected) {
            new PlotingActivity.ConnectBT().execute();
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

    @Override
    public void onValueSelected(Entry e, Highlight h) {

    }

    @Override
    public void onNothingSelected() {

    }

    private class ReadInput implements Runnable {

        private boolean bStop = false;
        //Handler handler = new Handler();
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
                    byte[] buffer = new byte[1024];
                    int bytes;
                    if (inputStream.available() > 0) {
                        bytes = inputStream.read(buffer);
                        //int i = 0;
                        /*
                         * This is needed because new String(buffer) is taking the entire buffer i.e. 256 chars on Android 2.3.4 http://stackoverflow.com/a/8843462/1287554
                         */
                        //for (i = 0; i < buffer.length && buffer[i] != 0; i++) {
                        //}
                        final String strInput = new String(buffer, 0, bytes);
//                        Log.d("Raw",strInput);
                         //SxxxxE contains 6 characters, idexes from 0 - 5
                        /*handler.post(new Runnable() {
                            public void run() {
//                                EditText editText=findViewById(R.id.textBluetoothReceived);
//                                editText.append(strInput)
                               // Log.d("Data =", strInput);
//                                int startidx = 1;
//                                int endidx = 5;
                                if(starttime == 0)
                                {
                                    starttime = SystemClock.elapsedRealtime();
                                    timestamp = 0;
                                    //Log.d("starttime= ",String.valueOf(starttime));
                                }
                                else
                                {
                                    timestamp = (SystemClock.elapsedRealtime() - starttime)/1000;
                                    //Log.d("timestart= ",String.valueOf(starttime));
                                    //Log.d("timestamp= ",String.valueOf(timestamp));
                                }
                                //timestamp = count;
                                if (strInput.contains("S") && strInput.contains("E")) {
                                    //Log.d("Data", "Valid");
                                    int strLength = strInput.length();
                                    int dataNumber = strLength/6;
                                    //Log.d("Data length", String.valueOf(strLength));
                                    ArrayList <String> data = new ArrayList<>();
                                    Float value;
                                    switch (dataNumber)
                                    {
                                        case 1:
                                            data.add(strInput.substring(1, 5));
                                            //Log.d("Data", String.valueOf(data));
                                            value = Float.parseFloat(data.get(0));
                                            tvY.setText(String.valueOf(value));
                                            tvX.setText(String.valueOf(timestamp));
                                            //setData(timestamp,value);
                                            values.add(new Entry(timestamp,value));
                                            count++;
//                                            timestamp++;
                                            break;
                                        case 2:
                                            data.add(strInput.substring(1, 5));
                                            data.add(strInput.substring(7, 11));
                                            for (int i = 0; i < dataNumber; i++) {
                                                //Log.d("Data", String.valueOf(data));
                                                value = Float.parseFloat(data.get(i));
                                                tvY.setText(String.valueOf(value));
                                                tvX.setText(String.valueOf(timestamp));
                                                //setData(timestamp,value);
                                                values.add(new Entry(timestamp, value));
                                                //timestamp = (float) (timestamp + 0.01);
                                                count++;
//                                                timestamp++;
                                            }
                                            break;
                                        case 3:
                                            data.add(strInput.substring(1, 5));
                                            data.add(strInput.substring(7, 11));
                                            data.add(strInput.substring(13, 17));
                                            for (int i = 0; i < dataNumber; i++) {
                                                value = Float.parseFloat(data.get(i));
                                                //Log.d("Data", String.valueOf(value));
                                                tvY.setText(String.valueOf(value));
                                                tvX.setText(String.valueOf(timestamp));
                                                //setData(timestamp,value);
                                                values.add(new Entry(timestamp, value));
                                                //timestamp = (float) (timestamp + 0.01);
                                                count++;
//                                                timestamp++;
                                            }
                                            break;
                                        case 4:
                                            data.add(strInput.substring(1, 5));
                                            data.add(strInput.substring(7, 11));
                                            data.add(strInput.substring(13, 17));
                                            data.add(strInput.substring(19, 23));
                                            for (int i = 0; i < dataNumber; i++) {
                                                value = Float.parseFloat(data.get(i));
                                                //Log.d("Data", String.valueOf(value));
                                                tvY.setText(String.valueOf(value));
                                                tvX.setText(String.valueOf(timestamp));
                                                //setData(timestamp,value);
                                                values.add(new Entry(timestamp, value));
                                                //timestamp = (float) (timestamp + 0.01);
                                                count++;
//                                                timestamp++;
                                            }
                                            break;
                                        case 5:
                                            data.add(strInput.substring(1, 5));
                                            data.add(strInput.substring(7, 11));
                                            data.add(strInput.substring(13, 17));
                                            data.add(strInput.substring(19, 23));
                                            data.add(strInput.substring(25, 29));
                                            for (int i = 0; i < dataNumber; i++) {
                                                value = Float.parseFloat(data.get(i));
                                                //Log.d("Data", String.valueOf(value));
                                                tvY.setText(String.valueOf(value));
                                                tvX.setText(String.valueOf(timestamp));
                                                //setData(timestamp,value);
                                                values.add(new Entry(timestamp, value));
                                                //timestamp = (float) (timestamp + 0.01);
                                                count++;
//                                                timestamp++;
                                            }
                                            break;
                                        case 6:
                                            data.add(strInput.substring(1, 5));
                                            data.add(strInput.substring(7, 11));
                                            data.add(strInput.substring(13, 17));
                                            data.add(strInput.substring(19, 23));
                                            data.add(strInput.substring(25, 29));
                                            data.add(strInput.substring(31, 35));
                                            for (int i = 0; i < dataNumber; i++) {
                                                value = Float.parseFloat(data.get(i));
                                               // Log.d("Data", String.valueOf(value));
                                                tvY.setText(String.valueOf(value));
                                                tvX.setText(String.valueOf(timestamp));
                                                //setData(timestamp,value);
                                                values.add(new Entry(timestamp, value));
                                                //timestamp = (float) (timestamp + 0.01);
                                                count++;
//                                                timestamp++;
                                            }
                                            break;
                                    }
                                    //setData(values);
                                    //Log.d("Start", "Splitting");

                                    //int value = Integer.parseInt(data);
                                    //Log.d("Value =", String.valueOf(value));

                                    if(count >= 500)
                                    {
                                        setData(values);
                                        count = 0;
                                        //Log.d("Chart","Update");
                                    }
                                    //Log.d("Y = ",String.valueOf(value));
                                    //Log.d("X = ",String.valueOf(timestamp));
                                }
                                 else
                                    {
                                        Log.d("Data","INvalid");
                                    }
                                //TODO
                            }
                        });*/
                        /*
                         * If checked then receive text, better design would probably be to stop thread if unchecked and free resources, but this is a quick fix
                         */
                        handler.obtainMessage(STATE_MESSAGE_RECEIVED,bytes,-1,strInput).sendToTarget();
                    }
                    //Thread.sleep(1);
                }
            } catch (IOException e) {
// TODO Auto-generated catch block
                e.printStackTrace();
            } //catch (InterruptedException e) {
// TODO Auto-generated catch block
//                e.printStackTrace();
 //           }

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

            progressDialog = ProgressDialog.show(PlotingActivity.this, "Hold on", "Connecting");// http://stackoverflow.com/a/11130220/1287554

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
                mReadThread = new PlotingActivity.ReadInput(); // Kick off input reader
             //   mReadThread.run();
            }

            progressDialog.dismiss();
        }
    }

}