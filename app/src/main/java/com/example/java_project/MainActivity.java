package com.example.java_project;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.java_project.databinding.ActivityMainBinding;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int SELECT_IMAGE_REQUEST = 1;
    private static final int TAKE_PHOTO_REQUEST = 2;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int EDIT_TEXT_REQUEST = 1;
    private static final int SPEECH_REQUEST_CODE = 200;

    private ActivityMainBinding binding;
    private Bitmap selectedImage;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupButtons();
        checkCameraPermission();
    }

    private void setupButtons() {
        binding.buttonEditText.setOnClickListener(v -> openEditActivity());
        binding.buttonSelectImage.setOnClickListener(v -> selectImage());
        binding.buttonRecognizeText.setOnClickListener(v -> recognizeText());
        binding.buttonTakePhoto.setOnClickListener(v -> takePhoto());
        binding.buttonScanQR.setOnClickListener(v -> scanQRCode());
        binding.buttonVoiceInput.setOnClickListener(v -> startVoiceInput());
        binding.buttonSearchGoogle.setOnClickListener(v -> searchInGoogle());
        binding.buttonSettings.setOnClickListener(v -> openSettings());
        binding.buttonHistory.setOnClickListener(v -> openHistory());
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, SELECT_IMAGE_REQUEST);
    }

    private void takePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, TAKE_PHOTO_REQUEST);
        } else {
            showToast("Camera not available");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_TEXT_REQUEST && resultCode == RESULT_OK && data != null) {
            handleEditTextResult(data);
        }

        if (requestCode == SELECT_IMAGE_REQUEST && resultCode == RESULT_OK) {
            handleImageSelection(data);
        }

        if (requestCode == TAKE_PHOTO_REQUEST && resultCode == RESULT_OK && data != null) {
            handlePhotoCapture(data);
        }

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            handleSpeechInput(data);
        }

        if (requestCode == 123 && resultCode == RESULT_OK && data != null) {
            handleHistoryResult(data);
        }
    }

    private void handleEditTextResult(Intent data) {
        String editedText = data.getStringExtra("editedText");
        if (editedText != null) {
            binding.editTextResults.setText(editedText);
            saveToHistory(editedText);
        }
    }

    private void handleImageSelection(@Nullable Intent data) {
        if (data == null || data.getData() == null) {
            showToast("Image data error!");
            return;
        }
        loadImageAsync(data.getData());
    }

    private void handlePhotoCapture(Intent data) {
        Bundle extras = data.getExtras();
        if (extras != null) {
            selectedImage = (Bitmap) extras.get("data");
            binding.imageView.setImageBitmap(selectedImage);
            enableProcessingButtons();
        }
    }

    private void handleSpeechInput(Intent data) {
        ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (result != null && !result.isEmpty()) {
            binding.editTextResults.setText(result.get(0));
        }
    }

    private void handleHistoryResult(Intent data) {
        String action = data.getStringExtra("action");
        String historyText = data.getStringExtra("text");
        if (historyText != null) {
            if ("replace".equals(action)) {
                binding.editTextResults.setText(historyText);
            } else if ("insert".equals(action)) {
                binding.editTextResults.append("\n" + historyText);
            }
        }
    }

    private void loadImageAsync(Uri imageUri) {
        executor.execute(() -> {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                handler.post(() -> {
                    selectedImage = bitmap;
                    binding.imageView.setImageBitmap(selectedImage);
                    enableProcessingButtons();
                });
            } catch (IOException e) {
                handler.post(() -> showToast("Image loading error"));
            }
        });
    }

    private void enableProcessingButtons() {
        binding.buttonRecognizeText.setEnabled(true);
        binding.buttonScanQR.setEnabled(true);
    }

    private void recognizeText() {
        if (selectedImage == null) {
            binding.editTextResults.setText("Please select image");
            return;
        }

        InputImage image = InputImage.fromBitmap(selectedImage, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        showProgress(true);
        recognizer.process(image)
                .addOnSuccessListener(this::handleTextRecognitionSuccess)
                .addOnFailureListener(e -> showError("Text recognition error: " + e.getMessage()))
                .addOnCompleteListener(task -> showProgress(false));
    }

    private void handleTextRecognitionSuccess(Text text) {
        StringBuilder result = new StringBuilder();
        for (Text.TextBlock block : text.getTextBlocks()) {
            result.append(block.getText()).append("\n");
        }
        String finalText = result.toString().trim();
        binding.editTextResults.setText(finalText.isEmpty() ? "No text found" : finalText);

        if (!finalText.isEmpty()) {
            saveToHistory(finalText);
        }
    }

    private void scanQRCode() {
        if (selectedImage == null) {
            binding.editTextResults.setText("Please select image");
            return;
        }

        InputImage image = InputImage.fromBitmap(selectedImage, 0);
        BarcodeScanner scanner = BarcodeScanning.getClient(
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                                Barcode.FORMAT_CODE_128,
                                Barcode.FORMAT_QR_CODE,
                                Barcode.FORMAT_EAN_13,
                                Barcode.FORMAT_UPC_A)
                        .build());

        showProgress(true);
        scanner.process(image)
                .addOnSuccessListener(barcodes -> handleBarcodeSuccess(barcodes))
                .addOnFailureListener(e -> showError("Barcode error: " + e.getMessage()))
                .addOnCompleteListener(task -> showProgress(false));
    }

    private void handleBarcodeSuccess(java.util.List<Barcode> barcodes) {
        if (barcodes.isEmpty()) {
            binding.editTextResults.setText("No codes found");
        } else {
            StringBuilder codes = new StringBuilder("Found codes:\n");
            for (Barcode barcode : barcodes) {
                codes.append(barcode.getRawValue()).append("\n");
            }
            binding.editTextResults.setText(codes.toString());
        }
    }

    private void copyToClipboard() {
        String text = binding.editTextResults.getText().toString();
        if (!text.isEmpty()) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("Recognized Text", text));
            showToast("Text copied to clipboard");
        } else {
            showToast("No text to copy");
        }
    }

    private void openEditActivity() {
        Intent intent = new Intent(this, EditActivity.class);
        intent.putExtra("recognizedText", binding.editTextResults.getText().toString());
        startActivityForResult(intent, EDIT_TEXT_REQUEST);
    }

    private void startVoiceInput() {
        try {
            startActivityForResult(
                    new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                            .putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now"),
                    SPEECH_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            showToast("Speech recognition not supported");
        }
    }

    private void searchInGoogle() {
        String query = binding.editTextResults.getText().toString().trim();
        if (!query.isEmpty()) {
            startActivity(new Intent(Intent.ACTION_WEB_SEARCH)
                    .putExtra(SearchManager.QUERY, query));
        } else {
            showToast("Enter search text");
        }
    }

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private void openHistory() {
        startActivityForResult(new Intent(this, HistoryActivity.class), 123);
    }

    private void saveToHistory(String text) {
        if (text.isEmpty()) return;

        try (SQLiteDatabase db = new HistoryDatabaseHelper(this).getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put("text", text);
            values.put("date", new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date()));
            db.insert("history", null, values);
        }
    }

    private void showProgress(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        binding.editTextResults.setText(message);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}