package com.halilmasali.contactbackup;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.search.SearchBar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    TextView userMail, personCount, lastUpdate;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        userMail = findViewById(R.id.userMail);
        personCount = findViewById(R.id.personCount);
        lastUpdate = findViewById(R.id.lastUpdate);
        checkAuthVerification();
        SearchBar searchBar = findViewById(R.id.search_bar);
        searchBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.logout) {
                logoutDialog();
            }
            return true;
        });
    }


    private void checkAuthVerification() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        } else
            userMail.setText(currentUser.getEmail());
    }

    private void logoutDialog() {
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
        dialogBuilder.setTitle("Logout");
        dialogBuilder.setMessage("Do you want to logout ?");
        dialogBuilder.setCancelable(false);
        dialogBuilder.setPositiveButton("Yes", (dialog, which) -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finishAffinity();
        });
        dialogBuilder.setNegativeButton("No", (dialog, which) -> {
            dialog.dismiss();
        });
        dialogBuilder.show();
    }
}