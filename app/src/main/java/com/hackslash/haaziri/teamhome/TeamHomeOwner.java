package com.hackslash.haaziri.teamhome;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAssignedNumbers;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.hackslash.haaziri.R;
import com.hackslash.haaziri.activitydialog.ActivityDialog;
import com.hackslash.haaziri.firebase.FirebaseVars;
import com.hackslash.haaziri.models.Session;
import com.hackslash.haaziri.models.Team;
import com.hackslash.haaziri.models.UserProfile;
import com.hackslash.haaziri.sessions.CurrentSessionActivity;
import com.hackslash.haaziri.utils.Constants;
import com.hackslash.haaziri.utils.MotionToastUtitls;

import java.util.ArrayList;

public class TeamHomeOwner extends AppCompatActivity {

    private static final String TAG = "TeamHomeOwner";
    //Temporary Session id (TO BE DELETED)
    //TODO: delete this before implementing actual session feature
    String temporarySessionId = "mySession";
    private TextView teamCodeTv;
    private String teamCode = "";
    private Button takeHaaziriBtn;
    private TextView recentSessionsBtn;
    private Context mContext = this;
    private ActivityDialog dialog;
    private String oldName = "My phone";
    private Team selectedTeam;
    private String teamPath = "";
    private TextView teamNameTv;
    private TextView recentSessionLabel;
    private ImageView backBtn;
    private TextView memberName;
    private ArrayList<String> memberIds;
    private MemberAdapter memberAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_home_owner);

        //making status bar white with black icons
        getWindow().setStatusBarColor(getColor(R.color.home_status_bar_color));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        Intent intent = getIntent();
        teamCode = intent.getStringExtra(Constants.TEAM_CODE_KEY);
        teamPath = "/teams/" + teamCode + "/";
        initVars();

        setupListeners();

        fetchTeamDetails();

        fetchMember();
    }
    //fetching member details and using loop to add member details in memberIds arryalist
    private void fetchMember() {
        DatabaseReference mRootRef = FirebaseVars.mRootRef;
        mRootRef.child(teamPath + "/members/");
        mRootRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserProfile memberDetails = snapshot.child("profile").getValue(UserProfile.class);
                for (DataSnapshot uid : snapshot.child(teamPath + "/members/").getChildren())
                    memberIds.add(uid.getValue(String.class));
                memberAdapter.notifyDataSetChanged();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                dialog.hideDialog();
                MotionToastUtitls.showErrorToast(mContext, "Error", "Some error occurred in fetching member details");
                Log.d(TAG, error.getMessage());

            }
        });
    }

    /**
     * Function to fetch the selected team details
     */
    private void fetchTeamDetails() {
        dialog.setTitle("Fetching team details");
        dialog.setMessage("Please wait while we fetch the team details");
        dialog.showDialog();
        FirebaseVars.mRootRef.child(teamPath).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                selectedTeam = snapshot.getValue(Team.class);
                teamNameTv.setText(selectedTeam.getTeamName());
                dialog.hideDialog();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                dialog.hideDialog();
                MotionToastUtitls.showErrorToast(mContext, "Error", "Some error occurred in fetching team details");
                Log.d(TAG, error.getMessage());
            }
        });
    }

    private void setupListeners() {
        takeHaaziriBtn.setOnClickListener(v -> {
            dialog.setTitle("Setting up new session");
            dialog.setMessage("Please wait while we setup new session");
            dialog.showDialog();
            makeDeviceDiscoverable();


        });
        backBtn.setOnClickListener(v -> finish());
        recentSessionsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, OwnerRecentSessionsActivity.class);
            intent.putExtra(Constants.TEAM_NAME_KEY, teamNameTv.getText().toString());
            intent.putExtra(Constants.TEAM_CODE_KEY, teamCode);
            startActivity(intent);
        });
    }

    private void makeDeviceDiscoverable() {
        int requestCode = 1;
        Intent discoverableIntent =
                new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivityForResult(discoverableIntent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 1) {
            if (resultCode != RESULT_CANCELED) {
                setupBluetooth();
            } else dialog.hideDialog();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void setupBluetooth() {
        final BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        //turning bluetooth on of the device;
        BluetoothAdapter bluetoothAdapter = manager.getAdapter();
        oldName = bluetoothAdapter.getName();
        Handler nameChangeHandler = new Handler();
        //changing name of bluetooth device to session
        nameChangeHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                bluetoothAdapter.setName(temporarySessionId);
                if (bluetoothAdapter.getName().equals(temporarySessionId)) {
                    setupNewSession();
                } else {
                    nameChangeHandler.postDelayed(this, 500);
                }
            }
        }, 500);
        bluetoothAdapter.enable();

    }

    private void setupNewSession() {
        Session session = new Session(temporarySessionId, System.currentTimeMillis());
        FirebaseVars.mRootRef.child(teamPath + "/sessions/" + temporarySessionId).setValue(session).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isComplete())
                    startHaaziri();
                else {
                    MotionToastUtitls.showErrorToast(mContext, "Error", "Some error occurred while creating your new session");
                    Log.d(TAG, task.getException().toString());
                    dialog.hideDialog();
                    cleanUp();
                }
            }
        });
    }


    /**
     * function that does the clean up for some error
     * such as changing name of bluetooth
     */
    private void cleanUp() {
        //TODO: Implement the function to handle any error and do the cleanup process
    }

    private void startHaaziri() {
        FirebaseVars.mRootRef.child(teamPath + "currentSessionId/").setValue(temporarySessionId).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    dialog.hideDialog();
                    Intent intent = new Intent(mContext, CurrentSessionActivity.class);
                    intent.putExtra(Constants.TEAM_CODE_KEY, teamCode);
                    intent.putExtra(Constants.TEAM_NAME_KEY, selectedTeam.getTeamName());
                    intent.putExtra(Constants.SESSION_CODE_KEY, temporarySessionId);
                    startActivity(intent);
                } else {
                    MotionToastUtitls.showErrorToast(mContext, "Error", "Some error occurred while creating your new session");
                    Log.d(TAG, task.getException().toString());
                    dialog.hideDialog();
                    cleanUp();
                }
            }
        });
    }

    private void initVars() {
        teamCodeTv = findViewById(R.id.teamCodeTv);
        teamCodeTv.setText("Team Code: " + teamCode);
        takeHaaziriBtn = findViewById(R.id.takeHaaziriBtn);
        Toolbar toolbar = findViewById(R.id.toolbar);
        teamNameTv = toolbar.findViewById(R.id.teamNameTv);
        backBtn = toolbar.findViewById(R.id.backBtn);
        recentSessionLabel = toolbar.findViewById(R.id.recentSessionsLabel);
        recentSessionLabel.setText("Team Members");
        recentSessionsBtn = findViewById(R.id.recentActivityBtn);
        dialog = new ActivityDialog(mContext);
        dialog.setCancelable(false);
    }
}