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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CustomerRegLog extends AppCompatActivity {

    TextView customerStatus, question;
    Button signIn,signUp;
    EditText emailET,passwordET;

    FirebaseAuth mAuth;
    DatabaseReference customerDataBaseRef;
    String onLineCustomerID;
    ProgressDialog loadingBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_reg_log);

        customerStatus=findViewById(R.id.statusCustomer);
        question=findViewById(R.id.accCreate);
        signIn=findViewById(R.id.signInCustomer);
        signUp=findViewById(R.id.signUpCustomer);
        emailET=findViewById(R.id.customerEmail);
        passwordET=findViewById(R.id.customerPassword);

        signUp.setVisibility(View.INVISIBLE);
        signUp.setEnabled(false);

        mAuth=FirebaseAuth.getInstance();
        loadingBar=new ProgressDialog(this);

        onLineCustomerID=mAuth.getCurrentUser().getUid();
        customerDataBaseRef= FirebaseDatabase.getInstance().getReference().child("Users").child("Customers")
                        .child(onLineCustomerID);

        question.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn.setVisibility(View.INVISIBLE);
                question.setVisibility(View.INVISIBLE);
                signUp.setVisibility(View.VISIBLE);
                signUp.setEnabled(true);
                customerStatus.setText("Регистрация для клиента");
            }
        });

        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email=emailET.getText().toString();
                String password=passwordET.getText().toString();

                registerCustomer(email,password);
            }
        });
        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email=emailET.getText().toString();
                String password=passwordET.getText().toString();

                signInCustomer(email,password);
            }
        });
    }

    private void signInCustomer(String email, String password) {
        loadingBar.setTitle("Вход клиента");
        loadingBar.setMessage("Дождитесь загрузки");
        loadingBar.show();

        mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()){
                    Toast.makeText(CustomerRegLog.this,"Успешный вход",Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();
                    Intent intent=new Intent(CustomerRegLog.this,CustomersMapActivity.class);
                    startActivity(intent);

                } else {
                    Toast.makeText(CustomerRegLog.this,"Ошибка",Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();
                }
            }
        });
    }

    private void registerCustomer(String email, String password) {
        loadingBar.setTitle("Регистрация клиента");
        loadingBar.setMessage("Дождитесь загрузки");
        loadingBar.show();

        mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()){
                    onLineCustomerID=mAuth.getCurrentUser().getUid();
                    customerDataBaseRef= FirebaseDatabase.getInstance().getReference().child("Users").child("Customers")
                            .child(onLineCustomerID);
                    customerDataBaseRef.setValue(true);

                    Toast.makeText(CustomerRegLog.this,"Регистрация прошла успешно",Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();
                } else {
                    Toast.makeText(CustomerRegLog.this,"Ошибка",Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();
                }
            }
        });
    }
}