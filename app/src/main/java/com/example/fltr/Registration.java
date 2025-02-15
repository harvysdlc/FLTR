package com.example.fltr;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class Registration extends AppCompatActivity {

    private EditText nameEditText;
    private Spinner genderSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        nameEditText = findViewById(R.id.nameEditText);
        genderSpinner = findViewById(R.id.genderSpinner);
        Button proceedButton = findViewById(R.id.proceedButton);

        // Spinner setup
        String[] genderOptions = {"Male", "Female", "Rather not say", "LGBTQIA+"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, genderOptions);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(genderAdapter);

        proceedButton.setOnClickListener(v -> {
            String name = nameEditText.getText().toString();
            String gender = genderSpinner.getSelectedItem().toString();

            if (name.isEmpty()) {
                Toast.makeText(Registration.this, "Please enter your name or nickname.", Toast.LENGTH_SHORT).show();
            } else {
                // show name and gender in a small box here
                //Toast.makeText(Registration.this, "Name: " + name + "\nGender: " + gender, Toast.LENGTH_LONG).show();
                @SuppressLint("CutPasteId") Button proceed = findViewById(R.id.proceedButton);
                proceed.setOnClickListener(view -> {
                    Intent intent = new Intent(Registration.this, ScreenMain.class);
                    startActivity(intent);
                });
            }
        });
    }
}