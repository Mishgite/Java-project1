package com.example.java_project;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class SettingsActivity extends AppCompatActivity {

    private Button buttonToggleTheme;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Загружаем сохранённую тему перед отрисовкой UI
        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        if (sharedPreferences.getBoolean("dark_mode", false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        buttonToggleTheme = findViewById(R.id.buttonToggleTheme);
        Button buttonBack = findViewById(R.id.buttonBack);

        updateButtonText(); // Устанавливаем правильный текст на кнопке

        buttonToggleTheme.setOnClickListener(v -> toggleTheme());
        buttonBack.setOnClickListener(v -> finish()); // Кнопка назад
    }

    private void toggleTheme() {
        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            editor.putBoolean("dark_mode", false);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            editor.putBoolean("dark_mode", true);
        }
        editor.apply();

        updateButtonText();
        recreate(); // Перезапускаем активность, чтобы применить изменения
    }

    private void updateButtonText() {
        if (sharedPreferences.getBoolean("dark_mode", false)) {
            buttonToggleTheme.setText("Выключить тёмную тему");
        } else {
            buttonToggleTheme.setText("Включить тёмную тему");
        }
    }
}
