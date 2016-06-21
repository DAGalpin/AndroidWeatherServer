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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import org.apache.http.HttpException;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;

import android.content.Context;

public class HttpServer {

    public static boolean sRunning = false;
    public static int sServerPort = 8088;
    private static Thread sServerThread;

    private static final String DAILY_WEATHER_PATTERN = "/data/2.5/forecast/daily";

    private Context mContext = null;

    private BasicHttpProcessor mHttpProc = null;
    private BasicHttpContext mHttpContext = null;
    private HttpService mHttpService = null;
    private HttpRequestHandlerRegistry mRequestHandlerRegistry = null;

    public HttpServer(Context context) {
        mContext = context;

        mHttpProc = new BasicHttpProcessor();
        mHttpContext = new BasicHttpContext();

        mHttpProc.addInterceptor(new ResponseDate());
        mHttpProc.addInterceptor(new ResponseServer());
        mHttpProc.addInterceptor(new ResponseContent());
        mHttpProc.addInterceptor(new ResponseConnControl());

        mHttpService = new HttpService(mHttpProc,
                new DefaultConnectionReuseStrategy(), new DefaultHttpResponseFactory());

        mRequestHandlerRegistry = new HttpRequestHandlerRegistry();

        mRequestHandlerRegistry.register(DAILY_WEATHER_PATTERN, new WeatherCommandHandler(context));

        mHttpService.setHandlerResolver(mRequestHandlerRegistry);
    }

    private ServerSocket serverSocket;

    public void runServer() {
        sServerThread = new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    serverSocket = new ServerSocket(sServerPort);
                    serverSocket.setReuseAddress(true);
                    while (sRunning) {
                        try {
                            final Socket socket = serverSocket.accept();
                            DefaultHttpServerConnection serverConnection = new DefaultHttpServerConnection();
                            serverConnection.bind(socket, new BasicHttpParams());
                            mHttpService.handleRequest(serverConnection, mHttpContext);
                            serverConnection.shutdown();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (HttpException he) {
                            he.printStackTrace();
                        }
                    }
                    serverSocket.close();
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                sRunning = false;
            }
        };
        sServerThread.start();
    }

    public synchronized void startServer() {
        sRunning = true;
        runServer();
    }

    public synchronized void stopServer() {
        sRunning = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        sServerThread.interrupt();
        try {
            sServerThread.join(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public synchronized boolean isRunning() {
        return sRunning;
    }
}
