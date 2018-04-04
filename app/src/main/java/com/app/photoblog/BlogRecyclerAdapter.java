package com.app.photoblog;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Adil khan on 3/5/2018.
 */

public class BlogRecyclerAdapter extends RecyclerView.Adapter<BlogRecyclerAdapter.ViewHolder> {

    public List<BlogPost> blog_list;
    public Context context;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;

    public BlogRecyclerAdapter(List<BlogPost> blog_list) {

        this.blog_list = blog_list;

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.blog_list_item, parent, false);
        context = parent.getContext();
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {

        holder.setIsRecyclable(false);

        final String blogPostId = blog_list.get(position).BlogPostId;
        final String currentUserId = firebaseAuth.getCurrentUser().getUid();

        String desc_data = blog_list.get(position).getDesc();
        holder.setDescText(desc_data);

        String image_url = blog_list.get(position).getImage_url();
        String thumb_url = blog_list.get(position).getImage_thumb();
        holder.setBlogImage(image_url, thumb_url);

        String user_id = blog_list.get(position).getUser_id();
        firebaseFirestore.collection("Users").document(user_id).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                String user_name = documentSnapshot.getString("name");
                String user_image = documentSnapshot.getString("image");
                holder.setUserInfo(user_name, user_image);
            }
        });


        long millisecond = blog_list.get(position).getTimestamp().getTime();
        String dateString = DateFormat.format("MM/dd/yyyy", new Date(millisecond)).toString();
        holder.setPostTime(dateString);

        firebaseFirestore.collection("Posts/" + blogPostId + "/Likes").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                if (!documentSnapshots.isEmpty()) {
                    int count = documentSnapshots.size();
                    holder.updateLikesCount(count);
                } else {
                    holder.updateLikesCount(0);
                }
            }
        });

        firebaseFirestore.collection("Posts/" + blogPostId + "/Likes").document(currentUserId).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {

                if (documentSnapshot.exists()) {
                    holder.blog_like_img.setImageResource(R.mipmap.action_like_accent);
                } else {
                    holder.blog_like_img.setImageResource(R.mipmap.action_like_grey);
                }
            }
        });

        holder.blog_like_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                firebaseFirestore.collection("Posts/" + blogPostId + "/Likes").document(currentUserId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (!task.getResult().exists()) {
                            Map<String, Object> likesMap = new HashMap<>();
                            likesMap.put("timestamp", FieldValue.serverTimestamp());

                            firebaseFirestore.collection("Posts/" + blogPostId + "/Likes").document(currentUserId).set(likesMap);
                            holder.blog_like_img.setImageResource(R.mipmap.action_like_accent);

                        } else {
                            firebaseFirestore.collection("Posts/" + blogPostId + "/Likes").document(currentUserId).delete();
                            holder.blog_like_img.setImageResource(R.mipmap.action_like_grey);
                        }
                    }
                });


            }
        });

    }

    @Override
    public int getItemCount() {
        return blog_list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private View mView;
        private TextView blog_username, blog_date, blog_desc, blog_like_count;
        private ImageView image_thumb, blog_user_image, blog_like_img;


        public ViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
            blog_like_img = mView.findViewById(R.id.blog_like_img);
        }

        public void setDescText(String descText) {
            blog_desc = mView.findViewById(R.id.blog_desc);
            blog_desc.setText(descText);
        }

        public void setBlogImage(String downloadUri, String thumbUri) {

            image_thumb = mView.findViewById(R.id.blog_image);

            RequestOptions placeHolderOption = new RequestOptions();
            placeHolderOption.placeholder(R.drawable.image_placeholder);
            Glide.with(context).setDefaultRequestOptions(placeHolderOption).load(downloadUri)
                    .thumbnail(Glide.with(context).load(thumbUri))
                    .into(image_thumb);
        }

        public void setUserInfo(String username, String downloadUri) {
            blog_username = mView.findViewById(R.id.blog_user_name);
            blog_username.setText(username);

            blog_user_image = mView.findViewById(R.id.blog_user_image);

            RequestOptions placeHolderOption = new RequestOptions();
            placeHolderOption.placeholder(R.drawable.profile_placeholder);

            Glide.with(context).applyDefaultRequestOptions(placeHolderOption).load(downloadUri).into(blog_user_image);
        }

        public void setPostTime(String date) {
            blog_date = mView.findViewById(R.id.blog_date);
            blog_date.setText(date);
        }

        public void updateLikesCount(int count) {
            blog_like_count = mView.findViewById(R.id.blog_like_count);
            blog_like_count.setText(count + " Likes");
        }

    }

}
