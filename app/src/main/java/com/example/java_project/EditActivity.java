package com.example.java_project;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EditActivity extends AppCompatActivity {

    private EditText editText;
    private Button buttonBold, buttonItalic, buttonClear, buttonSave;
    private ImageButton buttonBack;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        editText = findViewById(R.id.editText);
        buttonBold = findViewById(R.id.buttonBold);
        buttonItalic = findViewById(R.id.buttonItalic);
        buttonClear = findViewById(R.id.buttonClear);
        buttonSave = findViewById(R.id.buttonSave);
        buttonBack = findViewById(R.id.buttonBack);

        String receivedText = getIntent().getStringExtra("recognizedText");
        if (receivedText != null) {
            editText.setText(receivedText);
        }

        buttonBack.setOnClickListener(v -> {
            finish();
        });

        buttonBold.setOnClickListener(v -> applyBold());
        buttonItalic.setOnClickListener(v -> applyItalic());
        buttonClear.setOnClickListener(v -> editText.setText(""));
        buttonSave.setOnClickListener(v -> {
            String editedText = editText.getText().toString();
            Log.d("EditActivity", "Отредактированный текст: " + editedText);

            if (editedText.isEmpty()) {
                Toast.makeText(EditActivity.this, "Текст не может быть пустым", Toast.LENGTH_SHORT).show();
            } else {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("editedText", editedText);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
    }

    private void applyBold() {
        String text = editText.getText().toString();
        if (!text.isEmpty()) {
            editText.setText("**" + text + "**");
        }
    }

    private void applyItalic() {
        String text = editText.getText().toString();
        if (!text.isEmpty()) {
            editText.setText("_" + text + "_");
        }
    }
}
