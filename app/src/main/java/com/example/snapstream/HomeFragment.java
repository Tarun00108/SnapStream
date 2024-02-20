package com.example.snapstream;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
public class HomeFragment extends Fragment {
    private ProgressBar progressBar;
    private TextView progressText;
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "download_channel";
    private FirebaseFirestore firestore;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        firestore = FirebaseFirestore.getInstance();
        ((AppCompatActivity)getActivity()).getSupportActionBar().show();
        EditText editTextLink = view.findViewById(R.id.check);
        progressBar = view.findViewById(R.id.progressBar);
        progressText = view.findViewById(R.id.progressText);
        Button buttonDownload = view.findViewById(R.id.get);
        buttonDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String downloadLink = editTextLink.getText().toString();
                // Create a new instance of DownloadTask and execute it
                new DownloadTask(getContext()).execute(downloadLink);
            }
        });


        return view;
    }

    private class DownloadTask extends AsyncTask<String, Integer, Void> {
        private Context context;
        File outputFile;
        String contentType;

        public DownloadTask(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            createNotificationChannel();
            showNotification();
        }

        @Override
        protected Void doInBackground(String... params) {
            if (params.length > 0) {
                String downloadLink = params[0];
                try {
                    // Open a connection to the URL
                    URL url = new URL(downloadLink);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.connect();
                    contentType = connection.getContentType();
                    String fileExtension = getFileExtension(contentType);
                    File directory;
                    if (contentType.startsWith("image")) {
                        directory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                    } else if (contentType.startsWith("audio")) {
                        directory = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
                    } else if (contentType.startsWith("video")) {
                        directory = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
                    } else {
                        directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                    }
                    String filename = getFileNameFromUrl(downloadLink);
                    outputFile = new File(directory, filename);
                    // Download and save the file
                    InputStream input = connection.getInputStream();
                    FileOutputStream output = new FileOutputStream(outputFile);
                    byte[] buffer = new byte[1024];
                    int length;
                    int fileSize = connection.getContentLength();
                    int downloadedSize = 0;
                    while ((length = input.read(buffer)) != -1) {
                        output.write(buffer, 0, length);
                        downloadedSize += length;
                        publishProgress(downloadedSize, fileSize);
                    }
                    output.close();
                    input.close();
                    connection.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if (values.length >= 2) {
                int downloadedSize = values[0];
                int fileSize = values[1];
                int progress = (int) (((float) downloadedSize / fileSize) * 100);
                progressBar.setProgress(progress);
                progressText.setText(progress + "%");
                updateNotificationProgress(progress);
            } else {
            }
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            Toast.makeText(context, "Downloading Complete", Toast.LENGTH_SHORT).show();
            saveFileInfoToFirestore();
            cancelNotification();
        }

        private String getFileNameFromUrl(String url) {
            try {
                URL parsedUrl = new URL(url);
                String path = parsedUrl.getPath();
                // Decode the URL-encoded string
                String decodedFileName = URLDecoder.decode(path.substring(path.lastIndexOf('/') + 1), StandardCharsets.UTF_8.name());
                return decodedFileName;
            } catch (MalformedURLException | UnsupportedEncodingException e) {
                e.printStackTrace();
                return "default_filename";
            }
        }

        private String getFileExtension(String contentType) {
            switch (contentType) {
                case "image/jpeg":
                    return "jpg";
                case "image/png":
                    return "png";
                case "audio/mpeg":
                    return "mp3";
                case "video/mp4":
                    return "mp4";
                default:
                    return "unknown";
            }
        }

        private void createNotificationChannel() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                CharSequence name = getString(R.string.channel_name);
                String description = getString(R.string.channel_description);
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
                channel.setDescription(description);

                NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }
        }

        private void showNotification() {
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.baseline_download_24)
                    .setContentTitle("Downloading...")
                    .setContentText("Download in progress")
                    .setProgress(100, 0, true)
                    .setContentIntent(pendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }


        private void updateNotificationProgress(int progress) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon( R.drawable.baseline_download_24)
                    .setContentTitle("Downloading...")
                    .setContentText("Download in progress")
                    .setProgress(100, progress, false)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }

        private void cancelNotification() {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(NOTIFICATION_ID);
        }

        private void saveFileInfoToFirestore() {
            if (outputFile != null && contentType != null) {
                // Create a map to store file information
                Map<String, Object> fileInfo = new HashMap<>();
                // Add file information to the map
                fileInfo.put("fileName", outputFile.getName());
                fileInfo.put("fileType", contentType);
                // Access the authenticated user's email
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    Log.d("HomeFragment", "User email: " + user.getEmail());
                } else {
                    Log.e("HomeFragment", "User is not authenticated");
                }

                // Add the file information to Firestore under a collection named "files"
                firestore.collection("files").document( String.valueOf( user ) ).set(fileInfo)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d("HomeFragment", "File information saved to Firestore successfully");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e("HomeFragment", "Error saving file information to Firestore"+ e.getMessage());
                            }
                        });
            } else {
                Log.e("HomeFragment", "outputFile or contentType is null");
                Log.e("HomeFragment", "outputFile: " + outputFile);
                Log.e("HomeFragment", "contentType: " + contentType);
            }
        }

    }
}
