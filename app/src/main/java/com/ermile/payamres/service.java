package com.ermile.payamres;


import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.telephony.SmsManager;
import android.util.Log;

import com.android.volley.AuthFailureError;
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
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class service extends Service {

    String smsappkey = "e2c998bbb48931f40a0f7d1cba53434f";
    String link_LastSMS = "https://khadije.com/api/v6/smsapp/notsent";
    String link_newSMS = "https://khadije.com/api/v6/smsapp/queue";
    String link_smsIsSent = "https://khadije.com/api/v6/smsapp/sent";
    String id_smsForSend = null;

    Timer timer;
    TimerTask timerTask;
    String TAG = "Timers";
    int Your_X_SECS = 30;


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
        timer.schedule(timerTask, 6000, Your_X_SECS * 1000); //
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
        /*Shared Preferences for info user*/
        final SharedPreferences save_user = getApplicationContext().getSharedPreferences("save_user", MODE_PRIVATE);
        final Boolean has_number = save_user.getBoolean("has_number", false);
        final String number_phone = save_user.getString("number_phone", null);

        timerTask = new TimerTask() {
            public void run() {
                //use a handler to run a toast that shows the current timestamp
                handler.post(new Runnable() {
                    public void run() {
                        //TODO CALL NOTIFICATION FUNC
                        if (has_number && number_phone != null) {
                            LastSMSSending(getBaseContext());
                        }
                    }
                });
            }
        };
    }

    /*Last SMS for Sending*/
    public void LastSMSSending(final Context context_LastSMSSending){
        /*Get Number Phone */
        final SharedPreferences save_user = context_LastSMSSending.getApplicationContext().getSharedPreferences("save_user", MODE_PRIVATE);
        final Boolean has_number = save_user.getBoolean("has_number", false);
        final String number_phone = save_user.getString("number_phone", null);
        if (has_number && number_phone != null){
            StringRequest post_LastSMSSending = new StringRequest(Request.Method.POST, link_LastSMS,
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
                                        for (int newM = 0; newM <= result.length(); newM++) {
                                            JSONObject getsms_Forsend = result.getJSONObject(newM);
                                            id_smsForSend = getsms_Forsend.getString("id");
                                            String smsto = getsms_Forsend.getString("fromnumber");
                                            String sms_text = getsms_Forsend.getString("answertext");

                                            try {
                                                SmsManager smsManager = SmsManager.getDefault();
                                                smsManager.sendTextMessage(smsto, null, sms_text, null, null);
                                                Log.i("LastSMSSending", "last sms > ok true > send sms");
                                                SMS_Sent(context_LastSMSSending);
                                            } catch (Exception e) {
                                                Log.i("LastSMSSending_error","No Send last sms"+"\n"+smsto+"\n"+sms_text);
                                            }

                                        }
                                    }else {
                                        NewSMSSending(context_LastSMSSending);
                                        Log.i("LastSMSSending", "last sms > no sms for send :) > Check new sms");
                                    }


                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.i("LastSMSSending", "last sms > error");
                }
            }) {
                @Override
                public Map<String, String> getHeaders() {
                    HashMap<String, String> lastsms_headers = new HashMap<>();
                    lastsms_headers.put("smsappkey", smsappkey);
                    lastsms_headers.put("gateway", number_phone);
                    Log.i("LastSMSSending", "Send Header");
                    return lastsms_headers;
                }
            };
            AppContoroler.getInstance().addToRequestQueue(post_LastSMSSending);
        }

    }

    /*New SMS for Sending*/
    public void NewSMSSending(final Context context_NewSMSSending){
        /*Get Number Phone */
        final SharedPreferences save_user = context_NewSMSSending.getApplicationContext().getSharedPreferences("save_user", MODE_PRIVATE);
        final Boolean has_number = save_user.getBoolean("has_number", false);
        final String number_phone = save_user.getString("number_phone", null);
        if (has_number && number_phone != null){
            StringRequest post_NewSMSSending = new StringRequest(Request.Method.POST, link_newSMS,
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
                                        for (int newM = 0; newM <= result.length(); newM++) {
                                            JSONObject getsms_Forsend = result.getJSONObject(newM);
                                            id_smsForSend = getsms_Forsend.getString("id");
                                            String smsto = getsms_Forsend.getString("fromnumber");
                                            String sms_text = getsms_Forsend.getString("answertext");

                                            try {
                                                SmsManager smsManager = SmsManager.getDefault();
                                                smsManager.sendTextMessage(smsto, null, sms_text, null, null);
                                                Log.i("NewSMSSending", "last sms > ok true > send sms");
                                                SMS_Sent(context_NewSMSSending);
                                            } catch (Exception e) {
                                                Log.i("NewSMSSending","No Send");
                                            }
                                        }
                                    }else {
                                        Log.i("NewSMSSending", "new sms > no sms for send :)");
                                    }

                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.i("amin", "new sms > error");
                }
            }) {
                @Override
                public Map<String, String> getHeaders() {
                    HashMap<String, String> newsms_headers = new HashMap<>();
                    newsms_headers.put("smsappkey", smsappkey);
                    newsms_headers.put("gateway", number_phone);
                    Log.i("NewSMSSending", "Send Header");
                    return newsms_headers;
                }
            };
            AppContoroler.getInstance().addToRequestQueue(post_NewSMSSending);
        }

    }

    /*SMS Sent*/
    public void SMS_Sent(Context context_SMS_Sent){
        /*Get Number Phone */
        final SharedPreferences save_user = context_SMS_Sent.getApplicationContext().getSharedPreferences("save_user", MODE_PRIVATE);
        final Boolean has_number = save_user.getBoolean("has_number", false);
        final String number_phone = save_user.getString("number_phone", null);
        if (has_number && number_phone != null){
            StringRequest post_user_add = new StringRequest(Request.Method.POST, link_smsIsSent,
                    new Response.Listener<String>(){
                        @Override
                        public void onResponse(String response) {
                            try {
                                JSONObject mainObject = new JSONObject(response);
                                /*if sending from database is ok > Delete data from database*/
                                Boolean ok_sent = mainObject.getBoolean("ok");
                                if (ok_sent){
                                    Log.i("SMS_Sent","SMS Sent | "+id_smsForSend);
                                }else {
                                    Log.i("SMS_Sent","SMS NOT Sent | "+id_smsForSend);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.i("SMS_Sent", "sms sent > error");
                }
            })
            {
                @Override
                public Map<String, String> getHeaders()  {
                    HashMap<String, String> headers = new HashMap<>();
                    headers.put("smsappkey", smsappkey );
                    headers.put("gateway", number_phone );
                    Log.i("SMS_Sent", "Send Header");
                    return headers;
                }
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    Map<String, String> posting = new HashMap<>();
                    posting.put("smsid", id_smsForSend);
                    Log.i("SMS_Sent","Send Parametr id | "+id_smsForSend);
                    return posting;
                }
            };AppContoroler.getInstance().addToRequestQueue(post_user_add);
        }

    }



}