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

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

public class WeatherService extends Service {
    static private final int ONGOING_NOTIFICATION_ID = 1;

    static private final String LOG_TAG = WeatherService.class.getSimpleName();
    public WeatherService() {
    }

    private HttpServer mServer = null;
    @Override
    public void onCreate() {
        Log.i(LOG_TAG, "Creating and starting WeatherService");
        super.onCreate();

        mServer = new HttpServer(this);
        startServerImpl();
    }

    private void startServerImpl() {
        if ( !mServer.isRunning() ) {
            mServer.startServer();
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
            notificationBuilder.setSmallIcon(R.mipmap.ic_launcher);
            notificationBuilder.setTicker(getText(R.string.sunshine_server));
            notificationBuilder.setContentTitle(getText(R.string.sunshine_server));
            notificationBuilder.setWhen(System.currentTimeMillis());

            Intent notificationIntent = new Intent(this, ServerControl.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
            notificationBuilder.setContentIntent(pendingIntent);
            startForeground(ONGOING_NOTIFICATION_ID, notificationBuilder.build());
        }
    }

    private void stopServerImpl() {
        if ( mServer.isRunning() ) {
            mServer.stopServer();
            stopForeground(true);
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        Log.i(LOG_TAG, "Destroying WeatherService");
        stopServerImpl();
        super.onDestroy();
    }

    private final IWeatherService.Stub mBinder = new IWeatherService.Stub() {
        @Override
        public boolean isServerRunning() throws RemoteException {
            return mServer.isRunning();
        }

        @Override
        public void startServer() throws RemoteException {
            startServerImpl();
        }

        @Override
        public void stopServer() throws RemoteException {
            stopServerImpl();
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
