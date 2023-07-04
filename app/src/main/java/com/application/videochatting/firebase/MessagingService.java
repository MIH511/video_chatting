package com.application.videochatting.firebase;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.application.videochatting.activities.IncomingInvitationActivity;
import com.application.videochatting.utilities.Constants;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MessagingService extends FirebaseMessagingService {


    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d("FCM",token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

//        Log.d("KMKM",remoteMessage.getNotification().getBody());
        String type=remoteMessage.getData().get(Constants.REMOTE_MSG_TYPE);

        if(type!=null){
            if(type.equalsIgnoreCase(Constants.REMOTE_MSG_INVITATION)){

                Intent intent=new Intent(getApplicationContext(), IncomingInvitationActivity.class);
                intent.putExtra(Constants.REMOTE_MSG_MEETING_TYPE,remoteMessage.getData().get(Constants.REMOTE_MSG_MEETING_TYPE));
                intent.putExtra(Constants.KEY_FIRST_NAME,remoteMessage.getData().get(Constants.KEY_FIRST_NAME));
                intent.putExtra(Constants.KEY_LAST_NAME,remoteMessage.getData().get(Constants.KEY_LAST_NAME));
                intent.putExtra(Constants.KEY_EMAIL,remoteMessage.getData().get(Constants.KEY_EMAIL));
                intent.putExtra(Constants.REMOTE_MSG_INVITER_TOKEN,remoteMessage.getData().get(Constants.REMOTE_MSG_INVITER_TOKEN));
                intent.putExtra(Constants.REMOTE_MSG_MEETING_ROOM,remoteMessage.getData().get(Constants.REMOTE_MSG_MEETING_ROOM));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }else if (type.equalsIgnoreCase(Constants.REMOTE_MSG_INVITATION_RESPONSE)){
                Intent intent=new Intent(Constants.REMOTE_MSG_INVITATION_RESPONSE);
                intent.putExtra(Constants.REMOTE_MSG_INVITATION_RESPONSE
                ,remoteMessage.getData().get(Constants.REMOTE_MSG_INVITATION_RESPONSE));
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            }
        }
    }
}
