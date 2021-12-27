package com.example.appchat.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import com.example.appchat.R;
import com.example.appchat.adapters.ChatAdapter;
import com.example.appchat.databinding.ActivityChatBinding;
import com.example.appchat.listeners.UserListener;
import com.example.appchat.models.ChatMessage;
import com.example.appchat.models.User;
import com.example.appchat.network.ApiClient;
import com.example.appchat.network.ApiService;
import com.example.appchat.utilites.Constants;
import com.example.appchat.utilites.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.protobuf.Value;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends BaseActivity  {

    private ActivityChatBinding binding;
    private User receiverUser;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore dataBase;
    private  String conversationId=null;
    private  Boolean isReceiverAvailable=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Intent intent = getIntent();
    //    User user= (User) intent.getSerializableExtra(Constants.KEY_USER);
        setListeners();
        loadReceiverDetails();
        init();
        listenMessages();
    }
    private Bitmap getBitMapFromEncodeString(String encodeImage){
        if(encodeImage!=null){
            byte[]bytes = Base64.decode(encodeImage,Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        }else{
            return null;
        }

    }
    private  void init(){
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages= new ArrayList<>();
        chatAdapter= new ChatAdapter(
                chatMessages,getBitMapFromEncodeString(receiverUser.images),
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        binding.chatRecyclerView.setAdapter(chatAdapter);
        dataBase= FirebaseFirestore.getInstance();
    }


    private  void sendMessage(){
       if(!binding.inputMessage.getText().equals("") && binding.inputMessage.getText().length()>0){
           HashMap<String,Object> message= new HashMap<>();
           message.put(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID));
           message.put(Constants.KEY_RECEIVER_ID,receiverUser.id);
           message.put(Constants.KEY_MESSAGE,binding.inputMessage.getText().toString());
           message.put(Constants.KEY_TIMESTAMP, new Date());
           dataBase.collection(Constants.KEY_COLLECTION_CHAT).add(message);
           if(conversationId!=null){
               updateConversion(binding.inputMessage.getText().toString());
           }else{
               HashMap<String,Object>conversion= new HashMap<>();
               conversion.put(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID));
               conversion.put(Constants.KEY_SENDER_NAME,preferenceManager.getString(Constants.KEY_NAME));
               conversion.put(Constants.KEY_SENDER_IMAGE,preferenceManager.getString(Constants.KEY_IMAGE));
               conversion.put(Constants.KEY_RECEIVER_ID,receiverUser.id);
               conversion.put(Constants.KEY_RECEIVER_NAME,receiverUser.name);
               conversion.put(Constants.KEY_RECEIVER_IMAGE,receiverUser.images);
               conversion.put(Constants.KEY_LAST_MESSAGE,binding.inputMessage.getText().toString());
               conversion.put(Constants.KEY_TIMESTAMP,new Date());
               addConversion(conversion);
           }
           if(!isReceiverAvailable){
               try {
                   JSONArray tokens= new JSONArray();
                   tokens.put(receiverUser.token);
                   JSONObject data= new JSONObject();
                   data.put(Constants.KEY_USER_ID,preferenceManager.getString(Constants.KEY_USER_ID));
                   data.put(Constants.KEY_NAME,preferenceManager.getString(Constants.KEY_NAME));
                   data.put(Constants.KEY_FCM_TOKEN,preferenceManager.getString(Constants.KEY_FCM_TOKEN));
                   data.put(Constants.KEY_MESSAGE,binding.inputMessage.getText().toString());
                   JSONObject body=new JSONObject();
                   body.put(Constants.REMOTE_MSG_DATA,data);
                   body.put(Constants.REMOTE_MSG_REGISTRATION_IDS,tokens);
                   sendNotification(body.toString());
               }catch (Exception ex){
                   showToast(ex.getMessage());
               }
           }
           binding.inputMessage.setText(null);
       }
    }

    private  void showToast(String mes){
        Toast.makeText(getApplicationContext(), mes, Toast.LENGTH_SHORT).show();
    }


    private  void sendNotification(String messageBody){
        ApiClient.getClient().create(ApiService.class).sendMessage(
                Constants.getRemoteMsgHeaders(),
                messageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if(response.isSuccessful()){
                    try{
                        if(response.body()!=null){
                            JSONObject responseJSon=new JSONObject(response.body());
                            JSONArray results= responseJSon.getJSONArray("results");
                            if(responseJSon.getInt("failure")==1){
                                JSONObject error=(JSONObject) results.get(0);
                                showToast(error.getString("error"));
                                return ;
                            }
                        }
                    }catch(JSONException ex){
                        ex.printStackTrace();
                    }
                    showToast("...");
                }else{
                    showToast("Lỗi:"+response.code());
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                    showToast(t.getMessage());
            }
        });
    }

    private  void listenAvailabilityOfReceiver(){
        dataBase.collection(Constants.KEY_COLLECTION_USERS).document(
                receiverUser.id
        ).addSnapshotListener(ChatActivity.this,(value, error) ->{
            if(error!=null){
                return;
            }
            if(value!=null){
                if(value.getLong(Constants.KEY_AVAILABILITY)!=null){
                    int availability= Objects.requireNonNull(
                            value.getLong(Constants.KEY_AVAILABILITY)
                    ).intValue();
                    isReceiverAvailable= availability==1;
                }
                receiverUser.token=value.getString(Constants.KEY_FCM_TOKEN);
                if(receiverUser.images==null){
                    receiverUser.images=value.getString(Constants.KEY_IMAGE);
                    chatAdapter.setReceiverProfileImage(getBitMapFromEncodeString(receiverUser.images));
                    chatAdapter.notifyItemRangeChanged(0,chatMessages.size());
                }
            }
            if(isReceiverAvailable){
                binding.textAvailability.setVisibility(View.VISIBLE);
            }else{
                binding.textAvailability.setVisibility(View.GONE);
            }
            binding.imageCall.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(isReceiverAvailable){

                    }else{
                        showToast("Người dùng "+receiverUser.name+" không có online mà gọi cái gì");
                    }
                }
            });
            binding.imageCallVideo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(isReceiverAvailable){
                        Intent intent= new Intent(getApplicationContext(),OutgoingInvitationActivity.class);
                        intent.putExtra("user",receiverUser);
                        intent.putExtra("type","video");
                        startActivity(intent);
                    }else{
                        showToast("Người dùng "+receiverUser.name+" không có online mà gọi cái gì");
                    }
                }
            });
        } );
    }


    private  void listenMessages(){
        dataBase.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID,receiverUser.id)
                .addSnapshotListener(eventListener);
        dataBase.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID,receiverUser.id)
                .whereEqualTo(Constants.KEY_RECEIVER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    private  final EventListener<QuerySnapshot> eventListener=(value, error)->{
        if(error!=null){
            return ;
        }if(value!=null){
            int count=chatMessages.size();
            for(DocumentChange documentChange:value.getDocumentChanges()){
                if(documentChange.getType()==DocumentChange.Type.ADDED){
                    ChatMessage chatMessage= new ChatMessage();
                    chatMessage.senderId=documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.receiverId=documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    chatMessage.message=documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chatMessage.dateTime=getDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chatMessage.dateObject=documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessages.add(chatMessage);
                }
            }
            Collections.sort(chatMessages,(obj1,obj2)->obj1.dateObject.compareTo(obj2.dateObject));
            if(count==0){
                chatAdapter.notifyDataSetChanged();
            }else{
                chatAdapter.notifyItemRangeInserted(chatMessages.size(),chatMessages.size());
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size()-1);
            }
            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);
        if(conversationId==null){
            checkForConversation();
        }
    };
    private  void loadReceiverDetails(){
        receiverUser=(User)getIntent().getSerializableExtra(Constants.KEY_USER);
        binding.textName.setText(receiverUser.name);
    }

    private void setListeners(){
        binding.imageBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent  intent= new Intent(getApplicationContext(),MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);

                startActivity(intent);
            }
        });
        binding.layoutSend.setOnClickListener(v->sendMessage());
    }
    private  String getDateTime(Date date){
        return new SimpleDateFormat("MM dd,yyyy -  hh:mm a", Locale.getDefault()).format(date);
    }

    private  void addConversion(HashMap<String, Object> conversion){
        dataBase.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .add(conversion)
                .addOnSuccessListener(documentReference -> conversationId=documentReference.getId());
    }

    private  void updateConversion(String message){
        DocumentReference documentReference=
                dataBase.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversationId);
        documentReference.update(
                Constants.KEY_LAST_MESSAGE,message,
                Constants.KEY_TIMESTAMP, new Date()
        );
    }


    private  void checkForConversation (){
        if(chatMessages.size()!=0){
            checkForConverSationRemotely(
                    preferenceManager.getString(Constants.KEY_USER_ID),
                    receiverUser.id
            );
            checkForConverSationRemotely(
                    receiverUser.id,
                    preferenceManager.getString(Constants.KEY_USER_ID)
            );
        }
    }

    private  void checkForConverSationRemotely(String senderId,String receiverId){
        dataBase.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID,senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID,receiverId)
                .get().addOnCompleteListener(conversationOnCompleteListener);
    }
    private  final OnCompleteListener<QuerySnapshot> conversationOnCompleteListener= task->{
        if(task.isSuccessful()&& task.getResult()!=null && task.getResult().getDocuments().size()>0){
            DocumentSnapshot documentSnapshot=task.getResult().getDocuments().get(0);
            conversationId=documentSnapshot.getId();
        }
    };


    @Override
    protected void onResume() {
        super.onResume();
        listenAvailabilityOfReceiver();
    }

}