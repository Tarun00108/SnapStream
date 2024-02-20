package com.example.snapstream;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );
        if (isLoggedIn()) {
            // User is logged in, navigate to home fragment
            navigateToHomeFragment();
        } else {
            // User is not logged in, navigate to login fragment
            navigateToRegisterFragment();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate( R.menu.home_menu, menu );
        getSupportActionBar().hide();
        return super.onCreateOptionsMenu( menu );
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.home:
                Toast.makeText( this, "Home", Toast.LENGTH_SHORT ).show();
                break;

            case R.id.logout:
                Toast.makeText( this, "logout", Toast.LENGTH_SHORT ).show();
                logout();
                break;
            default:
                return super.onOptionsItemSelected( item );

        }

        return super.onOptionsItemSelected( item );
    }

    public void logout() {
        LoginFragment loginFragment = new LoginFragment();
        getSupportActionBar().show();
        // Add the fragment to the activity
        getSupportFragmentManager()
                .beginTransaction()
                .add( R.id.fragment_container, loginFragment )
                .commit();

    }

    private boolean isLoggedIn() {
        // Retrieve login status from SharedPreferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences( this );
        return sharedPreferences.getBoolean( "isLoggedIn", false );
    }

    private void navigateToHomeFragment() {
        // Navigate to home fragment
        getSupportFragmentManager().beginTransaction()
                .replace( R.id.fragment_container, new HomeFragment() )
                .commit();
    }

    private void navigateToRegisterFragment() {
        // Navigate to login fragment
        getSupportFragmentManager().beginTransaction()
                .replace( R.id.fragment_container, new RegisterFragment() );


    }
}