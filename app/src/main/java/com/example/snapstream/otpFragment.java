package com.example.snapstream;

import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class otpFragment extends Fragment {
    EditText Otp;
    Button VerifyOtp;
    String verificationCode;
    PhoneAuthProvider.ForceResendingToken resendingToken;
    TextView Resend;
    String text;
    ProgressBar progressBar;
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    Long timeoutSeconds = 60L;
    Handler mainHandler = new Handler( Looper.getMainLooper());


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        // Retrieve phone number argument from the bundle
        if (getArguments() != null) {
            text = getArguments().getString( "number" );
            Toast.makeText( getContext(), "Otp send to"+ text, Toast.LENGTH_SHORT ).show();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_otp, container, false);
        Otp = view.findViewById(R.id.otp);
        VerifyOtp = view.findViewById(R.id.verify);
        Resend = view.findViewById(R.id.resendOtp);
        progressBar = view.findViewById(R.id.progress1);
        progressBar.setVisibility( View.GONE );

        sendOtp(text, false);
        VerifyOtp.setOnClickListener(v -> {
            // Perform verification
            progressBar.setVisibility( View.VISIBLE );
            String enteredOtp = Otp.getText().toString();
           PhoneAuthCredential credential=  PhoneAuthProvider.getCredential( verificationCode, enteredOtp );
           signIn( credential );
           setInProgress( true );
        });

        Resend.setOnClickListener(v -> {
            // Resend OTP functionality
            sendOtp(text, true);
        });

        return view;
    }

    void sendOtp(String text, boolean isResend) {
        StartResendTimer();
        setInProgress( true );
        progressBar.setVisibility(View.VISIBLE);
        PhoneAuthOptions.Builder builder = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(text)
                .setTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .setActivity(requireActivity())
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                        signIn(phoneAuthCredential);
                        setInProgress( false );
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Toast.makeText(getContext(), "OTP verification Failed", Toast.LENGTH_SHORT).show();
                        setInProgress( false );
                    }

                    @Override
                    public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        super.onCodeSent(s, forceResendingToken);
                        verificationCode = s;
                        resendingToken = forceResendingToken;
                        setInProgress( false );
                        Toast.makeText(getContext(), "OTP sent successfully", Toast.LENGTH_SHORT).show();
                    }
                });

        if (isResend) {
            PhoneAuthProvider.verifyPhoneNumber(builder.setForceResendingToken(resendingToken).build());
        } else {
            PhoneAuthProvider.verifyPhoneNumber(builder.build());
        }
    }

    void setInProgress(boolean inProgress) {
        if(inProgress) {
            progressBar.setVisibility( View.VISIBLE );
            VerifyOtp.setVisibility( View.GONE );
        } else {
            progressBar.setVisibility( View.GONE );
            VerifyOtp.setVisibility( View.VISIBLE );
        }
    }

    void signIn(PhoneAuthCredential phoneAuthCredential) {
        //login and go to next activity
        setInProgress( true );
        mAuth.signInWithCredential( phoneAuthCredential ).addOnCompleteListener( new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
               if(task.isSuccessful() && text != null){
                   setInProgress( false );
                   Toast.makeText( getContext(), "Otp verification Successfull", Toast.LENGTH_SHORT ).show();
                       final Bundle bundle = new Bundle();
                       String Number = text;
                       bundle.putString("number",  Number );
                   NavController navController = Navigation.findNavController(requireView());
                   navController.navigate(R.id.action_otpFragment_to_homeFragment, bundle);
               }else {
                   Toast.makeText( getContext(), "OTP verification failed", Toast.LENGTH_SHORT ).show();
               }
            }
        } );

    }
    void StartResendTimer(){
        Resend.setEnabled( false );
        Timer timer = new Timer();

        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                timeoutSeconds--;
                // Post a Runnable to update the UI component on the main thread
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Resend.setText("Resend OTP in " + timeoutSeconds + " seconds");
                    }
                });
                if (timeoutSeconds <= 0) {
                    timeoutSeconds = 60L;
                    timer.cancel();
                    // Post a Runnable to update the UI component on the main thread
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Resend.setEnabled(true);
                        }
                    });
                }
            }
        }, 0, 1000);

    }
}
