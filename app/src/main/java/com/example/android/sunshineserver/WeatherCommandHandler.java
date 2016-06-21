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

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.format.DateUtils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class WeatherCommandHandler implements HttpRequestHandler {
    public static final String SHARED_PREFERENCES = "WeatherControl";
    public static final String LOGFILE = "WeatherLog";
    public static final String PREF_SERVER_MODE = "ServerMode";
    public static final String PREF_SERVER_ERROR = "ServerError";

    public static final int SERVER_MODE_RANDOM = 0;
    public static final int SERVER_MODE_STATIC = 1;
    public static final int SERVER_MODE_ERROR = 2;

    public static final long SECOND_IN_MILLIS = 1000;
    public static final long MINUTE_IN_MILLIS = SECOND_IN_MILLIS * 60;
    public static final long HOUR_IN_MILLIS = MINUTE_IN_MILLIS * 60;
    public static final long DAY_IN_MILLIS = HOUR_IN_MILLIS * 24;

    private Context context = null;

    public WeatherCommandHandler(Context context) {
        this.context = context;
    }

    private static long getWeatherDataStartDate() {
        long date = System.currentTimeMillis() - DAY_IN_MILLIS;
        return date;
    }

    private static final int sWeatherConditions[] = { 200, 201, 202, 203, 204, 205, 206, 207, 209, 210, 211, 212, 213,
            214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, // storm
            300, 301, 302, 303, 304, 305, 306, 307, 308, 309, 310, 311, 312, 313, 314, 315, 316, 317, 318, 319, 320,
            321, // light rain
            500, 501, 502, 503, 504, // rain
            511, // snow
            520, // rain
            531, // ragged shower
            600, 601, 602, 611, 612, 615, 616, 620, 621, 622, // snow
            701, 711, 721, 731, 741, 751, // fog
            761, 762, 771, 781, // storm
            800, // clear
            801, // light clouds
            802, 803, 804, // clouds
            900, 901, 902, 903, 904, 905, 906, 951, 952, 953, 954, 955, 956, 957, 958, 959, 960, 961, 962 };

    public static String getStringForWeatherCondition(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        String stringRet;
        if (weatherId >= 200 && weatherId <= 232) {
            stringRet = "Storm";
        } else if (weatherId >= 300 && weatherId <= 321) {
            stringRet = "Light Rain";
        } else switch (weatherId) {
            case 500:
                stringRet = "Light Rain";
                break;

            case 501:
                stringRet = "Moderate Rain";
                break;

            case 502:
                stringRet = "Heavy Rain";
                break;

            case 503:
                stringRet = "Intense Rain";
                break;

            case 504:
                stringRet = "Extreme Rain";
                break;

            case 511:
                stringRet = "Freezing Rain";
                break;

            case 520:
                stringRet = "Light Shower";
                break;

            case 521:
                stringRet = "Shower";
                break;

            case 522:
                stringRet = "Heavy Shower";
                break;

            case 531:
                stringRet = "Ragged Shower";
                break;

            case 600:
                stringRet = "Light Snow";
                break;

            case 601:
                stringRet = "Snow";
                break;

            case 602:
                stringRet = "Heavy Snow";
                break;

            case 611:
                stringRet = "Sleet";
                break;

            case 612:
                stringRet = "Shower Sleet";
                break;

            case 615:
                stringRet = "Rain and Snow";
                break;
            // light rain and snow
            case 616:
                stringRet = "Rain and Snow";
                break;

            case 620:
                stringRet = "Shower Snow";
                break;
            // light shower snow
            case 621:
                stringRet = "Shower Snow";
                break;

            case 622:
                stringRet = "Shower Snow";
                break;
            // heavy shower snow
            case 701:
                stringRet = "Mist";
                break;

            case 711:
                stringRet = "Smoke";
                break;

            case 721:
                stringRet = "Haze";
                break;

            case 731:
                stringRet = "Sand, Dust";
                break;

            case 741:
                stringRet = "Fog";
                break;

            case 751:
                stringRet = "Sand";
                break;

            case 761:
                stringRet = "Dust";
                break;

            case 762:
                stringRet = "Volcanic Ash";
                break;

            case 771:
                stringRet = "Squalls";
                break;

            case 781:
                stringRet = "Tornado";
                break;

            case 800:
                stringRet = "Clear";
                break;

            case 801:
                stringRet = "Mostly Clear";
                break;

            case 802:
                stringRet = "Scattered Clouds";
                break;

            case 803:
                stringRet = "Broken Clouds";
                break;

            case 804:
                stringRet = "Overcast Clouds";
                break;

            case 900:
                stringRet = "Tornado";
                break;

            case 901:
                stringRet = "Tropical Storm";
                break;

            case 902:
                stringRet = "Hurricane";
                break;

            case 903:
                stringRet = "Cold";
                break;

            case 904:
                stringRet = "Hot";
                break;

            case 905:
                stringRet = "Windy";
                break;

            case 906:
                stringRet = "Hail";
                break;

            case 951:
                stringRet = "Calm";
                break;

            case 952:
                stringRet = "Light Breeze";
                break;

            case 953:
                stringRet = "Gentle Breeze";
                break;

            case 954:
                stringRet = "Breeze";
                break;

            case 955:
                stringRet = "Fresh Breeze";
                break;

            case 956:
                stringRet = "Strong Breeze";
                break;

            case 957:
                stringRet = "High Wind";
                break;

            case 958:
                stringRet = "Gale";
                break;

            case 959:
                stringRet = "Severe Gale";
                break;

            case 960:
                stringRet = "Storm";
                break;

            case 961:
                stringRet = "Violent Storm";
                break;

            case 962:
                stringRet = "Hurricane";
                break;

            default:
                stringRet = "Unknown";
        }
        return stringRet;
    }

    String generateDailyWeather(long time) {
        double lowTempMin, highTempMax, humidityMin, humidityMax, windSpeedMin, windSpeedMax;
        int weatherIndex = (int)(Math.random() * sWeatherConditions.length * 2);
        int weatherId;
        if ( weatherIndex >= sWeatherConditions.length ) {
            weatherId = (int)(800.0f + Math.random() * 4);
        } else {
            weatherId = sWeatherConditions[weatherIndex];
        }
        if ((weatherId >= 200 && weatherId <= 504) ||
                (weatherId >= 520 && weatherId <= 531) ||
                (weatherId == 701) || (weatherId == 721) ||
                (weatherId == 741) || (weatherId == 781) ||
                (weatherId >= 960) || (weatherId <= 962))
        {
            lowTempMin = 2.0f; highTempMax = 33.0f;
            humidityMin = 40.0f; humidityMax = 100.0f;
        }
        else if ( (weatherId == 511) ||
                ( weatherId >= 600 && weatherId <= 622 )  ||
                ( weatherId == 906) )
        {
            lowTempMin = -23.333f; highTempMax = 5.0f;
            humidityMin = 10.0f; humidityMax = 30.0f;
        }
        else {
            lowTempMin = 4.0f; highTempMax = 37.0f;
            humidityMin = 10.0f; humidityMax = 30.0f;
        }
        double tempSpread = highTempMax - lowTempMin;
        double lowTemp = lowTempMin + Math.random() * tempSpread/2.0f;
        tempSpread = highTempMax - lowTemp;
        double highTemp = lowTemp + Math.random() * tempSpread;
        double pressure = 950 + Math.random() * 100;
        double humidity = humidityMin + Math.random() * (humidityMax-humidityMin);

        switch (weatherId) {
            case 781:
            case 900:
            case 902:
                windSpeedMax = 150.0;
                windSpeedMin = 117.6;  // hurricane
                break;
            case 901:
                windSpeedMax = 117.6;
                windSpeedMin = 102.9;
                break;
            case 800:
            case 801:
            case 802:
            case 803:
            case 804:
            case 951:
                windSpeedMin = 1.9f; // light air
                windSpeedMax = 6.4f;
                break;
            default:
                windSpeedMin = 39.9; // near gale
                windSpeedMax = 102.9; // violent storm
                break;
        }

        double windSpeed = windSpeedMin + Math.random() * (windSpeedMax-windSpeedMin);
        int direction = (int)(Math.random() * 360.0f);

        return String.format("{\"dt\":%d,\"temp\":{\"day\":29.49,\"min\":%.2f,\"max\":%.2f,\"night\":9.52,\"eve\":21.09,\"morn\":15.42},\"pressure\":%.2f,\"humidity\":%.0f,\"weather\":[{\"id\":%d,\"main\":\"%s\",\"description\":\"%s\",\"icon\":\"02d\"}],\"speed\":%.2f,\"deg\":%d,\"clouds\":20}",
                time, lowTemp, highTemp, pressure, humidity, weatherId, getStringForWeatherCondition(weatherId), getStringForWeatherCondition(weatherId), windSpeed, direction);
    }

    // static well-known weather output
    // each output has a well-known lowTemp, highTemp, pressure, humidity, weatherId, windSpeed, direction

    static private class StaticWeatherDay {
        private final double mLowTemp;
        private final double mHighTemp;
        private final double mPressure;
        private final double mHumidity;
        private final int mWeatherId;
        private final double mWindSpeed;
        private final int mDirection;
        StaticWeatherDay( double lowTemp, double highTemp, double pressure, double humidity, int weatherId, double windspeed, int direction ) {
            mLowTemp = lowTemp;
            mHighTemp = highTemp;
            mPressure = pressure;
            mHumidity = humidity;
            mWeatherId = weatherId;
            mWindSpeed = windspeed;
            mDirection = direction;
        }
    }

    /*
        Show a reasonable amount of different weather symbols and types.
     */
    static StaticWeatherDay[] staticWeather = {
        // Week 1
            new StaticWeatherDay( 13.32f, 18.27f, 996.68f, 96, 500, 1.2f, 0),
            new StaticWeatherDay( 12.66f, 17.34f, 996.12f, 97, 501, 4.8f, 45),
            new StaticWeatherDay( 12.07f, 16.48f, 995.70f, 90, 800, 8.2f, 90 ),

            new StaticWeatherDay( 11.53f, 12.34f, 1001.22f, 87, 802, 3.6f, 135 ),
            new StaticWeatherDay( 14.62f, 15.36f, 1001.51f, 88, 803, 4f, 180 ),

            new StaticWeatherDay( -2.0f, -1.1f, 997.54f, 78, 600, 2.6f, 225 ),
            new StaticWeatherDay( -1.5f, -1.0f, 996.55f, 80, 601, 3.8f, 270 ),

        // Week 2
            new StaticWeatherDay( -1.0f, 0.5f,  996.76f, 85, 602, 5.2f, 315 ),
            new StaticWeatherDay(  0.5f, 2.0f,  998.56f, 90, 611, 10.3f, 0 ),
            new StaticWeatherDay(  5.0f, 7.5f,  1000.21f, 80, 741, 1.5f, 45 ),
            new StaticWeatherDay(  10.21f, 12.35f,  1000.11f, 97, 960, 70.2f, 90 ),
            new StaticWeatherDay(  12.20f, 19.01f,  990.01f, 97, 960, 80.1f, 135 ),
            new StaticWeatherDay(  13.00f, 18.01f,  989.01f, 98, 901, 110.1f, 180 ),
            new StaticWeatherDay(  12.20f, 19.01f,  980.01f, 99, 902, 148.1f, 225 ),
    };

    String generateStaticWeather(long time) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < staticWeather.length; i++ ) {
            StaticWeatherDay sd = staticWeather[i];
            long dayTime = time + (i * DAY_IN_MILLIS);
            dayTime /= 1000;
            if ( i > 0 ) sb.append(",");
            sb.append(String.format("{\"dt\":%d,\"temp\":{\"day\":29.49,\"min\":%.2f,\"max\":%.2f,\"night\":9.52,\"eve\":21.09,\"morn\":15.42},\"pressure\":%.2f,\"humidity\":%.0f,\"weather\":[{\"id\":%d,\"main\":\"%s\",\"description\":\"%s\",\"icon\":\"02d\"}],\"speed\":%.2f,\"deg\":%d,\"clouds\":20}",
                    dayTime, sd.mLowTemp, sd.mHighTemp, sd.mPressure, sd.mHumidity, sd.mWeatherId, getStringForWeatherCondition(sd.mWeatherId), getStringForWeatherCondition(sd.mWeatherId), sd.mWindSpeed, sd.mDirection));
        }
        return sb.toString();
    }

    public static File getLogFile(Context c) {
        File f = new File(c.getFilesDir(), LOGFILE);
        if ( !f.exists() ) try {
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return f;
    }

    private String getMessageForErrorCode(int error) {
        switch(error) {
            case 401:
                return "Invalid API key. Please see http://openweathermap.org/faq#error401 for more info.";
            case 404:
                return "Location not found.";
            default:
                return "Error: " + error;
        }
    }

    private String getErrorResponse(int responseCode) {
        return new String().format("{\"cod\":%d, \"message\": \"%s\"}", responseCode, getMessageForErrorCode(responseCode));
    }

    @Override
    public void handle(HttpRequest request, final HttpResponse response,
                       HttpContext httpContext) throws HttpException, IOException {
        SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        final int mode = preferences.getInt(PREF_SERVER_MODE, SERVER_MODE_STATIC);
        final long time = System.currentTimeMillis();
        Uri uri = Uri.parse(request.getRequestLine().getUri());

        // "handle" both search queries and lat/long queries
        String q = uri.getQueryParameter("q");
        if ( null == q ) {
            String lat = uri.getQueryParameter("lat");
            String lon = uri.getQueryParameter("lon");
            if ( lat != null && lon != null ) {
                q = lat + "," + lon;
            } else {
                q = "";
            }
        }
        File f = getLogFile(context);
        FileWriter fw = new FileWriter(f, true);
        String log = String.format("Weather Request at: %s - %s%s", DateUtils.formatDateTime(context, time, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME ), q, System.getProperty("line.separator"));
        fw.append(log);
        fw.flush();
        fw.close();
        // if there is no query, return error 404 (not found)
        final int responseCode = mode == SERVER_MODE_ERROR ? preferences.getInt(PREF_SERVER_ERROR, 404) : q.length() == 0 ? 404 : 200;
        response.setStatusCode(responseCode);
        HttpEntity entity = new EntityTemplate(new ContentProducer() {
            public void writeTo(final OutputStream outstream) throws IOException {
                OutputStreamWriter writer = new OutputStreamWriter(outstream, "UTF-8");
                switch (mode) {
                    case SERVER_MODE_ERROR:
                        writer.write(getErrorResponse(responseCode));
                        writer.flush();
                        break;
                    case SERVER_MODE_RANDOM:
                        if( responseCode == 200 ) {
                            writer.write(
                                    "{\"city\":{\"id\":5375480,\"name\":\"Mountain View\",\"coord\":{\"lon\":-122.083847,\"lat\":37.386051},\"country\":\"US\",\"population\":0},\"cod\":\"200\",\"message\":0.0158,\"cnt\":14,\"list\":[");
                            for (int i = 0; i < 14; i++) {
                                if (i > 0) writer.write(",");
                                long dayTime = time + (i * DAY_IN_MILLIS);
                                dayTime /= 1000;
                                writer.write(generateDailyWeather(dayTime));
                            }
                            writer.write("]}");
                        } else {
                            writer.write(getErrorResponse(responseCode));
                        }
                        writer.flush();
                        break;
                    case SERVER_MODE_STATIC:
                        if ( responseCode == 200 ) {
                            writer.write(
                                    "{\"city\":{\"id\":5375480,\"name\":\"Mountain View\",\"coord\":{\"lon\":-122.083847,\"lat\":37.386051},\"country\":\"US\",\"population\":0},\"cod\":\"200\",\"message\":0.0158,\"cnt\":14,\"list\":[");
                            writer.write(generateStaticWeather(time));
                            writer.write("]}");
                        } else {
                            writer.write(getErrorResponse(responseCode));
                        }
                        writer.flush();
                        break;
                }
            }
        });
        response.setHeader("Content-Type", "text/html");
        response.setEntity(entity);
    }

    public Context getContext() {
        return context;
    }
}
