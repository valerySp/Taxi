package com.example.taxi;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class DriverRegLogin extends AppCompatActivity {

    TextView driverStatus, question;
    Button signIn,signUp;
    EditText emailET,passwordET;

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
    }
}