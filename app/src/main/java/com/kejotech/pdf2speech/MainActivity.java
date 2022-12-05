package com.kejotech.pdf2speech;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;

import com.google.android.material.button.MaterialButton;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import com.kejotech.pdf2speech.databinding.ActivityMainBinding;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private TextToSpeech textToSpeech;
    private TextView outputTextView;
    private static final int READ_REQUEST_CODE = 42;
    private static final String COLON = ":";
    private int totalPage = 0;
    private int displayPage = 0;
    private Intent intent;
    private String fullPath;
    private TextView fileName;
    private MaterialButton playSpeech;
    private InputStream inputStream;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        fileName = binding.contentMain.filePath;
        playSpeech = binding.contentMain.playStop;
        progressBar = binding.contentMain.progressBar;

        outputTextView = binding.contentMain.outputText;
        outputTextView.setMovementMethod(new ScrollingMovementMethod());

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                textToSpeech.setLanguage(Locale.US);
            }
        });

        /* getting user permission for external storage */
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE}, PackageManager.PERMISSION_GRANTED);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE);
                intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
                        "application/pdf"// .pdf
                });
                startActivityForResult(intent, READ_REQUEST_CODE);
            }
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                Uri uri = resultData.getData();
                Toast.makeText(this, uri.getPath(), Toast.LENGTH_SHORT).show();
                Log.v("URI", "uri path => " + uri.getPath());
                getPdfFilePath(uri);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void getPdfFilePath(Uri uri) {
        try {
            inputStream = MainActivity.this.getContentResolver().openInputStream(uri);
            fileName.setText(uri.getPath().split(COLON)[1]);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Log.v("URI", uri.getPath()+" full path => "+ fullPath);
        ReadText();
    }

    private void ReadText() {

        new Thread(new Runnable() {
            @Override
            public void run() {
//            String stringParser;
            StringBuilder fileContent = new StringBuilder();
            try {

                PdfReader pdfReader = new PdfReader(inputStream);

                int n = pdfReader.getNumberOfPages();

                // running a for loop to get the data from PDF
                // we are storing that data inside our string.
                for (int i = 1; i <= n; i++) {
                    fileContent.append(PdfTextExtractor.getTextFromPage(pdfReader, i).trim()).append("\n");
                    // to extract the PDF content from the different pages
                }
//                builder.append(fileContent);
                pdfReader.close();
                String finalFileContent = fileContent.toString();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        outputTextView.setText(finalFileContent);
                        progressBar.setVisibility(View.GONE);
                        if (outputTextView != null){
                            playSpeech.setOnClickListener(new View.OnClickListener() {
                                @SuppressLint("UseCompatLoadingForDrawables")
                                @Override
                                public void onClick(View v) {
                                    if (!textToSpeech.isSpeaking()){
                                        Drawable iconStop = getResources().getDrawable(R.drawable.ic_baseline_stop_circle_24);
                                        playSpeech.setIcon(iconStop);
                                        textToSpeech.speak(finalFileContent, TextToSpeech.QUEUE_FLUSH,null, null);
                                    }else if (textToSpeech.isSpeaking()){
                                        Drawable iconStop = getResources().getDrawable(R.drawable.ic_baseline_play_circle_24);
                                        playSpeech.setIcon(iconStop);
                                        textToSpeech.stop();
                                    }
                                }
                            });
                        }

                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
            }
        }).start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        textToSpeech.shutdown();
        binding = null;
    }
}