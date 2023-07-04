package com.application.videochatting.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.application.videochatting.R;
import com.application.videochatting.databinding.ActivityIncomingInvitationBinding;
import com.application.videochatting.network.ApiClient;
import com.application.videochatting.network.ApiService;
import com.application.videochatting.utilities.Constants;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetActivityInterface;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Url;

public class IncomingInvitationActivity extends AppCompatActivity {

    ActivityIncomingInvitationBinding binding;
    String meetingType=null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityIncomingInvitationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        init();
    }

    private void init(){

        meetingType= getIntent().getStringExtra(Constants.REMOTE_MSG_MEETING_TYPE);
        String firstName=getIntent().getStringExtra(Constants.KEY_FIRST_NAME);
        String lastName=getIntent().getStringExtra(Constants.KEY_LAST_NAME);
        String email=getIntent().getStringExtra(Constants.KEY_EMAIL);

        if(meetingType!=null){
            if(meetingType.equalsIgnoreCase("video")){
            binding.imageMeetingType.setImageResource(R.drawable.ic_videocam);
            }else {
                binding.imageMeetingType.setImageResource(R.drawable.ic_call);
            }
        }

        if(firstName!=null){
            binding.textFirstChar.setText(firstName.substring(0,1));
        }
        if(firstName!=null && lastName!=null){
            binding.textUserName.setText(String.format("%s %s",firstName, lastName));
        }
        if(email!=null){
            binding.textEmail.setText(email);
        }

        binding.imageAcceptInvitation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendInvitationResponse(Constants.REMOTE_MSG_INVITATION_ACCEPTED,getIntent().getStringExtra(Constants.REMOTE_MSG_INVITER_TOKEN));
            }
        });

        binding.imageDeclineInvitation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendInvitationResponse(Constants.REMOTE_MSG_INVITATION_REJECTED,getIntent().getStringExtra(Constants.REMOTE_MSG_INVITER_TOKEN));
            }
        });
    }

    private void sendInvitationResponse(String type, String receiverToken){

        try {
            JSONArray tokens=new JSONArray();
            tokens.put(receiverToken);

            JSONObject body=new JSONObject();
            JSONObject data=new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE,Constants.REMOTE_MSG_INVITATION_RESPONSE);
            data.put(Constants.REMOTE_MSG_INVITATION_RESPONSE,type);

            body.put(Constants.REMOTE_MSG_DATA,data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS,tokens);

            sendRemoteMessage(body.toString(),type);

        }catch (Exception e){
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }

    }

    private void sendRemoteMessage(String remoteMessageBody, String type){

        ApiClient.retrofit().create(ApiService.class).sendRemoteMessage(
                Constants.getRemoteMessageHeaders(),remoteMessageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()){

                    if (type.equalsIgnoreCase(Constants.REMOTE_MSG_INVITATION_ACCEPTED)){

                        try {
                            URL serverUrl=new URL("https://meet.jit.si");

                            JitsiMeetConferenceOptions.Builder builder=new JitsiMeetConferenceOptions.Builder();
                            builder.setServerURL(serverUrl);
                            builder .setWelcomePageEnabled(false);
                            builder.setFeatureFlag("call-integration.enabled",false);
                            builder.setRoom(getIntent().getStringExtra(Constants.REMOTE_MSG_MEETING_ROOM));
                            if(meetingType.equalsIgnoreCase("audio")){
                                builder.setVideoMuted(true);
                            }
                            
                           JitsiMeetActivity.launch(IncomingInvitationActivity.this,builder.build());

                            finish();
                        }catch (Exception e){
                            Toast.makeText(IncomingInvitationActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                    else {
                        Toast.makeText(IncomingInvitationActivity.this, "invitation rejected", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }else {
                    Toast.makeText(IncomingInvitationActivity.this, response.message(), Toast.LENGTH_SHORT).show();
                    finish();
                }

            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(IncomingInvitationActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private BroadcastReceiver invitationResponseReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type=intent.getStringExtra(Constants.REMOTE_MSG_INVITATION_RESPONSE);

            if(type.equalsIgnoreCase(Constants.REMOTE_MSG_INVITATION_CANCELLED)){
                Toast.makeText(context, "invitation cancelled", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                invitationResponseReceiver,new IntentFilter(Constants.REMOTE_MSG_INVITATION_RESPONSE)
        );
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(invitationResponseReceiver);
    }
}