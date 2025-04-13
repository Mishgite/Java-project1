package com.example.java_project;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class HistoryActivity extends AppCompatActivity {

    ListView listView;
    ArrayList<String> items = new ArrayList<>();
    ArrayList<String> texts = new ArrayList<>();
    HistoryDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        listView = new ListView(this);
        setContentView(listView);

        dbHelper = new HistoryDatabaseHelper(this);
        loadHistory();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedText = texts.get(position);
            showReplaceInsertDialog(selectedText);
        });
    }

    private void loadHistory() {
        items.clear();
        texts.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("history", null, null, null, null, null, "date DESC");

        while (cursor.moveToNext()) {
            String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
            String text = cursor.getString(cursor.getColumnIndexOrThrow("text"));
            items.add(date + "\n" + text);
            texts.add(text);
        }

        cursor.close();
        db.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
        listView.setAdapter(adapter);
    }

    private void showReplaceInsertDialog(String selectedText) {
        new AlertDialog.Builder(this)
                .setTitle("Выберите действие")
                .setMessage(selectedText)
                .setPositiveButton("Заменить", (dialog, which) -> {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("action", "replace");
                    resultIntent.putExtra("text", selectedText);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                })
                .setNegativeButton("Вставить", (dialog, which) -> {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("action", "insert");
                    resultIntent.putExtra("text", selectedText);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                })
                .setNeutralButton("Отмена", null)
                .show();
    }
}
