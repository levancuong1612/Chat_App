package com.example.appchat.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.appchat.R;
import com.example.appchat.adapters.UserAdapter;
import com.example.appchat.databinding.ActivityUsersBinding;
import com.example.appchat.listeners.UserListener;
import com.example.appchat.models.User;
import com.example.appchat.utilites.Constants;
import com.example.appchat.utilites.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class UsersActivity extends BaseActivity  implements UserListener {

    private ActivityUsersBinding binding;
    private PreferenceManager preferenceManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityUsersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager= new PreferenceManager(getApplicationContext());
        setListeners();
        getUsers();
    }
    private  void setListeners(){
        binding.imageBack.setOnClickListener(v->onBackPressed());
    }
    public  void getUsers(){
        loadImg(true);
        FirebaseFirestore database= FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .get()
                .addOnCompleteListener(task -> {
                    loadImg(false);
                    String currentUserID=preferenceManager.getString(Constants.KEY_USER_ID);
                    if(task.isSuccessful() && task.getResult()!=null){
                        List<User> users= new ArrayList<>();
                        for(QueryDocumentSnapshot documentSnapshot: task.getResult()){
                            if(currentUserID.equals(documentSnapshot.getId())){
                                continue;
                            }
                            User user= new User();
                            user.name=documentSnapshot.getString(Constants.KEY_NAME);
                            user.email=documentSnapshot.getString(Constants.KEY_EMAIL);
                            user.images=documentSnapshot.getString(Constants.KEY_IMAGE);
                            user.token=documentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                            user.id=documentSnapshot.getId();
                            users.add(user);
                        }
                        if(users.size()>0){
                            UserAdapter userAdapter= new UserAdapter(users,this);
                            binding.userRecyclerView.setAdapter(userAdapter);
                            binding.userRecyclerView.setVisibility(View.VISIBLE);
                        }else{
                            showErrorMessage();
                        }
                    }else{
                        showErrorMessage();
                    }
                });
    }
    private  void showErrorMessage(){
        binding.textErrorMessage.setText(String.format("%s","No user"));
        binding.textErrorMessage.setVisibility(View.VISIBLE);
    }
    private  void loadImg(Boolean isLoading){
        if(isLoading){
            binding.progressBar.setVisibility(View.VISIBLE);
        }else{
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onUserClicked(User user) {
        Intent intent= new Intent(getApplicationContext(),ChatActivity.class);
        intent.putExtra(Constants.KEY_USER,user);
        startActivity(intent);
        finish();
    }

    @Override
    public void initiateAudioMeeting(User user) {

    }

    @Override
    public void initiateVideoMeeting(User user) {

    }
}