package com.application.videochatting.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.application.videochatting.databinding.ActivitySignUpBinding;
import com.application.videochatting.utilities.Constants;
import com.application.videochatting.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    ActivitySignUpBinding binding;
    private PreferenceManager preferenceManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager=new PreferenceManager(this);
        setListeners();

    }

    private void showToast(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    private void setListeners() {
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.textSignIn.setOnClickListener(v -> onBackPressed());
        binding.buttonSignUp.setOnClickListener(v -> {
            if(isValidSignUpDetails())
            {
                signUp();
            }
        });
    }

    private Boolean isValidSignUpDetails(){

        if (binding.inputFirstName.getText().toString().trim().isEmpty()){
            showToast("Enter your first name");
            return false;
        }
        else if (binding.inputLastName.getText().toString().trim().isEmpty()){
            showToast("Enter your last name");
            return false;
        }
        else if (binding.inputEmail.getText().toString().trim().isEmpty()){
            showToast("Enter E-mail");
            return false;
        }
        else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()){
            showToast("Enter valid E-mail");
            return false;
        }
        else if (binding.inputPassword.getText().toString().trim().isEmpty()){
            showToast("Enter password");
            return false;
        }
        else if (binding.inputConfirmPassword.getText().toString().trim().isEmpty()){
            showToast("Confirm your password");
            return false;
        }
        else if (!binding.inputPassword.getText().toString().equalsIgnoreCase(binding.inputConfirmPassword.getText().toString())){
            showToast("Password & Confirm password must be same");
            return false;
        }
        else {
            return true;
        }
    }
    private void signUp(){

        loading(true);

        FirebaseFirestore database=FirebaseFirestore.getInstance();
        Map<String,Object> user=new HashMap<>();
        user.put(Constants.KEY_FIRST_NAME,binding.inputFirstName.getText().toString());
        user.put(Constants.KEY_LAST_NAME,binding.inputLastName.getText().toString());
        user.put(Constants.KEY_EMAIL,binding.inputEmail.getText().toString());
        user.put(Constants.KEY_PASSWORD,binding.inputPassword.getText().toString());
        database.collection(Constants.KEY_COLLECTION_USERS).add(user).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                loading(false);
                preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN,true);
                preferenceManager.putString(Constants.KEY_USER_ID,documentReference.getId());
                preferenceManager.putString(Constants.KEY_FIRST_NAME,binding.inputFirstName.getText().toString());
                preferenceManager.putString(Constants.KEY_LAST_NAME,binding.inputLastName.getText().toString());
                Intent intent=new Intent(getApplicationContext(),MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                loading(false);
                showToast(e.getMessage());
            }
        });
    }

    private void loading(Boolean isLoading){
        if(isLoading){
            binding.buttonSignUp.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        }
        else {
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.buttonSignUp.setVisibility(View.VISIBLE);
        }
    }
}