/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.sunshineserver;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ServerControl extends AppCompatActivity implements View.OnClickListener, TextWatcher {
    static private final String LOG_TAG = ServerControl.class.getSimpleName();
    SharedPreferences mPreferences;
    Button mStopServerButton;
    Button mClearLogButton;
    TextView mServerStarted;
    TextView mLog;
    TextView mError;
    File mLogFile;
    FileObserver mLogFileObserver;
    ScrollView mLogScroller;
    static final private int[] RADIO_GROUP = new int[] { R.id.radioReturnError, R.id.radioReturnRandom, R.id.radioReturnStatic };

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        String errorString = s.toString();
        if ( errorString.length() > 0) {
            try {
                mPreferences.edit().putInt(WeatherCommandHandler.PREF_SERVER_ERROR, Integer.parseInt(errorString)).apply();
            } catch (NumberFormatException  e) {
                e.printStackTrace();
            }
        }
    }

    class ReaderTask extends AsyncTask<File, Void, String>
    {
        @Override
        protected String doInBackground(File... files) {
            String fileContents = "";
            File f = files[0];
            try {
                BufferedReader br = new BufferedReader(new FileReader(mLogFile));
                StringBuilder sb = new StringBuilder((int)f.length());
                // read in the contents of the file
                String line;
                String lineSeparator = System.getProperty("line.separator");
                while (null != (line = br.readLine())) {
                    sb.append(line);
                    sb.append(lineSeparator);
                }
                fileContents = sb.toString();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return fileContents;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            CharSequence text = mLog.getText();
            if ( s.length() > text.length() ) {
                mLog.append(s.substring(text.length()));
            } else {
                mLog.setText(s);
            }
            mLogScroller.fullScroll(View.FOCUS_DOWN);
        }
    };

    private void uncheckRadioButton(int id) {
        ((RadioButton)findViewById(id)).setChecked(false);
    }

    private void checkRadioButton(int id) {
        for ( int i = 0; i < RADIO_GROUP.length; i++ ) {
            int curId = RADIO_GROUP[i];
            if ( curId != id ) {
                uncheckRadioButton(curId);
                if ( curId == R.id.radioReturnError ) {
                    mError.setEnabled(false);
                }
            } else {
                if ( curId == R.id.radioReturnError ) {
                    mError.setEnabled(true);
                }
            }
        }
    }

    public void onRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();
        int id = view.getId();

        if ( checked ) {
            checkRadioButton(id);
            int serverSetting;
            switch(id) {
                case R.id.radioReturnError:
                    serverSetting = WeatherCommandHandler.SERVER_MODE_ERROR;
                    break;
                case R.id.radioReturnRandom:
                    serverSetting = WeatherCommandHandler.SERVER_MODE_RANDOM;
                    break;
                default:
                    serverSetting = WeatherCommandHandler.SERVER_MODE_STATIC;
            }
            mPreferences.edit().putInt(WeatherCommandHandler.PREF_SERVER_MODE, serverSetting).apply();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startService();
        setContentView(R.layout.activity_server_control);
        mPreferences = getSharedPreferences(WeatherCommandHandler.SHARED_PREFERENCES, MODE_PRIVATE);
        mStopServerButton = (Button)findViewById(R.id.stopServerButton);
        mStopServerButton.setOnClickListener(this);
        mClearLogButton = (Button)findViewById(R.id.clearLogButton);
        mClearLogButton.setOnClickListener(this);
        mServerStarted = (TextView)findViewById(R.id.webServerStatus);
        mLog = (TextView)findViewById(R.id.logView);
        mLogScroller = (ScrollView)findViewById(R.id.logViewScroller);
        mError = (TextView)findViewById(R.id.error);
        mLogFile  =  WeatherCommandHandler.getLogFile(this);
        mLogFileObserver = new FileObserver(mLogFile.getAbsolutePath(), FileObserver.MODIFY) {
            @Override
            public void onEvent(int event, String path) {
                new ReaderTask().execute(mLogFile);
            }
        };
        new ReaderTask().execute(mLogFile);
        mLogFileObserver.startWatching();
        int serverModeSetting = mPreferences.getInt(WeatherCommandHandler.PREF_SERVER_MODE, WeatherCommandHandler.SERVER_MODE_STATIC);
        int serverErrorSetting = mPreferences.getInt(WeatherCommandHandler.PREF_SERVER_MODE, 404);

        mError.setText(Integer.toString(serverErrorSetting));
        mError.addTextChangedListener(this);

        int defaultId;

        switch ( serverModeSetting ) {
            case WeatherCommandHandler.SERVER_MODE_RANDOM:
                defaultId = R.id.radioReturnRandom;
                break;
            case WeatherCommandHandler.SERVER_MODE_ERROR:
                defaultId = R.id.radioReturnError;
                break;
            default:
                defaultId = R.id.radioReturnStatic;
        }
        ((RadioButton)findViewById(defaultId)).setChecked(true);
        checkRadioButton(defaultId);
    }

    private void startService() {
        Intent webServerService = new Intent(this, WeatherService.class);
        // this will extend the lifecycle of the service beyond the activity
        startService(webServerService);
        if ( mIWeatherService == null ) {
            bindService(webServerService, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void setServerState(boolean isRunning) {
        if ( isRunning ) {
            mStopServerButton.setText(R.string.btn_stop_server);
            mServerStarted.setText(R.string.status_weather_server_started);
        } else {
            mStopServerButton.setText(R.string.btn_start_server);
            mServerStarted.setText(R.string.status_weather_server_stopped);
        }
    }

    @Override
    public void onClick(View v) {
        if ( v == mStopServerButton ) {
            try {
                if ( mIWeatherService != null ) {
                    boolean isRunning = mIWeatherService.isServerRunning();
                    if (isRunning) {
                        mIWeatherService.stopServer();
                    } else {
                        startService();
                        mIWeatherService.startServer();
                    }
                    setServerState(!isRunning);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else if ( v == mClearLogButton ) {
            File f = WeatherCommandHandler.getLogFile(this);
            new AsyncTask<File,Void,Void>() {
                @Override
                protected Void doInBackground(File... params) {
                    File f = params[0];
                    try {
                        // opens the file with append == false, clearing it
                        FileWriter fw = new FileWriter(f, false);
                        fw.flush();
                        fw.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            }.execute(f);
        }
    }

    IWeatherService mIWeatherService;
    private ServiceConnection mConnection = new ServiceConnection() {
        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            // Following the example above for an AIDL interface,
            // this gets an instance of the IRemoteInterface, which we can use to call on the service
            mIWeatherService = IWeatherService.Stub.asInterface(service);
            try {
                setServerState( mIWeatherService.isServerRunning());
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.e(LOG_TAG, "Service has unexpectedly disconnected");
            mIWeatherService = null;
        }
    };
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mConnection);
        mStopServerButton.setOnClickListener(null);
        mLogFileObserver.stopWatching();
    }
}
