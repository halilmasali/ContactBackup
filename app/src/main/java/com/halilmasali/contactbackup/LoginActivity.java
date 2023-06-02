package com.halilmasali.contactbackup;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextInputEditText userName;
    private TextInputEditText password;
    private Button button;
    private TextView helpLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mAuth = FirebaseAuth.getInstance();
        userName = findViewById(R.id.userNameText);
        password = findViewById(R.id.passwordText);
        button = findViewById(R.id.signInButton);
        helpLabel = findViewById(R.id.textHelp);
    }

    public void loginButton(View view) {
        String email = String.valueOf(userName.getText());
        String Password = String.valueOf(password.getText());

        if (!email.equals("") && !Password.equals("")) {

            if (button.getText().equals("Sign In")) {
                mAuth.signInWithEmailAndPassword(email, Password)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                // Sign in success, update UI with the signed-in user's information
                                Log.d("LOG", "signInWithEmail:success");
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                // If sign in fails, display a message to the user.
                                Log.w("LOG", "signInWithEmail:failure", task.getException());
                                Toast.makeText(LoginActivity.this, Objects.requireNonNull(task.getException()).getLocalizedMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                mAuth.createUserWithEmailAndPassword(email,Password)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()){
                                Toast.makeText(LoginActivity.this,"Register Successfully",Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(LoginActivity.this, Objects.requireNonNull(task.getException()).getLocalizedMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        }
    }

    public void helpLabel(View view) {
        if (button.getText().equals("Sign In")) {
            button.setText("Sign Up");
            helpLabel.setText("You have an account?  Sign In");
        } else if (button.getText().equals("Sign Up")) {
            button.setText("Sign In");
            helpLabel.setText("You don't have an account?  Sign Up");
        }
    }

}