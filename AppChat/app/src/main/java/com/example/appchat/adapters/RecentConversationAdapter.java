package com.example.appchat.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appchat.databinding.ItemContainerReceivedMessageBinding;
import com.example.appchat.databinding.ItemContainerRecentConversionBinding;
import com.example.appchat.listeners.ConversionListener;
import com.example.appchat.models.ChatMessage;
import com.example.appchat.models.User;

import java.util.List;

public class RecentConversationAdapter  extends  RecyclerView.Adapter<RecentConversationAdapter.ConversionViewHolder>{

    private  final List<ChatMessage> chatMessages;
private  final ConversionListener conversionListener;
    public RecentConversationAdapter(List<ChatMessage> chatMessages, ConversionListener conversionListener) {
        this.chatMessages = chatMessages;
        this.conversionListener= conversionListener;
    }

    @NonNull
    @Override
    public ConversionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ConversionViewHolder(
                ItemContainerRecentConversionBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ConversionViewHolder holder, int position) {
        holder.setData(chatMessages.get(position));
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    class ConversionViewHolder extends RecyclerView.ViewHolder{
        ItemContainerRecentConversionBinding binding;
        ConversionViewHolder(ItemContainerRecentConversionBinding itemContainerRecentConversionBinding){
            super(itemContainerRecentConversionBinding.getRoot());
            binding=itemContainerRecentConversionBinding;

        }
        void setData(ChatMessage chatMessage){
            binding.imageProfile.setImageBitmap(getConversionImages(chatMessage.conversionImage));
            binding.textName.setText(chatMessage.conversionName);
            binding.textRecentMessage.setText(chatMessage.message);
            binding.getRoot().setOnClickListener(v->{
                User user= new User();
                user.id=chatMessage.conversionId;
                user.name=chatMessage.conversionName;
                user.images= chatMessage.conversionImage;
                conversionListener.onConversionClicked(user);
            });
        }
    }


    private Bitmap getConversionImages(String encodeImages){
        byte[] bytes= Base64.decode(encodeImages,Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
    }
}
