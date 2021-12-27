package com.example.appchat.firebase;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.appchat.R;
import com.example.appchat.activities.ChatActivity;
import com.example.appchat.activities.IncomingInvitationActivity;
import com.example.appchat.models.User;
import com.example.appchat.utilites.Constants;
import com.example.appchat.utilites.PreferenceManager;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Random;

public class MessagingService extends FirebaseMessagingService {
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        PreferenceManager preferenceManager= new PreferenceManager(getApplicationContext());

        User user= new User();
        user.id=remoteMessage.getData().get(Constants.KEY_USER_ID);
        user.name=remoteMessage.getData().get(Constants.KEY_NAME);
        user.images=preferenceManager.getString(Constants.KEY_IMAGE);
        user.email=preferenceManager.getString(Constants.KEY_EMAIL);
        user.token=remoteMessage.getData().get(Constants.KEY_FCM_TOKEN);
        int notificationId= new Random().nextInt();
        String channelID="chat_message";
        Intent intent= new Intent(this, ChatActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(Constants.KEY_USER,user);

        PendingIntent pendingIntent= PendingIntent.getActivity(getApplicationContext(), 0, intent,PendingIntent.FLAG_ONE_SHOT);
        NotificationCompat.Builder builder= new NotificationCompat.Builder(this, channelID);
        builder.setSmallIcon(R.drawable.ic_notifications);
        builder.setContentTitle(user.name);
        builder.setContentText(remoteMessage.getData().get(Constants.KEY_MESSAGE));
        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(
                remoteMessage.getData().get(Constants.KEY_MESSAGE)
        ));
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(true);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            CharSequence channelName="Tin nhắn mới" ;
        String channelDescription="this notification channel is used for chat";
        int importance= NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel= new NotificationChannel(channelID, channelName,importance);
            channel.setDescription(channelDescription);
            NotificationManager notificationManager= getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
       NotificationManagerCompat notificationCompat= NotificationManagerCompat.from(this);
        notificationCompat.notify(notificationId,builder.build());
        String type= remoteMessage.getData().get(Constants.REMOTE_MSG_TYPE);
        if(type!=null){
            if(type.equals(Constants.REMOTE_MSG_INVITATION)){
                Intent intent1= new Intent(getApplicationContext(), IncomingInvitationActivity.class);
                intent1.putExtra(
                        Constants.REMOTE_MSG_MEETING_TYPE,
                        remoteMessage.getData().get(Constants.REMOTE_MSG_MEETING_TYPE)
                );
                intent1.putExtra(
                        Constants.REMOTE_MSG_INVITER_TOKEN,
                        remoteMessage.getData().get(Constants.REMOTE_MSG_INVITER_TOKEN)
                );
                intent1.putExtra(
                        Constants.KEY_USER,
                        user
                );
                intent1.putExtra(
                        Constants.REMOTE_MSG_MEETING_ROOM,
                        remoteMessage.getData().get(Constants.REMOTE_MSG_MEETING_ROOM)
                );


                intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent1);
            }else if(type.equals(Constants.REMOTE_MSG_INVITATION_RESPONSE)){
                Intent intent2= new Intent(Constants.REMOTE_MSG_INVITATION_RESPONSE);
                intent2.putExtra(
                        Constants.REMOTE_MSG_INVITATION_RESPONSE,
                        remoteMessage.getData().get(Constants.REMOTE_MSG_INVITATION_RESPONSE)
                );
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent2);

            }

        }
    }
}
