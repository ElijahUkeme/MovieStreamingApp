package com.example.moviestreamingapp.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.moviestreamingapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

public class UploadMoviesThumbNailActivity extends AppCompatActivity {

    Uri videoThumbUri;
    String thumbnail_uri;
    ImageView thumbnail_image;
    StorageReference mStorageRefThumbNail;
    DatabaseReference videoRef;
    TextView selectedText;
    RadioButton radioButtonLatest, radioButtonPopular,radioButtonNoType,radioButtonSlide;
    StorageTask mStorageTask;
    DatabaseReference updatedDatabaseRef;
    Button pickImage,uploadImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_movies_thumb_nail);
        selectedText = findViewById(R.id.no_thumbnail_selected);
        thumbnail_image = findViewById(R.id.imageView);
        mStorageRefThumbNail = FirebaseStorage.getInstance().getReference().child("Video ThumbNail");
        radioButtonLatest = findViewById(R.id.latestMoviesRadio);
        radioButtonPopular =  findViewById(R.id.popularMoviesRadio);
        radioButtonNoType = findViewById(R.id.noTypeMoviesRadio);
        radioButtonSlide = findViewById(R.id.slideMoviesRadio);
        pickImage = findViewById(R.id.btn_upload_thumbnail);
        uploadImage = findViewById(R.id.buttonUpload);
        videoRef = FirebaseDatabase.getInstance().getReference().child("Videos");
        String currentUid = getIntent().getStringExtra("currentUid");
         updatedDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Videos").child(currentUid);

         pickImage.setOnClickListener(view -> {
             openImageGallery();
         });
         uploadImage.setOnClickListener(view -> {
             uploadFileToFirebase();
         });

        radioButtonPopular.setOnClickListener(view -> {
            String popular = radioButtonPopular.getText().toString();
            updatedDatabaseRef.child("video_slide").setValue("");
            updatedDatabaseRef.child("video_type").setValue(popular);
        });

        radioButtonLatest.setOnClickListener(view -> {
            String latest = radioButtonPopular.getText().toString();
            updatedDatabaseRef.child("video_slide").setValue("");
            updatedDatabaseRef.child("video_type").setValue(latest);
        });

        radioButtonNoType.setOnClickListener(view -> {
            String noType = radioButtonPopular.getText().toString();
            updatedDatabaseRef.child("video_slide").setValue("");
            updatedDatabaseRef.child("video_type").setValue(noType);
        });

        radioButtonSlide.setOnClickListener(view -> {
            String slide = radioButtonPopular.getText().toString();
            updatedDatabaseRef.child("video_slide").setValue(slide);
        });
    }

    private void openImageGallery(){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent,100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK && data.getData() != null) {
            videoThumbUri = data.getData();

            try {

                String thumbNailName = getFileName(videoThumbUri);
                selectedText.setText(thumbNailName);
                thumbnail_image.setImageURI(videoThumbUri);

            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }
    @SuppressLint("Range")
    private String getFileName(Uri uri){
        String result = null;
        if (uri.getScheme().equals("content")){
            Cursor cursor = getContentResolver().query(uri,null,null,null,null);
            try {
                if (cursor !=null && cursor.moveToFirst()){
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }finally {
                cursor.close();
            }
            if (result == null){
                result = uri.getPath();
                int cut = result.lastIndexOf("/");
                if (cut != -1){
                    result = result.substring(cut + 1);
                }
            }
        }
        return result;
    }

    private void uploadFiles(){
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading Thumbnail...");
        progressDialog.show();
        String video_name = getIntent().getStringExtra("thumbNailName");
        StorageReference storageReference = mStorageRefThumbNail.child(video_name+"."+getFileExtension(videoThumbUri));

        storageReference.putFile(videoThumbUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        thumbnail_uri = uri.toString();
                        updatedDatabaseRef.child("video_thumb").setValue(thumbnail_uri);
                        progressDialog.dismiss();
                        Toast.makeText(UploadMoviesThumbNailActivity.this, "File Uploaded Successfully", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(UploadMoviesThumbNailActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {

                double progress = (100.0 * taskSnapshot.getBytesTransferred())/taskSnapshot.getTotalByteCount();
                progressDialog.setMessage((int)progress +"% uploaded...");
            }
        });
    }

    private void uploadFileToFirebase(){
        if (selectedText.equals("No Thumbnail Selected")){
            Toast.makeText(this, "Please Select An Image First", Toast.LENGTH_SHORT).show();
        }else if (mStorageTask != null && mStorageTask.isInProgress()){
            Toast.makeText(this, "Files Upload Already In Progress", Toast.LENGTH_SHORT).show();
        }else {
            uploadFiles();
        }
    }

    public String getFileExtension(Uri uri){
        ContentResolver cr = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(cr.getType(uri));
    }
}