package com.example.snapstream;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hbb20.CountryCodePicker;

import java.util.HashMap;
import java.util.Map;

public class RegisterFragment extends Fragment {
    private EditText email, password, number;
    private Button SendOtp;
    private TextView aleradyLogin;
    private ImageView googleBtn;
    private ProgressBar progressBar;
    private CountryCodePicker countryCodePicker;
    FirebaseAuth auth;
    GoogleSignInOptions gso;
    private FirebaseFirestore firestore;
    GoogleSignInClient gsc;
    FirebaseDatabase database;
    DatabaseReference reference;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate( R.layout.fragment_register, container, false );
        email = view.findViewById( R.id.email );
        password = view.findViewById( R.id.password );
        number = view.findViewById( R.id.number );
        aleradyLogin = view.findViewById( R.id.alreadyLogin );
        SendOtp = view.findViewById( R.id.registerbtn );
        progressBar = view.findViewById( R.id.progress );
        progressBar.setVisibility( View.GONE );
        countryCodePicker = view.findViewById( R.id.countrycode );
        countryCodePicker.registerCarrierNumberEditText( number );
        googleBtn = view.findViewById( R.id.Gpic );
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        database = FirebaseDatabase.getInstance().getReference().getDatabase();
        gso = new GoogleSignInOptions.Builder( GoogleSignInOptions.DEFAULT_SIGN_IN ).requestIdToken( getString( R.string.default_web_client_id ) ).requestEmail().build();
        gsc = GoogleSignIn.getClient( getContext(), gso );
        clickButton();
        return view;
    }

    private void firebaseAuthforEmail() {
        auth.createUserWithEmailAndPassword( email.getText().toString().trim(), password.getText().toString().trim() )
                .addOnCompleteListener( getActivity(), task -> {
                    if (task.isSuccessful()) {
                        String phoneNumber = countryCodePicker.getFullNumberWithPlus();
                        fragment( phoneNumber );
                        FirebaseUser user = auth.getCurrentUser();
                        Users users = new Users( email, password, number );
                        users.setEmail( user.getEmail() );
                        // users.setPassword( String.valueOf( user. );
                        users.setPhoneNumber( user.getPhoneNumber() );
                        database.getReference().child( "Users" ).setValue( users );
                    } else {
                        Toast.makeText( getContext(), "Check your Email & Password", Toast.LENGTH_SHORT ).show();
                        Log.d( "RegisterFragment", "onComplete: " + "un-successful" );
                    }
                } )
                .addOnFailureListener( e -> Log.d( "RegisterFragment", "onFailure: " + "un-successful" + e.getMessage() ) );
    }

    private void firebaseAuthForGoogle(String idToken, String userEmail) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        auth.signInWithCredential(credential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                            // Create a map to store user information
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("email", userEmail);

                            // Add the user information to Firestore under a collection named "users"
                            firestore.collection("users").document(uid).set(userData)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Log.d("HomeFragment", "User information saved to Firestore successfully");
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.e("HomeFragment", "Error saving user information to Firestore", e);
                                        }
                                    });

                            NavController navController = Navigation.findNavController(requireView());
                            navController.navigate(R.id.action_registerFragment_to_homeFragment);
                        } else {
                            Toast.makeText(getContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void SignIn() {
        Intent signInIntent = gsc.getSignInIntent();
        startActivityForResult(signInIntent, 1000);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1000) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthForGoogle(account.getIdToken(), account.getEmail());

            } catch (ApiException e) {
                Toast.makeText(getContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void fragment(String phoneNumber) {
        if (phoneNumber != null) {
            final Bundle bundle = new Bundle();
            String Number = phoneNumber;
            bundle.putString( "number", Number );
            progressBar.setVisibility( View.VISIBLE );
            NavController navController = Navigation.findNavController( requireView() );
            navController.navigate( R.id.action_registerFragment_to_otpFragment, bundle );
        }
    }

    private void clickButton() {
        SendOtp.setOnClickListener( view -> {
            if (email.getText().toString().isEmpty()) {
                email.setError( "Email field is empty" );
            } else if (password.getText().toString().isEmpty()) {
                password.setError( "Password field is empty" );
            } else if (!countryCodePicker.isValidFullNumber()) {
                number.setError( "Phone number not valid" );
            } else {
                firebaseAuthforEmail();
            }
        } );
        aleradyLogin.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavController navController = Navigation.findNavController( requireView() );
                navController.navigate( R.id.loginFragment );
            }
        } );

        googleBtn.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Login Using Google
                SignIn();
            }
        } );

    }
}
