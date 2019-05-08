package com.ermile.payamres;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.ermile.payamres.network.AppContoroler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class SendSMS extends Service {

    Context context;

    public SendSMS(Context context) {
        this.context = context;
    }

    final SharedPreferences save_user = context.getSharedPreferences("save_user", MODE_PRIVATE);
    final String number_phone = save_user.getString("number_phone", "09109610612");

    DatabaseManager dbm;
    String smsappkey = "e2c998bbb48931f40a0f7d1cba53434f";
    String link_SendToServer = "https://khadije.com/fa/api/v6/smsapp/new";
    String link_LastSMS = "https://khadije.com/fa/api/v6/smsapp/notsent";
    String link_newSMS = "https://khadije.com/fa/api/v6/smsapp/queue";
    String id_smsForSend = null;
    String link_smsIsSent = "https://khadije.com/fa/api/v6/smsapp/sent";


    Timer timer;
    TimerTask timerTask;
    String TAG = "Timers";
    int Your_X_SECS = 60;


    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);

        startTimer();

        return START_STICKY;
    }


    @Override
    public void onCreate() {
        Log.e(TAG, "onCreate");


    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        stoptimertask();
        super.onDestroy();


    }

    //we are going to use a handler to be able to run in our TimerTask
    final Handler handler = new Handler();


    public void startTimer() {
        //set a new Timer
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        //schedule the timer, after the first 5000ms the TimerTask will run every 10000ms
        timer.schedule(timerTask, 6000, Your_X_SECS * 10); //
        //timer.schedule(timerTask, 5000,1000); //
    }

    public void stoptimertask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void initializeTimerTask() {

        timerTask = new TimerTask() {
            public void run() {

                //use a handler to run a toast that shows the current timestamp
                handler.post(new Runnable() {
                    public void run() {

                        //TODO CALL NOTIFICATION FUNC
                        if (hasInternetConnection(context)){
                            LastSMSSending();
                        }else {
                            Log.i("amin","Send SMS > no net");
                        }

                    }
                });
            }
        };
    }


    /*Last SMS for Sending*/
    public void LastSMSSending(){

        StringRequest post_user_add = new StringRequest(Request.Method.POST, link_LastSMS,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject mainObject = new JSONObject(response);
                            /*if sending from database is ok > Delete data from database*/
                            Boolean ok_dashboard = mainObject.getBoolean("ok");
                            if (ok_dashboard) {
                                if (!mainObject.isNull("result")){
                                    JSONArray result = mainObject.getJSONArray("result");
                                    Log.i("amin", "last sms > ok true");
                                    for (int newM = 0; newM <= result.length(); newM++) {
                                        JSONObject getsms_Forsend = result.getJSONObject(newM);
                                        id_smsForSend = getsms_Forsend.getString("id");
                                        String smsto = getsms_Forsend.getString("fromnumber");
                                        String sms_text = getsms_Forsend.getString("text");

                                        SmsManager smsManager = SmsManager.getDefault();
                                        smsManager.sendTextMessage(smsto, null, sms_text, null, null);
                                        Log.i("amin", "last sms > ok true > send sms");
                                        SMS_Sent();
                                    }
                                }else {
                                    NewSMSSending();
                                    Log.i("amin", "last sms > no sms for send :) > Check new sms");
                                }


                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("smsappkey", smsappkey);
                headers.put("gateway", number_phone);
                return headers;
            }
        };
        AppContoroler.getInstance().addToRequestQueue(post_user_add);
    }

    /*New SMS for Sending*/
    public void NewSMSSending(){
        StringRequest post_user_add = new StringRequest(Request.Method.POST, link_newSMS,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject mainObject = new JSONObject(response);
                            /*if sending from database is ok > Delete data from database*/
                            Boolean ok_dashboard = mainObject.getBoolean("ok");
                            Log.i("amin", "new sms > ok is: "+ok_dashboard);
                            if (ok_dashboard) {
                                if (!mainObject.isNull("result")){
                                    JSONArray result = mainObject.getJSONArray("result");
                                    Log.i("amin", "new sms > ok true");
                                    for (int newM = 0; newM <= result.length(); newM++) {
                                        JSONObject getsms_Forsend = result.getJSONObject(newM);
                                        id_smsForSend = getsms_Forsend.getString("id");
                                        String smsto = getsms_Forsend.getString("fromnumber");
                                        String sms_text = getsms_Forsend.getString("text");

                                        SmsManager smsManager = SmsManager.getDefault();
                                        smsManager.sendTextMessage(smsto, null, sms_text, null, null);
                                        Log.i("amin", "new sms > ok true > send sms");
                                        SMS_Sent();
                                    }
                                }else {
                                    Log.i("amin", "new sms > no sms for send :)");
                                }

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("smsappkey", smsappkey);
                headers.put("gateway", number_phone);
                return headers;
            }
        };
        AppContoroler.getInstance().addToRequestQueue(post_user_add);
    }

    /*SMS Sent*/
    public void SMS_Sent(){
        StringRequest post_user_add = new StringRequest(Request.Method.POST, link_smsIsSent,
                new Response.Listener<String>(){
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject mainObject = new JSONObject(response);
                            /*if sending from database is ok > Delete data from database*/
                            Boolean ok_sent = mainObject.getBoolean("ok");
                            if (ok_sent){
                                Log.i("amin","SMS Sent | "+id_smsForSend);
                            }else {
                                Log.i("amin","SMS NOT Sent | "+id_smsForSend);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        })
        {
            @Override
            public Map<String, String> getHeaders()  {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("smsappkey", smsappkey );
                headers.put("gateway", number_phone );
                headers.put("smsid", id_smsForSend );
                Log.i("amin","header > SMS Sent | "+id_smsForSend);
                return headers;
            }
        };AppContoroler.getInstance().addToRequestQueue(post_user_add);
    }


    /*check InterNet Connection*/
    public static boolean hasInternetConnection(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiNetwork != null && wifiNetwork.isConnected())
        {
            return true;
        }
        NetworkInfo mobileNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (mobileNetwork != null && mobileNetwork.isConnected())
        {
            return true;
        }
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected())
        {
            return true;
        }
        return false;
    }


}