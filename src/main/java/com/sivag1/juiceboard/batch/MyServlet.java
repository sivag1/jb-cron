/*
   For step-by-step instructions on connecting your Android application to this backend module,
   see "App Engine Java Servlet Module" template documentation at
   https://github.com/GoogleCloudPlatform/gradle-appengine-templates/tree/master/HelloWorld
*/

package com.sivag1.juiceboard.batch;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;
import com.google.android.gcm.server.InvalidRequestException;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.google.appengine.repackaged.com.google.gson.Gson;
import com.sivag1.juiceboard.lib.data.JuiceLevel;

import java.io.IOException;
import java.util.Date;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MyServlet extends HttpServlet {

    public static String FIREBASE_JL_URL = "https://boiling-inferno-7296.firebaseio.com/juicelevels";

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        doPost(req, resp);

    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        //Create a new Firebase instance and subscribe on child events.
        Firebase firebase = new Firebase(FIREBASE_JL_URL);
        firebase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    final JuiceLevel originDevice = data.getValue(JuiceLevel.class);

                    if ((originDevice.getLastNotifPushDate() == null || (new Date().getTime() - originDevice.getLastNotifPushDate().getTime() > 300000))
                            && originDevice.getLastKnownPercentage() <= 100) {

                        final String uid = originDevice.getUid();
                        final String deviceId = originDevice.getDeviceId();

                        Firebase newRef = new Firebase(FIREBASE_JL_URL);
                        Query query = newRef.orderByChild("uid").equalTo(uid);

                        query.addChildEventListener(new ChildEventListener() {
                            @Override
                            public void onChildAdded(DataSnapshot snapshot, String previousChild) {

                                JuiceLevel juiceLevel = snapshot.getValue(JuiceLevel.class);

                                final String pushRegId = juiceLevel.getPushRegId();

                                if (!deviceId.equals(juiceLevel.getDeviceId())) {

                                    final String GCM_API_KEY = "AIzaSyBzu3E_AYE1sJrEWhgRWm4rI155DkZ5AI4";
                                    final int retries = 3;
                                    final String notificationToken = pushRegId;
                                    Sender sender = new Sender(GCM_API_KEY);
                                    Message msg = new Message.Builder().addData("message", new Gson().toJson(originDevice)).build();

                                    try {
                                        Result result = sender.send(msg, notificationToken, retries);
                                    } catch (InvalidRequestException e) {
                                    } catch (IOException e) {
                                    }
                                }
                            }

                            @Override
                            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                            }

                            @Override
                            public void onChildRemoved(DataSnapshot dataSnapshot) {
                            }

                            @Override
                            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                            }

                            @Override
                            public void onCancelled(FirebaseError firebaseError) {
                            }
                        });
                    }
                }
            }

            public void onCancelled(FirebaseError firebaseError) {
            }
        });
    }
}
