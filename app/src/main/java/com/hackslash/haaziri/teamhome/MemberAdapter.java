package com.hackslash.haaziri.teamhome;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.hackslash.haaziri.R;
import com.hackslash.haaziri.firebase.FirebaseVars;
import com.hackslash.haaziri.models.Session;
import com.hackslash.haaziri.models.SessionAttendee;
import com.hackslash.haaziri.models.Team;
import com.hackslash.haaziri.models.UserProfile;

import java.util.ArrayList;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.ViewHolder> {
    private static final String TAG="MemberAdapter";
    private final Context mContext;
    private ArrayList<String> memberIds;

    public MemberAdapter(ArrayList<String> memberIds,Context mContext) {
        this.mContext = mContext;
        this.memberIds = memberIds;
    }

    @NonNull
    @Override
    public MemberAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View joinMembers = LayoutInflater.from(parent.getContext()).inflate(R.layout.attendee_card,parent,false);
        return new MemberAdapter.ViewHolder(joinMembers);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberAdapter.ViewHolder holder, int position) {
        holder.fetchMemberUid(memberIds.get(position));
    }

    @Override
    public int getItemCount() {
        return memberIds.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView memberName;
        private CardView memberCard;
        private ImageView memberImg;
        private ImageView icon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            memberName = itemView.findViewById(R.id.attendeename);
            memberCard = itemView.findViewById(R.id.attendeecardview);
            memberImg = itemView.findViewById(R.id.userimg);
            icon = itemView.findViewById(R.id.ticktv);
        }

        public void fetchMemberUid(String memberUid) {
            FirebaseVars.mRootRef.child("/teams/members/").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists())
                        setData(snapshot.getValue(UserProfile.class));
                }



                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.d(TAG, error.getMessage());
                }
            });
        }

        private void setData(UserProfile value) {
            memberName.setText(value.getName());

        }
    }
}
