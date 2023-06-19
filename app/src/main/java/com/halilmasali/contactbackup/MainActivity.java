package com.halilmasali.contactbackup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.search.SearchBar;
import com.google.android.material.search.SearchView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    private RecyclerView recyclerViewContacts, recyclerViewSearch;
    private ContactAdapter contactAdapter, searchedContactAdaptor;
    private List<ContactViewModel> contacts;
    private List<ContactViewModel> searchedContacts;
    private List<ContactViewModel> cloudContacts = new ArrayList<>();
    private TextView userMail, personCount, lastUpdate;
    private SwipeRefreshLayout swipeRefresh;
    private SharedPreferences sharedPreferences;
    private ImageView imageSync;
    private FirebaseFirestore firestore;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        userMail = findViewById(R.id.userMail);
        personCount = findViewById(R.id.personCount);
        lastUpdate = findViewById(R.id.lastUpdate);
        imageSync = findViewById(R.id.imageSync);
        recyclerViewContacts = findViewById(R.id.recyclerView);
        recyclerViewContacts.setLayoutManager(new LinearLayoutManager(this));
        sharedPreferences = getSharedPreferences("lastUpdate", Context.MODE_PRIVATE);
        recyclerViewSearch = findViewById(R.id.recyclerViewSearch);
        recyclerViewSearch.setLayoutManager(new LinearLayoutManager(this));
        searchedContacts = new ArrayList<>();
        searchedContactAdaptor = new ContactAdapter(searchedContacts);
        recyclerViewSearch.setAdapter(searchedContactAdaptor);

        contacts = new ArrayList<>();
        contactAdapter = new ContactAdapter(cloudContacts);
        recyclerViewContacts.setAdapter(contactAdapter);

        checkAuthVerification();
        firestoreReadData();
        if (checkPermission()) {
            readContacts();
        } else {
            requestPermission();
        }


        SearchBar searchBar = findViewById(R.id.search_bar);
        searchBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.logout) {
                logoutDialog();
            } else if (item.getItemId() == R.id.deleteContacts) {
                deleteDialog();
            } else if (item.getItemId() == R.id.about) {
                aboutDialog();
            }
            return true;
        });
        SearchView searchView = findViewById(R.id.search_view);
        searchView.getEditText().setOnEditorActionListener((v, actionId, event) -> {

            searchList(String.valueOf(searchView.getText()));
            searchBar.setText(searchView.getText());
            return false;
        });

        //region Swipe Refresh
        swipeRefresh = findViewById(R.id.swipeRefreshLayout);
        swipeRefresh.setOnRefreshListener(() -> {
            checkSyncStatus();
            firestoreReadData();
            swipeRefresh.setRefreshing(false);
        });
        //endregion
        lastUpdate.setText(sharedPreferences.getString("lastUpdate", "Last Update:"));
    }

    @SuppressLint("NotifyDataSetChanged")
    private void searchList(String newText) {
        searchedContacts.clear();
        String searchText = newText.toLowerCase();
        if (!searchText.isEmpty()) {
            for (ContactViewModel item : cloudContacts) {
                if (item.getName().toLowerCase().contains(searchText)) {
                    searchedContacts.add(item);
                }
            }
            if (searchedContactAdaptor != null) {
                searchedContactAdaptor.notifyDataSetChanged();
            }
        }
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                readContacts();
            } else {
                Toast.makeText(this, "Contact access permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint({"Range"})
    private void readContacts() {
        contacts.clear();
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

                contacts.add(new ContactViewModel(displayName, phoneNumber, contactId));
            }
            cursor.close();
        } else {
            Toast.makeText(this, "Contact not found.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean containsContactId(String contactId) {
        for (ContactViewModel contact : cloudContacts) {
            if (contact.getContactId().equals(contactId)) {
                return true;
            }
        }
        return false;
    }

    private boolean changedContact(ContactViewModel contact) {
        for (ContactViewModel contacts : cloudContacts) {
            if (contacts.getContactId().equals(contact.getContactId())) {
                if (!contacts.getName().equals(contact.getName()) ||
                        !contacts.getPhoneNumber().equals(contact.getPhoneNumber())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void checkAuthVerification() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        } else {
            userMail.setText(currentUser.getEmail());
            userId = mAuth.getUid();
            firestore = FirebaseFirestore.getInstance();
        }

    }

    public void firestoreSyncData(ContactViewModel contactViewModel) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", contactViewModel.getName());
        data.put("phone", contactViewModel.getPhoneNumber());

        if (userId != null) {
            firestore.collection("users")
                    .document(userId)
                    .collection("contacts")
                    .document(contactViewModel.getContactId())
                    .set(data)
                    .addOnSuccessListener(documentReference -> {
                        Log.d("TAG", "DocumentSnapshot successfully written!");
                        //Toast.makeText(this, "Save successfully", Toast.LENGTH_LONG).show();
                    })
                    .addOnFailureListener(e -> Log.w("TAG", "Error writing document", e));
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void firestoreReadData() {
        if (userId != null) {
            cloudContacts.clear();
            firestore.collection("users").document(userId)
                    .collection("contacts").get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            cloudContacts.add(new ContactViewModel(
                                    Objects.requireNonNull(document.get("name")).toString(),
                                    Objects.requireNonNull(document.get("phone")).toString(), document.getId()));

                        }
                        Log.d("TAG", "Cloud read success");
                        personCount.setText(MessageFormat.format("{0} Person", cloudContacts.size()));
                        contactAdapter.notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> {
                        Log.w("TAG", "Error reading document", e);
                        Toast.makeText(this, "Error reading document", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void deleteDialog() {
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
        dialogBuilder.setTitle("Delete");
        dialogBuilder.setMessage("Do you want to delete all contacts ?");
        dialogBuilder.setCancelable(false);
        dialogBuilder.setPositiveButton("Yes", (dialog, which) -> {
            deleteContacts();
        });
        dialogBuilder.setNegativeButton("No", (dialog, which) -> {
            dialog.dismiss();
        });
        dialogBuilder.show();
    }

    private void deleteContacts() {
        firestore.collection("users").document(userId).collection("contacts")
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                document.getReference().delete();
                            }
                            firestoreReadData();
                            imageSync.setImageResource(R.drawable.sync_problem_24);
                        }
                    } else {
                        // Handle error
                        Exception exception = task.getException();
                        if (exception != null) {
                            exception.printStackTrace();
                        }
                    }
                });
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

    private void checkSyncStatus() {
        readContacts();
        List<ContactViewModel> unSyncContacts = new ArrayList<>();
        for (ContactViewModel contact : contacts) {
            if (!containsContactId(contact.getContactId())) {
                unSyncContacts.add(contact);
            } else if (changedContact(contact)) {
                unSyncContacts.add(contact);
            }
        }
        if (unSyncContacts.size() > 0) {
            MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
            dialogBuilder.setTitle("Sync Request");
            dialogBuilder.setMessage(unSyncContacts.size() + " change found. Do you want to sync your contacts on Cloud ?");
            dialogBuilder.setCancelable(false);
            dialogBuilder.setPositiveButton("Yes", (dialog, which) -> {
                for (ContactViewModel contact : unSyncContacts) {
                    firestoreSyncData(contact);
                }
                Date currentDate = new Date();
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy - HH:mm", Locale.getDefault());
                String formattedDateTime = dateFormat.format(currentDate);
                String time = "Last Update:" + formattedDateTime;
                sharedPreferences.edit().putString("lastUpdate", time).apply();
                lastUpdate.setText(time);
                imageSync.setImageResource(R.drawable.cloud_sync_24);
                firestoreReadData();
            });
            dialogBuilder.setNegativeButton("No", (dialog, which) -> {
                dialog.dismiss();
                imageSync.setImageResource(R.drawable.sync_problem_24);
            });
            dialogBuilder.show();
        } else if (cloudContacts == null || cloudContacts.size() < 1) {
            for (ContactViewModel contact : contacts) {
                firestoreSyncData(contact);
            }
        } else {
            imageSync.setImageResource(R.drawable.cloud_sync_24);
            Toast.makeText(this, "Contact backup up-to date", Toast.LENGTH_LONG).show();
        }
    }

    private void aboutDialog() {
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
        dialogBuilder.setTitle("About");
        dialogBuilder.setIcon(R.drawable.info_24);
        dialogBuilder.setMessage("Developed By:\n" +
                "Halil İbrahim Maşalı 192119017 N.Ö. \n" +
                "Kaan Atakan Yılmaz 192113050 N.Ö.");
        dialogBuilder.setCancelable(true);
        dialogBuilder.show();
    }
}