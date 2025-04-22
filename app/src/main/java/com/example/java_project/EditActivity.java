package com.example.java_project;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.java_project.databinding.ActivityEditBinding;

public class EditActivity extends AppCompatActivity {
    private ActivityEditBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String receivedText = getIntent().getStringExtra("recognizedText");
        if (receivedText != null) {
            binding.editText.setText(receivedText);
        }

        binding.buttonBack.setOnClickListener(v -> finish());

        binding.buttonShareText.setOnClickListener(v -> shareText());

        binding.buttonBold.setOnClickListener(v -> applyBold());

        binding.buttonItalic.setOnClickListener(v -> applyItalic());

        binding.buttonClear.setOnClickListener(v -> binding.editText.setText(""));

        binding.buttonSave.setOnClickListener(v -> {
            String editedText = binding.editText.getText().toString();
            Log.d("EditActivity", "Отредактированный текст: " + editedText);

            if (editedText.isEmpty()) {
                Toast.makeText(this, "Текст не может быть пустым", Toast.LENGTH_SHORT).show();
            } else {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("editedText", editedText);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
    }

    private void applyBold() {
        String text = binding.editText.getText().toString();
        if (!text.isEmpty()) {
            binding.editText.setText("**" + text + "**");
        }
    }

    private void applyItalic() {
        String text = binding.editText.getText().toString();
        if (!text.isEmpty()) {
            binding.editText.setText("__" + text + "__");
        }
    }

    private void shareText() {
        String textToShare = binding.editText.getText().toString().trim();

        if (textToShare.isEmpty()) {
            Toast.makeText(this, "Нет текста для отправки", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, textToShare);
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, "Поделиться через...");
        startActivity(shareIntent);
    }
}