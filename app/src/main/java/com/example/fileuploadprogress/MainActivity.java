package com.example.fileuploadprogress;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.net.InetAddress;

public class MainActivity extends AppCompatActivity {

    private int STORAGE_PERMISSION = 1;

    private static final int FILE_SELECT_REQUEST_CODE = 1;
    Button selectFile, pauseBtn, cancelBtn;
    TextView fileName, fileSize, progressPercent;
    ProgressBar mProgressBar;

    StorageTask mStorageTask;
    FirebaseAuth mAuth;
    StorageReference mStorageRef;

    Uri fileUri;

    String displayName = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        selectFile = findViewById(R.id.selectFile);
        pauseBtn = findViewById(R.id.pauseUpload);
        cancelBtn = findViewById(R.id.cancelUpload);

        fileName = findViewById(R.id.fileName);
        fileSize = findViewById(R.id.fileSize);
        progressPercent = findViewById(R.id.progressPercent);
        mProgressBar = findViewById(R.id.fileProgress);

        pauseBtn.setEnabled(false);
        cancelBtn.setEnabled(false);
        selectFile.setEnabled(true);

        mAuth = FirebaseAuth.getInstance();


        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
        } else {

            requestStoragePermission();
        }

        mStorageRef = FirebaseStorage.getInstance().getReference();

        mAuth.signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("Success", "signInAnonymously:success");
                            FirebaseUser user = mAuth.getCurrentUser();


                            selectFile.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {


                                    if (ContextCompat.checkSelfPermission(MainActivity.this,
                                            Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

                                        Toast.makeText(MainActivity.this, "Permission Granted", Toast.LENGTH_SHORT).show();
                                        openFileSelector();

                                    } else {

                                        requestStoragePermission();
                                    }


                                }
                            });

                            pauseBtn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {

                                    String btnText = pauseBtn.getText().toString();

                                    if (btnText.equals("Pause Upload")) {

                                        mStorageTask.pause();
                                        pauseBtn.setText("Resume Upload");

                                    } else {

                                        mStorageTask.resume();
                                        pauseBtn.setText("Pause Upload");
                                    }


                                }
                            });

                            cancelBtn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {

                                    mStorageTask.cancel();
                                    Toast.makeText(MainActivity.this, "Cancelling. Please Wait..", Toast.LENGTH_SHORT).show();
                                    mStorageTask.addOnCanceledListener(new OnCanceledListener() {
                                        @Override
                                        public void onCanceled() {


                                            if(mStorageTask.isCanceled()){

                                                Toast.makeText(MainActivity.this, "Upload Cancelled", Toast.LENGTH_SHORT).show();

                                                fileSize.setText("0 MB / 0MB");
                                                Log.i("fileSize", "fileSize Passed");
                                                progressPercent.setText("0%");
                                                Log.i("progressPercent", "progressPercent Passed");
                                                mProgressBar.setProgress(0);
                                                Log.i("mProgressBar", "mProgressBar Passed");
                                                fileName.setText("filename.type");
                                                Log.i("fileName", "fileName Passed");
                                                pauseBtn.setText("Pause Upload");

                                                pauseBtn.setEnabled(false);
                                                cancelBtn.setEnabled(false);
                                                selectFile.setEnabled(true);


                                            }else{

                                                mStorageTask.pause();
                                                mStorageTask.cancel();
                                                Toast.makeText(MainActivity.this, "Cancelling", Toast.LENGTH_SHORT).show();
                                            }

                                        }
                                    });


                                }
                            });
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("FAILURE", "signInAnonymously:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
//                            updateUI(null);
                        }

                        // ...
                    }
                });

    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
