package com.example.moviestreamingapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileUtils;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.moviestreamingapp.activities.UploadMoviesThumbNailActivity;
import com.example.moviestreamingapp.model.VideoUploadDetails;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import org.apache.commons.io.FilenameUtils;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    Uri videoUri;
    TextView tvVideoSelected;
    String videoCategory;
    String videoTitle;
    String currentUid;
    StorageReference mStorageRef;
    StorageTask mUploadTask;
    DatabaseReference videoReference;
    EditText videoDescription;
    Spinner spinner;
    Button chooseVideo,uploadVideo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvVideoSelected = findViewById(R.id.text_view_video_selected);
        videoDescription = findViewById(R.id.movie_description_editText);
        spinner = findViewById(R.id.spinner);
        chooseVideo = findViewById(R.id.button_choose_video);
        uploadVideo = findViewById(R.id.upload_movie_btn);
        mStorageRef = FirebaseStorage.getInstance().getReference().child("Videos");
        videoReference = FirebaseDatabase.getInstance().getReference().child("Videos");

        List<String> categories = new ArrayList<>();
        categories.add("Action");
        categories.add("Adventures");
        categories.add("Song");
        categories.add("Sport");
        categories.add("Comedy");
        categories.add("Romantic");
        categories.add("Keyboard Training");

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,categories);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);

        chooseVideo.setOnClickListener(view -> {
            openVideo();
        });

        uploadVideo.setOnClickListener(view -> {
            uploadFileToFirebase();
        });
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        videoCategory = adapterView.getItemAtPosition(i).toString();
        Toast.makeText(this, videoCategory, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
    private void openVideo(){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        startActivityForResult(intent,101);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ((requestCode==101) && (resultCode == RESULT_OK) && (data.getData() != null)){
            videoUri = data.getData();

            String path = null;
            Cursor cursor;
            int column_index_data;
            String [] projection = {MediaStore.MediaColumns.DATA,MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media._ID,MediaStore.Video.Thumbnails.DATA};
            final String orderBy = MediaStore.Video.Media.DEFAULT_SORT_ORDER;
            cursor = MainActivity.this.getContentResolver().query(videoUri,projection,null,null,orderBy);
            column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            while (cursor.moveToNext()){
                path = cursor.getString(column_index_data);
                videoTitle = FilenameUtils.getBaseName(path);
            }
            tvVideoSelected.setText(videoTitle);
        }
    }

    private void uploadFileToFirebase(){
        if (tvVideoSelected.equals("No Video Selected")){
            Toast.makeText(this, "Please Select a Video to be Uploaded", Toast.LENGTH_SHORT).show();
        }else {
            if (mUploadTask !=null && mUploadTask.isInProgress()){
                Toast.makeText(this, "Video Upload Already in Progress", Toast.LENGTH_SHORT).show();
            }else {
                uploadVideo();
            }
        }
    }
    private void uploadVideo(){
        if (videoUri !=null){
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Video Uploading...");
            progressDialog.show();
            final StorageReference storageReference = mStorageRef.child(videoTitle);
            mUploadTask = storageReference.putFile(videoUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String video_uri = uri.toString();
                            VideoUploadDetails videoUploadDetails = new VideoUploadDetails(
                                    "", "", "", video_uri, videoTitle, videoDescription.getText().toString(),
                                    videoCategory
                            );
                            String uploadSid = videoReference.push().getKey();
                            videoReference.child(uploadSid).setValue(videoUploadDetails);
                            currentUid = uploadSid;
                            progressDialog.dismiss();
                            if (currentUid.equals(uploadSid)){
                                moveToThumbNailActivity();
                            }
                        }
                    });
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                    double progress = (100 * taskSnapshot.getBytesTransferred()/taskSnapshot.getTotalByteCount());
                    progressDialog.setMessage((int)progress+"% Uploaded");
                }
            });
        }else {
            Toast.makeText(this, "No Video Was Selected For the Upload", Toast.LENGTH_SHORT).show();
        }
    }

    private void moveToThumbNailActivity(){
        Intent intent = new Intent(MainActivity.this, UploadMoviesThumbNailActivity.class);
        intent.putExtra("currentUid",currentUid);
        intent.putExtra("thumbNailName",videoTitle);
        startActivity(intent);
    }
}