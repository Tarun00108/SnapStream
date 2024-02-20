package com.example.snapstream;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import java.util.concurrent.Executor;

public class LoginFragment extends Fragment {
    String TAG = "LoginFragment";
    private TextView next, ForgetPassword;
    private EditText email, password;
    private Button Login;
    FirebaseAuth auth;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate( R.layout.fragment_login, container, false );
        auth = FirebaseAuth.getInstance();
        next = view.findViewById( R.id.newaccount );
        email = view.findViewById( R.id.email1 );
        password = view.findViewById( R.id.password1 );
        Login = view.findViewById( R.id.loginbtn );
        ForgetPassword = view.findViewById( R.id.forget );
        clickbutton();
        return view;
    }

    // Account created with firebase
    private void firebaseAuth() {
        auth.signInWithEmailAndPassword( email.getText().toString().trim(), password.getText().toString().trim() ).addOnCompleteListener( getActivity(), (OnCompleteListener<AuthResult>) new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    fragment();
                    Toast.makeText( getContext(), "Login successfully", Toast.LENGTH_SHORT ).show();
                    Log.d( TAG, "onComplete: " + "successful" );
                } else {
                    Toast.makeText( getContext(), "Credentials are wrong", Toast.LENGTH_SHORT ).show();
                    Log.d( TAG, "onComplete: " + "un-successful" );
                }
            }
        } ).addOnFailureListener( new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText( getContext(), e.getMessage().toString(), Toast.LENGTH_SHORT ).show();
            }
        } );

    }

    // Click on signup button

    private void clickbutton() {
        Login.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (email.getText().toString().isEmpty()) {
                    email.setError( "Email field is empty" );
                } else if (password.getText().toString().isEmpty()) {
                    password.setError( "Password field is empty" );

                } else {
                    firebaseAuth();
                }
            }
        } );

        next.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavController navController = Navigation.findNavController( requireView() );
                navController.navigate( R.id.registerFragment );
            }
        } );

        ForgetPassword.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder( getContext() );
                View dialogView = getLayoutInflater().inflate( R.layout.dialog_forget, null );
                EditText emailBox = dialogView.findViewById( R.id.forgetemail );
                builder.setView( dialogView );
                AlertDialog dialog = builder.create();

                dialogView.findViewById( R.id.resetBtn ).setOnClickListener( new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String userEmail = emailBox.getText().toString();
                        if (TextUtils.isEmpty( userEmail ) || !Patterns.EMAIL_ADDRESS.matcher( userEmail ).matches()) {
                            Toast.makeText( getContext(), "Enter a valid email address", Toast.LENGTH_SHORT ).show();
                            return;
                        }
                        auth.sendPasswordResetEmail( userEmail ).addOnCompleteListener( new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText( getContext(), "Check your email", Toast.LENGTH_SHORT ).show();
                                    dialog.dismiss();
                                } else {
                                    Toast.makeText( getContext(), "Unable to send, Failure", Toast.LENGTH_SHORT ).show();
                                }
                            }
                        } );
                    }
                } );

                dialogView.findViewById( R.id.btncancel ).setOnClickListener( new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                } );

                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawable( new ColorDrawable( 0 ) );
                }
                dialog.show();
            }
        } );
    }

    private void fragment() {
        NavController navController = Navigation.findNavController( requireView() );
        navController.navigate( R.id.homeFragment );
    }

}