//        updateUI(currentUser);
        Toast.makeText(this, "User: "+currentUser, Toast.LENGTH_SHORT).show();
    }


    private void requestStoragePermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Permission Needed")
                    .setMessage("This permission is needed to select the file to be uploaded.")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    STORAGE_PERMISSION);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            dialog.dismiss();

                        }
                    }).create().show();

        } else {

            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == STORAGE_PERMISSION) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
            } else {

                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }

    }

    private void openFileSelector() {

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {

            startActivityForResult(Intent.createChooser(intent, "Select a file.."),
                    FILE_SELECT_REQUEST_CODE);

        } catch (android.content.ActivityNotFoundException ex) {

            Toast.makeText(this, "Please install a file manager", Toast.LENGTH_SHORT).show();

        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable final Intent data) {

        if (requestCode == FILE_SELECT_REQUEST_CODE && resultCode == RESULT_OK) {

            fileUri = data.getData();

            Log.v("FILE_URI", "FILE_URI:   " + fileUri);

            String uriString = fileUri.toString();

            File myFile = new File(uriString);
//            String path = myFile.getAbsolutePath();


            if (uriString.startsWith("content://")) {

                Cursor cursor = null;

                try {

                    cursor = MainActivity.this.getContentResolver().query(fileUri, null, null, null, null);
                    if (cursor != null && cursor.moveToNext()) {

                        displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    }

                } finally {

                    cursor.close();
                }
            } else if (uriString.startsWith("file://")) {

                displayName = myFile.getName();
            }

            fileName.setText(displayName);

            Toast.makeText(this, "You've selected a file", Toast.LENGTH_SHORT).show();

            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Warning!")
                    .setMessage("Smaller files cannot be cancelled directly")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            Log.v("File", "FileName:  " + displayName);

                            pauseBtn.setEnabled(true);
                            cancelBtn.setEnabled(true);
                            selectFile.setEnabled(false);

                            //            Uri file = Uri.fromFile(new File("path/to/images/rivers.jpg"));
                            StorageReference riversRef = mStorageRef.child(displayName);

                            fileUri = data.getData();

                            mStorageTask = riversRef.putFile(fileUri)
                                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                        @Override
                                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                            // Get a URL to the uploaded content
//                            Uri downloadUrl = taskSnapshot.getDownloadUrl();

                                            AlertDialog.Builder alertSuccess = new AlertDialog.Builder(MainActivity.this);
                                            alertSuccess.setMessage("File uploaded successfully.");
                                            alertSuccess.setCancelable(false);
                                            alertSuccess.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {

                                                    dialog.dismiss();

                                                }
                                            });
                                            AlertDialog alert = alertSuccess.create();
                                            alert.show();

                                            fileSize.setText("0 MB / 0MB");
                                            Log.i("fileSize", "fileSize Passed");
                                            progressPercent.setText("0%");
                                            Log.i("progressPercent", "progressPercent Passed");
                                            mProgressBar.setProgress(0);
                                            Log.i("mProgressBar", "mProgressBar Passed");
                                            fileName.setText("filename.type");
                                            Log.i("fileName", "fileName Passed");
                                            pauseBtn.setText("Pause Upload");

                                            pauseBtn.setEnabled(false);
                                            cancelBtn.setEnabled(false);
                                            selectFile.setEnabled(true);

                                            Log.v("Done", "Successful");
                                            Toast.makeText(MainActivity.this, "Successful", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception exception) {
                                            // Handle unsuccessful uploads
                                            // ...

                                            Log.v("Error Occurred", "Error: " + exception);
                                            Toast.makeText(MainActivity.this, "Error: " + exception, Toast.LENGTH_LONG).show();
                                            pauseBtn.setEnabled(false);
                                            cancelBtn.setEnabled(false);
                                            selectFile.setEnabled(true);

                                            fileSize.setText("0 MB / 0MB");
                                            progressPercent.setText("0%");
                                            mProgressBar.setProgress(0);
                                            fileName.setText("filename.type");
                                            pauseBtn.setText("Pause Upload");

                                        }
                                    }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                                        @Override
                                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {

                                            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                                            mProgressBar.setProgress((int) progress);

                                            String progressText = taskSnapshot.getBytesTransferred() / (1024) + " KB " + " / " + taskSnapshot.getTotalByteCount() / (1024) + " KB ";
                                            fileSize.setText(progressText);
                                            progressPercent.setText((int) progress + "%");

                                        }
                                    });

                        }
                    }).create().show();

        }

        super.onActivityResult(requestCode, resultCode, data);


    }


}
