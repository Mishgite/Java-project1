package com.example.java_project;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.java_project.databinding.ActivitySettingsBinding;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private SharedPreferences sharedPreferences;
    private static final String DARK_MODE_KEY = "dark_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Инициализация View Binding
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Настройка темы ДО установки контента
        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        applyTheme();

        setupUI();
    }

    private void applyTheme() {
        boolean isDarkMode = sharedPreferences.getBoolean(DARK_MODE_KEY, false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    private void setupUI() {
        // Кнопка "Назад"
        binding.buttonBack.setOnClickListener(v -> finish());

        // Кнопка переключения темы
        updateThemeButtonText();
        binding.buttonToggleTheme.setOnClickListener(v -> toggleTheme());
    }

    private void toggleTheme() {
        boolean newMode = !sharedPreferences.getBoolean(DARK_MODE_KEY, false);
        sharedPreferences.edit().putBoolean(DARK_MODE_KEY, newMode).apply();
        applyTheme();
        recreate(); // Для мгновенного применения темы
    }

    private void updateThemeButtonText() {
        binding.buttonToggleTheme.setText(
                sharedPreferences.getBoolean(DARK_MODE_KEY, false)
                        ? "Выключить тёмную тему"
                        : "Включить тёмную тему"
        );
    }
}