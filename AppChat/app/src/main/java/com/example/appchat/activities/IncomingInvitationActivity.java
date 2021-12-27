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
import android.view.View;
import android.widget.Toast;

import com.example.appchat.R;
import com.example.appchat.adapters.UserAdapter;
import com.example.appchat.databinding.ActivityIncomingInvitationBinding;
import com.example.appchat.models.User;
import com.example.appchat.network.ApiClient;
import com.example.appchat.network.ApiService;
import com.example.appchat.utilites.Constants;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IncomingInvitationActivity extends AppCompatActivity {

    ActivityIncomingInvitationBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityIncomingInvitationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        String meetingType= getIntent().getStringExtra(Constants.REMOTE_MSG_MEETING_TYPE);
        if(meetingType!=null){
            if(meetingType.equals("video")){
                binding.imageMeetingType.setImageResource(R.drawable.ic_video_call_24);
            }
        }
        String fullName= getIntent().getStringExtra(Constants.KEY_NAME);
        String email= getIntent().getStringExtra(Constants.KEY_EMAIL);
        String image= getIntent().getStringExtra(Constants.KEY_IMAGE);
        User user= (User) getIntent().getSerializableExtra(Constants.KEY_USER);


        binding.textviewUsername.setText(user.name);
        binding.textviewEmail.setText(user.email);
        binding.imageMeetingType.setImageBitmap(getUserImage(user.images));

        binding.imageAcceptInvitation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendInvitationResponse(
                        Constants.REMOTE_MSG_INVITATION_ACCEPTED,
                        getIntent().getStringExtra(Constants.REMOTE_MSG_INVITER_TOKEN)
                );
            }
        });
        binding.imageRejectInvitation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendInvitationResponse(
                        Constants.REMOTE_MSG_INVITATION_REJECTED,
                        getIntent().getStringExtra(Constants.REMOTE_MSG_INVITER_TOKEN)
                );
            }
        });
    }

    private Bitmap getUserImage(String encodeImages){
        byte[] bytes= Base64.decode(encodeImages,Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
    }
    private  void sendInvitationResponse(String type, String receiverToken){
        try{
            JSONArray tokens= new JSONArray();
            tokens.put(receiverToken);
            JSONObject data= new JSONObject();
            JSONObject body= new JSONObject();
            data.put(Constants.REMOTE_MSG_TYPE,Constants.REMOTE_MSG_INVITATION_RESPONSE);
            data.put(Constants.REMOTE_MSG_INVITATION_RESPONSE,type);

            body.put(Constants.REMOTE_MSG_DATA,data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS,tokens);
            sendRemoteMessage(body.toString(),type);

        }catch (Exception ex){
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    private  void sendRemoteMessage(String remoteMessageBody, String type){
        ApiClient.getClient().create(ApiService.class).sendMessage(
                Constants.getRemoteMsgHeaders(),remoteMessageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if(response.isSuccessful()){
                    if(type.equals(Constants.REMOTE_MSG_INVITATION_ACCEPTED)){
                      try {
                            URL  serverURL=new URL("https://mit.jit.si");

                      }catch (Exception ex){
                          Toast.makeText(getApplicationContext(), ex.getMessage(), Toast.LENGTH_SHORT).show();

                      }

                    }else{
                        Toast.makeText(getApplicationContext(), "Từ chối cuộc gọi", Toast.LENGTH_SHORT).show();
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
    private BroadcastReceiver invitationResponseReceiver= new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type= intent.getStringExtra(Constants.REMOTE_MSG_INVITATION_RESPONSE);
            if(type!=null){
                if(type.equals(Constants.REMOTE_MSG_INVITATION_CANCELED)){
                    Toast.makeText(getApplicationContext(), "Hủy cuộc gọi", Toast.LENGTH_SHORT).show();
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