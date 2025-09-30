package com.mybrand.livepersondemo;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private EditText brandIdEdit;
    private EditText appInstallIdEdit;
    private android.widget.Button saveBtn;
    private android.widget.Button cancelBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        brandIdEdit = findViewById(R.id.edit_brand_id);
        appInstallIdEdit = findViewById(R.id.edit_app_install_id);
        saveBtn = findViewById(R.id.btn_save);
        cancelBtn = findViewById(R.id.btn_cancel);

        brandIdEdit.setText(getSharedPreferences("lp_demo", MODE_PRIVATE).getString("brand_id", MainActivity.BRAND_ID));
        appInstallIdEdit.setText(getSharedPreferences("lp_demo", MODE_PRIVATE).getString("app_install_id", MainActivity.APP_INSTALL_ID));

        saveBtn.setOnClickListener(v -> {
            save();
            finish();
        });

        cancelBtn.setOnClickListener(v -> finish());
    }

    private void save() {
        getSharedPreferences("lp_demo", MODE_PRIVATE)
                .edit()
                .putString("brand_id", brandIdEdit.getText().toString().trim())
                .putString("app_install_id", appInstallIdEdit.getText().toString().trim())
                .apply();
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
    }
}


