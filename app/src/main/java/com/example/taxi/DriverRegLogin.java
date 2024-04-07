package com.example.taxi;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class DriverRegLogin extends AppCompatActivity {

    TextView driverStatus, question;
    Button signIn,signUp;
    EditText emailET,passwordET;
    FirebaseAuth mAuth;
    ProgressDialog loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_reg_login);

        driverStatus=findViewById(R.id.statusDriver);
        question=findViewById(R.id.accCreate);
        signIn=findViewById(R.id.signInDriver);
        signUp=findViewById(R.id.signUpDriver);
        emailET=findViewById(R.id.driverEmail);
        passwordET=findViewById(R.id.driverPassword);

        signUp.setVisibility(View.INVISIBLE);
        signUp.setEnabled(false);

        mAuth=FirebaseAuth.getInstance();
        loadingBar=new ProgressDialog(this);

        question.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn.setVisibility(View.INVISIBLE);
                question.setVisibility(View.INVISIBLE);
                signUp.setVisibility(View.VISIBLE);
                signUp.setEnabled(true);
                driverStatus.setText("Регистрация для водителя");
            }
        });

        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email=emailET.getText().toString();
                String password=passwordET.getText().toString();

                registerDriver(email,password);
            }
        });

        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email=emailET.getText().toString();
                String password=passwordET.getText().toString();

                signInDriver(email,password);
            }
        });
    }

    private void signInDriver(String email, String password) {
        loadingBar.setTitle("Вход водителя");
        loadingBar.setMessage("Дождитесь загрузки");
        loadingBar.show();

        mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()){
                    Toast.makeText(DriverRegLogin.this,"Успешный вход",Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();
                    Intent intent=new Intent(DriverRegLogin.this,DriverMapsActivity.class);
                    startActivity(intent);

                } else {
                    Toast.makeText(DriverRegLogin.this,"Ошибка",Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();
                }
            }
        });
    }


    private void registerDriver(String email, String password) {
        loadingBar.setTitle("Регистрация водителя");
        loadingBar.setMessage("Дождитесь загрузки");
        loadingBar.show();

        mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()){
                    Toast.makeText(DriverRegLogin.this,"Регистрация прошла успешно",Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();
                    Intent intent=new Intent(DriverRegLogin.this,DriverMapsActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(DriverRegLogin.this,"Ошибка",Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();
                }
            }
        });
    }
}