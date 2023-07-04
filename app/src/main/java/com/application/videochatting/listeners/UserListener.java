package com.application.videochatting.listeners;

import com.application.videochatting.models.User;

public interface UserListener {

    void initiateVideoChatting(User user);
    void initiateCallChatting(User user);
    void onMultipleUsersAction(Boolean isMultipleUsersSelected);
}
