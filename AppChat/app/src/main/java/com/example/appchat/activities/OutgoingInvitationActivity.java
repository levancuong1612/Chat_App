package com.example.appchat.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Toast;

import com.example.appchat.R;
import com.example.appchat.databinding.ActivityOutgoingInvitationBinding;
import com.example.appchat.models.User;
import com.example.appchat.network.ApiClient;
import com.example.appchat.network.ApiService;
import com.example.appchat.utilites.Constants;
import com.example.appchat.utilites.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
public class OutgoingInvitationActivity extends AppCompatActivity {
    private PreferenceManager preferenceManager;
    private String inviterToken=null;
    String meetingRoom=null;
    ActivityOutgoingInvitationBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding= ActivityOutgoingInvitationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager  = new PreferenceManager(getApplicationContext());

        String meetingType=getIntent().getStringExtra("type");
        User user= (User) getIntent().getSerializableExtra("user");
        if(meetingType!=null){
            if(meetingType.equals("video")){
                binding.imageMeetingType.setImageResource(R.drawable.ic_video_call_24);
            }
        }
        if(user!=null){
            binding.imageUser.setImageBitmap(getUserImage(user.images));
            binding.textviewUsername.setText(user.name);
            binding.textviewEmail.setText(user.email);

        }
        binding.imageStopInvitation.setOnClickListener(v->{
            if(user!=null){
                cancelInvitation(user.token);
            }
        });
        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                if(task.isSuccessful() && task.getResult()!=null){
                    inviterToken=task.getResult().getToken();
                    if(meetingType!=null && user!=null){
                        initialMeeting(meetingType,user.token);
                    }
                }
            }
        });
    }
    private Bitmap getUserImage(String encodeImages){
        byte[] bytes= Base64.decode(encodeImages,Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
    }
    private  void initialMeeting(String meetingType,String receiverToken){
        try {
            JSONArray tokens= new JSONArray();
            tokens.put(receiverToken);
            JSONObject data= new JSONObject();
            JSONObject body= new JSONObject();
            data.put(Constants.REMOTE_MSG_TYPE,Constants.REMOTE_MSG_INVITATION);
            data.put(Constants.REMOTE_MSG_MEETING_TYPE,meetingType);
            data.put(Constants.KEY_USER_ID,preferenceManager.getString(Constants.KEY_USER_ID));
            data.put(Constants.KEY_NAME,preferenceManager.getString(Constants.KEY_NAME));
            data.put(Constants.REMOTE_MSG_INVITER_TOKEN,inviterToken);
            meetingRoom=preferenceManager.getString(Constants.KEY_USER_ID)+ "-" +
                    UUID.randomUUID().toString().substring(0,5);
            data.put(Constants.REMOTE_MSG_MEETING_ROOM,meetingRoom);



            body.put(Constants.REMOTE_MSG_DATA,data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS,tokens);
            sendRemoteMessage(body.toString(),Constants.REMOTE_MSG_INVITATION);

            
        }catch (Exception ex){
            Toast.makeText(getApplicationContext(), ex.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    private  void sendRemoteMessage(String remoteMessageBody, String type){
        ApiClient.getClient().create(ApiService.class).sendMessage(
                Constants.getRemoteMsgHeaders(),remoteMessageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call,@NonNull Response<String> response) {
                if(response.isSuccessful()){
                    if(type.equals(Constants.REMOTE_MSG_INVITATION)){
                        Toast.makeText(getApplicationContext(), "Đã nhận cuộc gọi", Toast.LENGTH_SHORT).show();
                    }else if(type.equals(Constants.REMOTE_MSG_INVITATION_RESPONSE)){
                        Toast.makeText(getApplicationContext(), "Kết thúc cuộc gọi", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }else{
                    Toast.makeText(getApplicationContext(), response.message(), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call,@NonNull Throwable t) {
                Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private  void cancelInvitation( String receiverToken){
        try{
            JSONArray tokens= new JSONArray();
            tokens.put(receiverToken);
            JSONObject data= new JSONObject();
            JSONObject body= new JSONObject();
            data.put(Constants.REMOTE_MSG_TYPE,Constants.REMOTE_MSG_INVITATION_RESPONSE);
            data.put(Constants.REMOTE_MSG_INVITATION_RESPONSE,Constants.REMOTE_MSG_INVITATION_CANCELED);

            body.put(Constants.REMOTE_MSG_DATA,data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS,tokens);
            sendRemoteMessage(body.toString(),Constants.REMOTE_MSG_INVITATION_RESPONSE);

        }catch (Exception ex){
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    private BroadcastReceiver invitationResponseReceiver= new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type= intent.getStringExtra(Constants.REMOTE_MSG_INVITATION_RESPONSE);
            if(type!=null){
                if(type.equals(Constants.REMOTE_MSG_INVITATION_ACCEPTED)){
                    Toast.makeText(getApplicationContext(), "Chấp nhận cuộc gọi", Toast.LENGTH_SHORT).show();
                }else if(type.equals(Constants.REMOTE_MSG_INVITATION_REJECTED)){
                    Toast.makeText(context, "Từ chối cuộc gọi", Toast.LENGTH_SHORT).show();
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
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
                invitationResponseReceiver
        );
    }
}