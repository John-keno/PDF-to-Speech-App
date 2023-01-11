package com.kejotech.pdf2speech;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import com.kejotech.pdf2speech.databinding.ActivityMainBinding;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
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
    private MaterialButton save;
    private InputStream inputStream;
    private ProgressBar progressBar;
    private MediaPlayer mediaPlayer;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

//        setSupportActionBar(binding.toolbar);


        fileName = binding.contentMain.filePath;
        playSpeech = binding.contentMain.playStop;
        save = binding.contentMain.stop;
        progressBar = binding.contentMain.progressBar;
        mediaPlayer = new MediaPlayer();

        mProgressDialog = new ProgressDialog(MainActivity.this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage("Converting PDF to Audio");



        outputTextView = binding.contentMain.outputText;
        outputTextView.setMovementMethod(new ScrollingMovementMethod());

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String setVoice = sharedPreferences.getString("voice","male");
        int setSpeechRate = sharedPreferences.getInt("speech rate", 10);
        float speechRate = setSpeechRate/10.0f;


        Set<String> a = new HashSet<>();
        a.add("set voices");//here you can give male if you want to select male voice.
        Voice female = new Voice("en-us-x-ioc-local", new Locale("en_US"), 400, 200, false,a);
        Voice male = new Voice("en-us-x-iom-local", new Locale("en_US"), 400, 200, false,a);

        textToSpeech = new TextToSpeech(getApplicationContext(), status -> {

            if (status == TextToSpeech.SUCCESS) {
                Toast.makeText(getApplicationContext(),"engine initialized",Toast.LENGTH_LONG).show();
                int result = 0;
                if(setVoice.equals("male")){
                    textToSpeech.setVoice(male);
                    result = textToSpeech.setVoice(male);
                }
                else if(setVoice.equals("female")){
                    textToSpeech.setVoice(female);
                    result = textToSpeech.setVoice(female);
                }
                textToSpeech.setSpeechRate(speechRate);
                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "This Language is not supported");
                }

            } else {
                Log.e("TTS", "Initialization Failed!");
            }
        },"com.google.android.tts");






        /* getting user permission for external storage */
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE}, PackageManager.PERMISSION_GRANTED);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
                        "application/pdf"// .pdf
                });
                startActivityForResult(intent, READ_REQUEST_CODE);
            }
        });
        save.setEnabled(false);
        playSpeech.setEnabled(false);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                progressBar.setVisibility(View.VISIBLE);
                if (mediaPlayer != null){
                    @SuppressLint("UseCompatLoadingForDrawables")
                    Drawable iconStop = getResources().getDrawable(R.drawable.ic_baseline_play_arrow_24);
                    playSpeech.setIcon(iconStop);
                    mediaPlayer.stop();
                    mediaPlayer = new MediaPlayer();
                }
                Uri uri = resultData.getData();
                Toast.makeText(this, uri.getPath(), Toast.LENGTH_SHORT).show();
                Log.v("URI", "uri path => " + uri.getPath());
                getPdfFilePath(uri);
                mProgressDialog.show();
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
                    fileContent.append("page ").append(i).append(".\n\n").append(PdfTextExtractor.getTextFromPage(pdfReader, i).trim()).append("\n\n");
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
                        if (outputTextView != null && finalFileContent.length() < TextToSpeech.getMaxSpeechInputLength()){
                            if(!save.isEnabled() && !playSpeech.isEnabled()){
                                save.setEnabled(true);
                                playSpeech.setEnabled(true);
                            }
                            String path = saveSpeechToAudio(finalFileContent);

                            textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                                @Override
                                public void onStart(String utteranceId) {

                                }

                                @Override
                                public void onDone(String utteranceId) {
                                    mProgressDialog.dismiss();
                                    try {
                                        mediaPlayer.setDataSource(path);
                                        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                                        mediaPlayer.prepare();

                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        Log.d(TAG, "onClick media: "+e.getLocalizedMessage());
                                    }
                                }

                                @Override
                                public void onError(String utteranceId) {

                                }
                            });

                            save.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (mediaPlayer != null) {
                                        @SuppressLint("UseCompatLoadingForDrawables")
                                        Drawable iconStop = getResources().getDrawable(R.drawable.ic_baseline_play_arrow_24);
                                        playSpeech.setIcon(iconStop);
                                        mediaPlayer.seekTo(0);
                                        mediaPlayer.pause();

                                    }
                                }
                            });




                            playSpeech.setOnClickListener(new View.OnClickListener() {
                                @SuppressLint("UseCompatLoadingForDrawables")
                                @Override
                                public void onClick(View v) {
                                    if (!mediaPlayer.isPlaying()){
                                        Drawable iconStop = getResources().getDrawable(R.drawable.ic_baseline_pause_24);
                                        if (mediaPlayer != null) {
                                            mediaPlayer.start();
                                        }

                                        playSpeech.setIcon(iconStop);

                                    }else if (mediaPlayer.isPlaying()){
                                        Drawable iconStop = getResources().getDrawable(R.drawable.ic_baseline_play_arrow_24);
                                        playSpeech.setIcon(iconStop);
                                        if (mediaPlayer != null) {
                                            mediaPlayer.pause();
                                        }
                                    }
                                }
                            });
                            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                @Override
                                public void onCompletion(MediaPlayer mp) {
                                    Toast.makeText(getApplicationContext(), "Done Playing Audio",Toast.LENGTH_LONG).show();
                                    @SuppressLint("UseCompatLoadingForDrawables")
                                    Drawable iconStop = getResources().getDrawable(R.drawable.ic_baseline_play_arrow_24);
                                    playSpeech.setIcon(iconStop);
                                }
                            });
                        }else {
                            if(save.isEnabled() && playSpeech.isEnabled()){
                                save.setEnabled(false);
                                playSpeech.setEnabled(false);
                            }
                            mProgressDialog.dismiss();
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setCancelable(false);
                            builder.setTitle("ERROR");
                            builder.setMessage("File too large");
                            builder.setPositiveButton("Dismiss", (dialog, which) -> dialog.dismiss());

                            AlertDialog dialog = builder.create();
                            dialog.show();

                        }

                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
            }
        }).start();
    }
    private String saveSpeechToAudio(String speakTextTxt) {

        HashMap<String, String> myHashRender = new HashMap<String, String>();
        myHashRender.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, speakTextTxt);

        String exStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/PDIO Audio";
        File appTmpPath = new File(exStoragePath);
        appTmpPath.mkdirs();
        String name = "/"+fileName.getText().toString();
        String finalName = name.substring(name.lastIndexOf("/")+1,name.lastIndexOf("."));
        String tempFilename = finalName+".wav";
        String tempDestFile = appTmpPath.getAbsolutePath() + "/" + tempFilename;
        textToSpeech.synthesizeToFile(speakTextTxt, myHashRender, tempDestFile);
        return tempDestFile;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            mediaPlayer.stop();
            mediaPlayer = new MediaPlayer();
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        textToSpeech.stop();
        textToSpeech.shutdown();
        binding = null;
    }
}