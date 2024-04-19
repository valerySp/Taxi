package com.example.taxi;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    CircleImageView circleImageView;
    private EditText nameET,phoneET,carET;
    private ImageView closeBtn,saveBtn;
    private TextView imageChangeBtn;
    private String getType;
    private String checker = "", myUrl;
    private static final int IMAGE_PICK_GALLERY_CODE=103;
    private StorageTask uploadTask;
    private StorageReference storageProfileImageRef;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        circleImageView = (CircleImageView)findViewById(R.id.profile_image);
        nameET = (EditText) findViewById(R.id.name);
        phoneET = (EditText) findViewById(R.id.phone);
        carET = (EditText) findViewById(R.id.car);
        closeBtn = (ImageView) findViewById(R.id.close_btn);
        saveBtn = (ImageView)findViewById(R.id.save_btn);
        imageChangeBtn = (TextView) findViewById(R.id.change_photo);

        storageProfileImageRef = FirebaseStorage.getInstance().getReference().child("Profile Pictures");
        getType=getIntent().getStringExtra("type");

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(getType);

        if(getType.equals("Drivers")){
            carET.setVisibility(View.VISIBLE);
        }

        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(getType.equals("Drivers")){
                    startActivity(new Intent(SettingsActivity.this, DriverMapsActivity.class));
                }
                else {
                    startActivity(new Intent(SettingsActivity.this, CustomersMapActivity.class));
                }
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checker.equals("clicked")){
                    validateControllers();
                }
                else {
                   validateAndSaveOnlyInformation();
                }
            }
        });

        imageChangeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checker = "clicked";

                Intent galleryIntent =new Intent(Intent.ACTION_OPEN_DOCUMENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent,IMAGE_PICK_GALLERY_CODE);
               // CropImage.activity().setAspectRatio(1,1).start(SettingsActivity.this);
            }
        });
        getUserInformation();
    }

    private void validateControllers() {
        if(TextUtils.isEmpty(nameET.getText().toString())){
            Toast.makeText(this, "Заполните поле имя", Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(phoneET.getText().toString())){
            Toast.makeText(this, "Заполните поле номер", Toast.LENGTH_SHORT).show();
        }
        else if(getType.equals("Drivers") && TextUtils.isEmpty(carET.getText().toString())){
            Toast.makeText(this, "Заполните марку автомобиля", Toast.LENGTH_SHORT).show();
        }
        else if(checker.equals("clicked"))
        {
            uploadProfileImage();
        }
    }

    private void uploadProfileImage() {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Загрузка информации");
        progressDialog.setMessage("Пожалуйста, подождите");
        progressDialog.show();

        if (imageUri != null){
            final StorageReference fileRef = storageProfileImageRef
                    .child(mAuth.getCurrentUser().getUid() + ".jpg");
            uploadTask = fileRef.putFile(imageUri);

            uploadTask.continueWithTask(new Continuation() {
                @Override
                public Object then(@NonNull Task task) throws Exception {
                    if(!task.isSuccessful()){
                        throw task.getException();
                    }
                    return  fileRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if(task.isSuccessful()){
                        Uri downloadUrl = (Uri) task.getResult();
                        myUrl = downloadUrl.toString();

                        HashMap<String, Object> userMap = new HashMap<>();
                        userMap.put("uid",mAuth.getCurrentUser().getUid());
                        userMap.put("name", nameET.getText().toString());
                        userMap.put("phone",phoneET.getText().toString());
                        userMap.put("image",myUrl);

                        if (getType.equals("Drivers")){
                            userMap.put("carname",carET.getText().toString());
                        }

                        databaseReference.child(mAuth.getCurrentUser().getUid()).updateChildren(userMap);

                        progressDialog.dismiss();

                        if (getType.equals("Drivers"))
                        {
                            startActivity(new Intent(SettingsActivity.this, DriverMapsActivity.class));
                        }else {
                            startActivity(new Intent(SettingsActivity.this, CustomersMapActivity.class));
                        }
                    }
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode==RESULT_OK&&requestCode==IMAGE_PICK_GALLERY_CODE&&data!=null){
            imageUri=data.getData();
            circleImageView.setImageURI(imageUri);
        }
        else {
            if (getType.equals("Drivers"))
            {
                startActivity(new Intent(SettingsActivity.this, DriverMapsActivity.class));
            }else {
                startActivity(new Intent(SettingsActivity.this, CustomersMapActivity.class));
            }
            Toast.makeText(this, "Произошла ошибка", Toast.LENGTH_SHORT).show();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void validateAndSaveOnlyInformation() {
        if(TextUtils.isEmpty(nameET.getText().toString())){
            Toast.makeText(this, "Заполните поле имя", Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(phoneET.getText().toString())){
            Toast.makeText(this, "Заполните поле номер", Toast.LENGTH_SHORT).show();
        }
        else if(getType.equals("Drivers") && TextUtils.isEmpty(carET.getText().toString())){
            Toast.makeText(this, "Заполните марку автомобиля", Toast.LENGTH_SHORT).show();
        }
        else {
            HashMap<String, Object> userMap = new HashMap<>();
            userMap.put("uid",mAuth.getCurrentUser().getUid());
            userMap.put("name", nameET.getText().toString());
            userMap.put("phone",phoneET.getText().toString());

            if (getType.equals("Drivers")){
                userMap.put("carname",carET.getText().toString());
            }

            databaseReference.child(mAuth.getCurrentUser().getUid()).updateChildren(userMap);

            if (getType.equals("Drivers"))
            {
                startActivity(new Intent(SettingsActivity.this, DriverMapsActivity.class));
            }else {
                startActivity(new Intent(SettingsActivity.this, CustomersMapActivity.class));
            }
        }
    }

    private void getUserInformation() {
        databaseReference.child(mAuth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0)
                {
                    String name = dataSnapshot.child("name").getValue().toString();
                    String phone  = dataSnapshot.child("phone").getValue().toString();

                    nameET.setText(name);
                    phoneET.setText(phone);


                    if (getType.equals("Drivers")) {
                        String carname = dataSnapshot.child("carname").getValue().toString();
                        carET.setText(carname);
                    }

                    if (dataSnapshot.hasChild("image")) {
                        String image = dataSnapshot.child("image").getValue().toString();
                        Picasso.get().load(image).into(circleImageView);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}