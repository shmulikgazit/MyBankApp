package com.mybrand.livepersondemo;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private EditText brandIdEdit;
    private EditText appInstallIdEdit;
    private EditText issuerEdit;
    private EditText oktaClientIdEdit;
    private EditText idpDisplayNameEdit;
    private EditText redirectUriEdit;
    private android.widget.Button saveBtn;
    private android.widget.Button cancelBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        brandIdEdit = findViewById(R.id.edit_brand_id);
        appInstallIdEdit = findViewById(R.id.edit_app_install_id);
        issuerEdit = findViewById(R.id.edit_issuer);
        oktaClientIdEdit = findViewById(R.id.edit_okta_client_id);
        idpDisplayNameEdit = findViewById(R.id.edit_idp_display_name);
        redirectUriEdit = findViewById(R.id.edit_redirect_uri);
        saveBtn = findViewById(R.id.btn_save);
        cancelBtn = findViewById(R.id.btn_cancel);

        loadSettings();

        saveBtn.setOnClickListener(v -> {
            save();
            finish();
        });

        cancelBtn.setOnClickListener(v -> finish());
    }

    private void loadSettings() {
        android.content.SharedPreferences prefs = getSharedPreferences("lp_demo", MODE_PRIVATE);
        brandIdEdit.setText(prefs.getString("brand_id", MainActivity.BRAND_ID));
        appInstallIdEdit.setText(prefs.getString("app_install_id", MainActivity.APP_INSTALL_ID));
        issuerEdit.setText(prefs.getString("issuer", MainActivity.DEFAULT_ISSUER));
        oktaClientIdEdit.setText(prefs.getString("okta_client_id", ""));
        idpDisplayNameEdit.setText(prefs.getString("idp_display_name", ""));
        redirectUriEdit.setText(prefs.getString("redirect_uri", MainActivity.DEFAULT_REDIRECT_URI));
    }

    private void save() {
        getSharedPreferences("lp_demo", MODE_PRIVATE)
                .edit()
                .putString("brand_id", brandIdEdit.getText().toString().trim())
                .putString("app_install_id", appInstallIdEdit.getText().toString().trim())
                .putString("issuer", issuerEdit.getText().toString().trim())
                .putString("okta_client_id", oktaClientIdEdit.getText().toString().trim())
                .putString("idp_display_name", idpDisplayNameEdit.getText().toString().trim())
                .putString("redirect_uri", redirectUriEdit.getText().toString().trim())
                .apply();
        Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show();
    }
}
