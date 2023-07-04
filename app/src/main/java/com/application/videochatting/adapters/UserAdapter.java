package com.application.videochatting.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.application.videochatting.databinding.ItemContainerUserBinding;
import com.application.videochatting.listeners.UserListener;
import com.application.videochatting.models.User;
import com.application.videochatting.utilities.Constants;

import java.util.ArrayList;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private final List<User> users;
    private final UserListener userListener;
    private List<User> selectedUsers;

    public UserAdapter(List<User> users,UserListener userListener) {

        this.users = users;
        this.userListener=userListener;
        this.selectedUsers=new ArrayList<>();
    }

    public List<User> getSelectedUsers() {
        return selectedUsers;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemContainerUserBinding itemContainerUserBinding=ItemContainerUserBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new UserViewHolder(itemContainerUserBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {

        holder.setUserData(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder{

        ItemContainerUserBinding binding;
        UserViewHolder(ItemContainerUserBinding itemContainerUserBinding) {
            super(itemContainerUserBinding.getRoot());
            binding=itemContainerUserBinding;
        }

        void setUserData(User user){
            binding.textFirstChar.setText(user.firstName.substring(0,1));
            binding.textUserName.setText(String.format("%s %s",
                    user.firstName,
                    user.LastName));
            binding.textEmail.setText(user.email);
            binding.imageVideoChatting.setOnClickListener(v -> userListener.initiateVideoChatting(user));
            binding.imageAudioChatting.setOnClickListener(v -> userListener.initiateCallChatting(user));

            binding.userContainer.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if(binding.imageSelected.getVisibility() !=View.VISIBLE){
                    selectedUsers.add(user);
                    binding.imageSelected.setVisibility(View.VISIBLE);
                    binding.imageAudioChatting.setVisibility(View.GONE);
                    binding.imageVideoChatting.setVisibility(View.GONE);
                    userListener.onMultipleUsersAction(true);
                    }
                    return true;
                }
            });

            binding.userContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(binding.imageSelected.getVisibility() ==View.VISIBLE){

                        selectedUsers.remove(user);
                        binding.imageSelected.setVisibility(View.GONE);
                        binding.imageVideoChatting.setVisibility(View.VISIBLE);
                        binding.imageAudioChatting.setVisibility(View.VISIBLE);
                        if(selectedUsers.size()==0){
                            userListener.onMultipleUsersAction(false);
                        }else {
                            if(selectedUsers.size()>0){
                                selectedUsers.add(user);
                                binding.imageSelected.setVisibility(View.VISIBLE);
                                binding.imageAudioChatting.setVisibility(View.GONE);
                                binding.imageVideoChatting.setVisibility(View.GONE);

                            }
                        }
                    }
                }
            });
        }
    }

}
