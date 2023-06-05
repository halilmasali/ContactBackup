package com.halilmasali.contactbackup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.search.SearchBar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    private RecyclerView recyclerViewContacts;
    private ContactAdapter contactAdapter;
    private List<ContactViewModel> contacts;
    TextView userMail, personCount, lastUpdate;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        userMail = findViewById(R.id.userMail);
        personCount = findViewById(R.id.personCount);
        lastUpdate = findViewById(R.id.lastUpdate);
        recyclerViewContacts = findViewById(R.id.recyclerView);
        recyclerViewContacts.setLayoutManager(new LinearLayoutManager(this));

        contacts = new ArrayList<>();
        contactAdapter = new ContactAdapter(contacts);
        recyclerViewContacts.setAdapter(contactAdapter);

        if (checkPermission()) {
            readContacts();
        } else {
            requestPermission();
        }

        checkAuthVerification();
        SearchBar searchBar = findViewById(R.id.search_bar);
        searchBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.logout) {
                logoutDialog();
            }
            return true;
        });
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                readContacts();
            } else {
                Toast.makeText(this, "Rehber erişimi izni reddedildi.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint({"Range", "NotifyDataSetChanged"})
    private void readContacts() {
        Cursor cursor = getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                null,
                null,
                null,
                ContactsContract.Contacts.DISPLAY_NAME + " ASC"
        );

        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                String displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                Cursor phoneCursor = getContentResolver().query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        new String[]{contactId},
                        null
                );

                String phoneNumber = "";
                if (phoneCursor != null && phoneCursor.moveToFirst()) {
                    phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    phoneCursor.close();
                }

                contacts.add(new ContactViewModel(displayName, phoneNumber));
            }
            personCount.setText(MessageFormat.format("{0} Person", cursor.getCount()));
            cursor.close();
            contactAdapter.notifyDataSetChanged();
        } else {
            Toast.makeText(this, "Rehberde kişi bulunamadı.", Toast.LENGTH_SHORT).show();
        }
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