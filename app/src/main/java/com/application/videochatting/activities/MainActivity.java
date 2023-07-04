package com.application.videochatting.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import com.application.videochatting.adapters.UserAdapter;
import com.application.videochatting.databinding.ActivityMainBinding;
import com.application.videochatting.listeners.UserListener;
import com.application.videochatting.models.User;
import com.application.videochatting.utilities.Constants;
import com.application.videochatting.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements UserListener {

    private ActivityMainBinding binding;
    private PreferenceManager preferenceManager;
    private UserAdapter adapter;
    private ArrayList<User> users;
    private int REQUEST_CODE_BATTERY_OPTIMIZATIONS =1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager=new PreferenceManager(this);
        binding.swipeRefreshLayout.setOnRefreshListener(this::getAllUsers);
        getUserDetails();
        getToken();
        setListeners();
        checkForBatteryOptimizations();
    }

    private void setListeners() {
        binding.textSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });
    }

    private void getUserDetails() {

        binding.textTitle.setText(String.format("%s %s",
                preferenceManager.getString(Constants.KEY_FIRST_NAME),
                preferenceManager.getString(Constants.KEY_LAST_NAME)));
    }

    private void getToken(){
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }
    private void updateToken(String token){
        Runnable runnable=new Runnable() {
            @Override
            public void run() {
                FirebaseFirestore database=FirebaseFirestore.getInstance();

                DocumentReference documentReference=
                        database.collection(Constants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Constants.KEY_USER_ID));
                documentReference.update(Constants.KEY_FCM_TOKEN,token)
                        .addOnFailureListener(e -> { showToast("failed to update token");});
                getAllUsers();
            }
        };

        runnable.run();
    }

    private void getAllUsers() {

        Runnable runnable=new Runnable() {
            @Override
            public void run() {
                binding.swipeRefreshLayout.setRefreshing(true);
                users=new ArrayList<>();
                FirebaseFirestore database=FirebaseFirestore.getInstance();

                database.collection(Constants.KEY_COLLECTION_USERS)
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                binding.swipeRefreshLayout.setRefreshing(false);
                                String myUserId=preferenceManager.getString(Constants.KEY_USER_ID);

                                if(task.isSuccessful() && task.getResult()!=null){
                                    users.clear();
                                    for(QueryDocumentSnapshot documentSnapshot: task.getResult()){

                                        if(myUserId.equalsIgnoreCase(documentSnapshot.getId())){
                                            continue;
                                        }
                                        User user=new User();
                                        user.email=documentSnapshot.getString(Constants.KEY_EMAIL);
                                        user.firstName=documentSnapshot.getString(Constants.KEY_FIRST_NAME);
                                        user.LastName=documentSnapshot.getString(Constants.KEY_LAST_NAME);
                                        user.token=documentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                                        users.add(user);
                                    }
                                    if(users.size()>0){
                                        setAdapter();

                                    }else {
                                        binding.textErrorMessage.setText(String.format("%s","No users available"));
                                        binding.textErrorMessage.setVisibility(View.VISIBLE);
                                    }

                                }else {
                                    binding.textErrorMessage.setText(String.format("%s","No users available"));
                                    binding.textErrorMessage.setVisibility(View.VISIBLE);
                                }
                            }
                        });
            }
        };
        runnable.run();

    }

    private void setAdapter() {

        adapter=new UserAdapter(users,this);
        binding.userRecyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    private void signOut(){
        showToast("signing out.....");
        FirebaseFirestore database=FirebaseFirestore.getInstance();

        DocumentReference documentReference=
                database.collection(Constants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Constants.KEY_USER_ID));
        HashMap<String,Object> updates=new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(aVoid -> { preferenceManager.clear();
                    startActivity(new Intent(getApplicationContext(),SignInActivity.class));})
                .addOnFailureListener(e -> { showToast(" Something went wrong");});
    }

    private void showToast(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void initiateVideoChatting(User user) {

        if(user.token==null || user.token.trim().isEmpty()){
            showToast(user.firstName+" "+user.LastName+" is not available for calling");
        }
        else {
            Intent intent=new Intent(getApplicationContext(), OutgoingInvitationActivity.class);
            intent.putExtra(Constants.KEY_USER,user);
            intent.putExtra(Constants.KEY_TYPE,"video");
            startActivity(intent);
        }
    }

    @Override
    public void initiateCallChatting(User user) {

        if(user.token==null || user.token.trim().isEmpty()){
            showToast(user.firstName+" "+user.LastName+" is not available for calling");
        }
        else {
            Intent intent=new Intent(getApplicationContext(), OutgoingInvitationActivity.class);
            intent.putExtra(Constants.KEY_USER,user);
            intent.putExtra(Constants.KEY_TYPE,"audio");
            startActivity(intent);
        }
    }

    @Override
    public void onMultipleUsersAction(Boolean isMultipleUsersSelected) {

        if (isMultipleUsersSelected){
            binding.imageConference.setVisibility(View.VISIBLE);
            binding.imageConference.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent=new Intent(getApplicationContext(), OutgoingInvitationActivity.class);
                    intent.putExtra("selectedUsers",new Gson().toJson(adapter.getSelectedUsers()));
                    intent.putExtra("type","video");
                    intent.putExtra("isMultiple",true);
                    startActivity(intent);
                }
            });
        }else {
            binding.imageConference.setVisibility(View.GONE);
        }
    }

    private void checkForBatteryOptimizations(){
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.M){

            PowerManager powerManager= (PowerManager) getSystemService(POWER_SERVICE);

            if(!powerManager.isIgnoringBatteryOptimizations(getPackageName())){

                AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("warning");
                builder.setMessage("Battery Optimization is enabled, It can interrupt running background services.");
                builder.setPositiveButton("Disable", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Intent intent=new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                        startActivityForResult(intent,REQUEST_CODE_BATTERY_OPTIMIZATIONS);
                    }
                });
                builder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.create().show();
            }
        }
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        super.startActivityForResult(intent, requestCode);
        if(requestCode==REQUEST_CODE_BATTERY_OPTIMIZATIONS){
            checkForBatteryOptimizations();
        }
    }
    
}