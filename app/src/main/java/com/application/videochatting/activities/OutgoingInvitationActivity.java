package com.application.videochatting.activities;

import androidx.annotation.NonNull;
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
import com.application.videochatting.databinding.ActivityOutcomingInvitationBinding;
import com.application.videochatting.models.User;
import com.application.videochatting.network.ApiClient;
import com.application.videochatting.network.ApiService;
import com.application.videochatting.utilities.Constants;
import com.application.videochatting.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OutgoingInvitationActivity extends AppCompatActivity {

    private ActivityOutcomingInvitationBinding binding;
    private User user;
    private String inviterToken= null;
    private PreferenceManager preferenceManager;
    private String meetingRoom=null;
    private String meetingType=null;
    private int rejectionCount=0;
    private int totalReceivers=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityOutcomingInvitationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init();
        setListeners();
    }

    private void setListeners() {
        binding.imageDeclineInvitation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(getIntent().getBooleanExtra("isMultiple",false)){
                    Type type=new TypeToken<ArrayList<User>>(){
                    }.getType();
                    ArrayList<User> receivers=new Gson().fromJson(getIntent().getStringExtra("selectedUsers"),type);
                   cancelInvitation(null,receivers);
                }else {
                    if(user!=null){
                        cancelInvitation(user.token,null);
                    }
            }
        }
        });
    }

    void init(){
        meetingType=getIntent().getStringExtra(Constants.KEY_TYPE);
        user= (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        preferenceManager=new PreferenceManager(getApplicationContext());
        if(meetingType!=null){
            if(meetingType.equalsIgnoreCase("video")){
                binding.imageMeetingType.setImageResource(R.drawable.ic_videocam);
            }else {
                binding.imageMeetingType.setImageResource(R.drawable.ic_call);
            }
        }
        binding.textFirstChar.setText(user.firstName.substring(0,1));
        binding.textEmail.setText(user.email);
        binding.textUserName.setText(String.format("%s %s",
                user.firstName,
                user.LastName));

        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                if(task.isSuccessful() && task.getResult()!=null){
                    inviterToken=task.getResult().getToken();
                    if(getIntent().getBooleanExtra("isMultiple",false)){
                        Type type=new TypeToken<ArrayList<User>>(){
                        }.getType();
                        ArrayList<User> receivers=new Gson().fromJson(getIntent().getStringExtra("selectedUsers"),type);
                        if(receivers!=null){
                            totalReceivers=receivers.size();
                        }
                        initiateMeeting(meetingType,null,receivers);
                    }else {
                        if(user!=null){
                            totalReceivers=1;
                            initiateMeeting(meetingType,user.token,null);
                        }
                    }

                }
            }
        });

    }

    private void initiateMeeting(String meetingType, String receiverToken, ArrayList<User> receivers){

        try {
            JSONArray tokens=new JSONArray();

            if(receiverToken!=null){
            tokens.put(receiverToken);
            }

            if(receivers!=null && receivers.size()>0){
                StringBuilder userNames=new StringBuilder();
                for(int i=0;i<receivers.size();i++){

                    tokens.put(receivers.get(i).token);
                    userNames.append(receivers.get(i).firstName).append(" ").append(receivers.get(i).LastName).append("\n");
                }

                binding.textEmail.setVisibility(View.GONE);
                binding.textFirstChar.setVisibility(View.GONE);
                binding.textUserName.setText(userNames.toString());
            }

            JSONObject body=new JSONObject();
            JSONObject data=new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE,Constants.REMOTE_MSG_INVITATION);
            data.put(Constants.REMOTE_MSG_MEETING_TYPE,meetingType);
            data.put(Constants.KEY_FIRST_NAME,preferenceManager.getString(Constants.KEY_FIRST_NAME));
            data.put(Constants.KEY_LAST_NAME,preferenceManager.getString(Constants.KEY_LAST_NAME));
            data.put(Constants.KEY_EMAIL,preferenceManager.getString(Constants.KEY_EMAIL));
            data.put(Constants.REMOTE_MSG_INVITER_TOKEN,inviterToken);

            meetingRoom= preferenceManager.getString(Constants.KEY_USER_ID)+
                    " "+ UUID.randomUUID().toString().substring(0,5);
            data.put(Constants.REMOTE_MSG_MEETING_ROOM,meetingRoom);
            body.put(Constants.REMOTE_MSG_DATA,data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS,tokens);
            sendRemoteMessage(body.toString(),Constants.REMOTE_MSG_INVITATION);
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
                    if(type.equalsIgnoreCase(Constants.REMOTE_MSG_INVITATION)){
                        Toast.makeText(OutgoingInvitationActivity.this, "Invitation sent successfully", Toast.LENGTH_SHORT).show();
                    }else if (type.equalsIgnoreCase(Constants.REMOTE_MSG_INVITATION_RESPONSE)){
                        Toast.makeText(OutgoingInvitationActivity.this, "Invitation cancelled", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                }else {
                    Toast.makeText(OutgoingInvitationActivity.this, response.message(), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(OutgoingInvitationActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });


    }

    private void cancelInvitation(String receiverToken, ArrayList<User> receivers){

        try {
            JSONArray tokens=new JSONArray();
            if(receiverToken!=null){
                tokens.put(receiverToken);
            }

            if(receivers!=null && receivers.size()>0){

                for(User user:receivers){
                    tokens.put(user.token);
                }

            }

            JSONObject body=new JSONObject();
            JSONObject data=new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE,Constants.REMOTE_MSG_INVITATION_RESPONSE);
            data.put(Constants.REMOTE_MSG_INVITATION_RESPONSE,Constants.REMOTE_MSG_INVITATION_CANCELLED);

            body.put(Constants.REMOTE_MSG_DATA,data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS,tokens);

            sendRemoteMessage(body.toString(),Constants.REMOTE_MSG_INVITATION_RESPONSE);

        }catch (Exception e){
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }

    }

    private BroadcastReceiver invitationResponseReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type=intent.getStringExtra(Constants.REMOTE_MSG_INVITATION_RESPONSE);

            if(type.equalsIgnoreCase(Constants.REMOTE_MSG_INVITATION_ACCEPTED)){
                Toast.makeText(context, "invitation accepted", Toast.LENGTH_SHORT).show();
                try {
                    URL serverUrl=new URL("https://meet.jit.si");

                    JitsiMeetConferenceOptions.Builder builder=new JitsiMeetConferenceOptions.Builder();
                    builder.setServerURL(serverUrl);
                    builder .setWelcomePageEnabled(false);
                    builder.setFeatureFlag("call-integration.enabled",false);
                    builder.setRoom(meetingRoom);
                    if(meetingType.equalsIgnoreCase("audio")){
                        builder.setVideoMuted(true);
                    }

                    JitsiMeetActivity.launch(OutgoingInvitationActivity.this,builder.build());
                    finish();
                }catch (Exception e){
                    Toast.makeText(OutgoingInvitationActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }else if(type.equalsIgnoreCase(Constants.REMOTE_MSG_INVITATION_REJECTED)){
                rejectionCount+=1;
                if(totalReceivers==rejectionCount){
                    Toast.makeText(context, "invitation rejected", Toast.LENGTH_SHORT).show();
                    finish();
                }

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
