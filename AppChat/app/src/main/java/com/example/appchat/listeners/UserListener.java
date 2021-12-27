package com.example.appchat.listeners;

import com.example.appchat.models.User;

public interface UserListener {
    void onUserClicked(User user);
    void initiateAudioMeeting(User user);
    void initiateVideoMeeting(User user);
}
