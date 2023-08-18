package com.example.projet_mobile;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.renderscript.ScriptGroup;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
//UI Views
    private MaterialButton inputImageBtn;
    private MaterialButton recognizedTextBtn;
    private ShapeableImageView imageIv;
    private EditText recognizedTextEt;

    //TAG
    private static final String TAG = "MAIN_TAG";

    private Uri imageUri=null;
    // to handle the result of Camera/Gallery permissions
    private static final  int CAMERA_REQUEST_CODE=100;
    private static final  int STORAGE_REQUEST_CODE=101;

    //arrays of permission required to pick from camera ,gallery
    private String[] cameraPermissions;
    private String[] storagePermissions;
    //progress dialog
    private ProgressDialog progressDialog;


    //TextRecognizer
    private TextRecognizer textRecognizer;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //init UI Views
        inputImageBtn = findViewById(R.id.inputImageBtn);
        recognizedTextBtn = findViewById(R.id.recognizedTextBtn);
        imageIv = findViewById(R.id.imageIv);
        recognizedTextEt = findViewById(R.id.recognizedTextEt);

        //init arrays of permissions required for camera, gallery
        cameraPermissions=new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions=new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE};

        //init steup the progress dialog,show while text from image is being recognized
        progressDialog=new ProgressDialog(this);
        progressDialog.setTitle("please wait");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCanceledOnTouchOutside(false);


        //init TextRecognize
        textRecognizer  = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        //handle click,show input image dialog
        inputImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                showInputImageDialog();
            }
        });
        //handle click, start recognizing text from image we took from camera/gallery
        recognizedTextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //check if image is picked or not,picked if imageUri is not null

                if(imageUri== null){
                    //imageIri is null which means we haven't picked image yet , you can't recofnize text
                    Toast.makeText(MainActivity.this, "Pick image first...", Toast.LENGTH_SHORT).show();
                }
                else{
                    //imageUri is not null, which means we have picked image , we can recognize text
                    recognizeTextFromImage();
                }

            }
        }

        );


    }

    private void recognizeTextFromImage() {
        Log.d(TAG, "recognizeTextFromImage: ");
        //set Input from image uri
        progressDialog.setMessage("Preparing image ...");
        progressDialog.show();

        try {
            //prepare InputImage from image url
            InputImage inputImage = InputImage.fromFilePath(this,imageUri);
            //image prepared, we are about to start text recognition process , change progress message
            progressDialog.setMessage("Recognizing text ...");
            //start text recognition prepared , we are about to start text recognition process, change progress message
            Task<Text> textTaskResult = textRecognizer.process(inputImage)
            .addOnSuccessListener (new OnSuccessListener<Text>() {
                @Override
                public void onSuccess(Text text) {
                    //process completed , dismiss dialog
                    progressDialog.dismiss();
                    //get the recognized text
                    String recognizedText = text.getText();
                    Log.d(TAG, "onSuccess: recognizedText:"+recognizedText);
                    //set the recognized text to edit text
                    recognizedTextEt.setText(recognizedText);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    //failed recognizeing text from image, dismiss dialog, show reason in Toast
                    progressDialog.dismiss();
                    Log.e(TAG, "onFailure: ",e);
                    Toast.makeText(MainActivity.this, "Failed recognizing text due to "+e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            //Exception occured while preparing Input Image, dismiss dialog , show some reason Toast
            progressDialog.dismiss();
            Log.e(TAG, "recognizeTextFromImage: ",e);
            Toast.makeText(this, "Failed preparing image due to"+e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

    }

    private void showInputImageDialog() {
        PopupMenu popupMenu = new PopupMenu(this,inputImageBtn);


        popupMenu.getMenu().add(Menu.NONE, 1, 1 ,"CAMERA");
        popupMenu.getMenu().add(Menu.NONE, 2, 2 ,"GALLERY");

        popupMenu.show();

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                int id = menuItem.getItemId();
                if(id==1){
                    //camera is clicked, check if camera permissions are granted or not
                    Log.d(TAG, "onMenuItemClick: Camera clicked...");
                   if(checkCameraPermission()) {
                       //camera permissions granted,we can launch camera intent
                       pickImageCamera();
                   }
                   else{
                       //camera permissions not granted,request the camera permissions
                       requestCameraPermission();
                   }
                }
                else if(id==2){
                    //Gallery is clicked , check if storage permission is granted or not
                    Log.d(TAG, "onMenuItemClick: Gallery Clicked");
                    if(checkStoragePermission()){
                        //storage permission granted, we can launch the gallery intent
                      pickImageGallery();
                    }
                    else{
                        //storage permission not granted, request the storage permission
                        requestStoragePermission();
                    }
                }
                return true;
            }
        });
    }

    private void pickImageGallery(){
        Log.d(TAG, "pickImageGallery: ");
        //intent to pick image from gallery will show all resources from where we can pick the image

        Intent intent = new Intent(Intent.ACTION_PICK);
//set type of file we want to pick i.e .image
        intent.setType("image/*");
        galleryActivityResultLauncher.launch(intent);

    }
    private ActivityResultLauncher<Intent> galleryActivityResultLauncher =registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    //here will receive the image, if picked
                    if(result.getResultCode()== Activity.RESULT_OK){
                        //image picked
                        Intent data =result.getData();
                        imageUri =data.getData();
                        Log.d(TAG, "onActivityResult: imageUri "+imageUri);
                        //set to imageview
                        imageIv.setImageURI(imageUri);
                    }
                    else{
                        Log.d(TAG, "onActivityResult: cancelled");
                        //cancelled
                        Toast.makeText(MainActivity.this,"Cancelled...",Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );



    private void pickImageCamera(){
        Log.d(TAG, "pickImageCamera: ");
        //get ready the image data to store in MediaStore
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Sample Title");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Sample Description");
        //imageUri
        imageUri= getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
        //intent to launch camera
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraActivityResultLauncher.launch(intent);
        }
        private ActivityResultLauncher<Intent>cameraActivityResultLauncher=registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        //here we will receive th image, if token from camera
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            //image is taken from camera
                            //we are already have the image in imageUri using function pickImageCamera
                            Log.d(TAG, "onActivityResult: imageUri"+imageUri);
                            imageIv.setImageURI(imageUri);
                        }
                        else{
                            //cancelled
                            Log.d(TAG, "onActivityResult: cancelled");
                            Toast.makeText( MainActivity.this, "Cancelled", Toast.LENGTH_SHORT).show();

                        }
                    }
                }
        );
    private  boolean checkStoragePermission(){
        boolean result = ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)==(PackageManager.PERMISSION_GRANTED);

        return result;
    }

    private void requestStoragePermission(){
        ActivityCompat.requestPermissions(this, storagePermissions,STORAGE_REQUEST_CODE);
    }
    private boolean checkCameraPermission(){
        boolean cameraResult = ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==(PackageManager.PERMISSION_GRANTED);
        boolean storageResult = ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)==(PackageManager.PERMISSION_GRANTED);

        return cameraResult && storageResult;
    }
    private void requestCameraPermission(){
        //request camera permissions(for camera intent)
        ActivityCompat.requestPermissions(this,cameraPermissions,CAMERA_REQUEST_CODE);


    }
    //handle permission results

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);


        switch (requestCode)  {
            case CAMERA_REQUEST_CODE:{
                //check if some action from permissions granted, we can launch camera intent
                if(grantResults.length>0){
                    //check if camera storage permissions granted contains boolean results either true or false
                    boolean cameraAccepted = grantResults[0]== PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1]== PackageManager.PERMISSION_GRANTED;
//check if both permissions are denied can't launch camera intent
                    if(cameraAccepted && storageAccepted){
                        //both permissions are denied can't launch camera intent
                        pickImageCamera();
                    }
                    else{
//one or both permissions are denied can't launch camera intent
                        Toast.makeText(this, "Camera & Storage permissions are required", Toast.LENGTH_SHORT).show();
                    }
                }
                else{
                    //Neither allowed not denied rather cancelled
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
                }
            }
            break;
            case STORAGE_REQUEST_CODE:{
                //CHECK if some action from permission dialog performed or not allow/deny

                if (grantResults.length>0){
                    //check if storage permissions granted, contains boolean or not allow/deny
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    //check if storage permission is granted or not
                    if(storageAccepted){
                        //storage permission granted, we can launch gallery intent
                        pickImageGallery();

                    }
                    else{
                        //storage permission denied, can't launch gallery intent
                        Toast.makeText(this, "Storage permission is required", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
        }
    }
}



