package iiit.speech.itra;

import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.Spannable;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import iiit.speech.dialog.DialogManager;
import iiit.speech.domain.DomainDesc;

public class VaidyaActivity extends Activity implements
        RecognitionListener, TextToSpeech.OnInitListener {

    /* Named searches allow to quickly reconfigure the decoder */
    public final String GREET_RESPONSE = "greet";
    public final String SYMPTOM_RESPONSE = "symptom";
    public final String BINARY_RESPONSE = "binary";
    public final String SYMPTOM_QUERY_RESPONSE = "symp_query";
    public final String DISEASE_QUERY_RESPONSE = "disease_query";
    public final String GENERIC_SEARCH = "generic";
    public final String FIRSTAID_QUERY_RESPONSE = "first_aid";
    // For google ASR
    private final int REQ_CODE_SPEECH_INPUT = 100;

    private SpeechRecognizer recognizer;
    private String current_response = GREET_RESPONSE;

    Button mic_button;
    Button reset_button;
    Button capture_button;
    TextView part_result_text;
    public TextView result_text;
    TextView caption_text;
    TextView micText;

    public int langid;
    public Locale ttsLocale;
    public String langName = "_";
    public File assetDir;
    public List<String> state_history;
    DialogManager dialogManager;

    public DomainDesc domain;

    private boolean listening = false;

    private TextToSpeech tts;
    private final int MY_DATA_CHECK_CODE = 0;

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 200;
    private Uri cameraFileUri;

    VaidyaActivity app;

    private ChatArrayAdapter chatArrayAdapter;
    private ListView listView;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        state_history = new ArrayList<>();
        // Prepare the data for UI

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            langid = extras.getInt("langid");
        }

        if(langid == 0){
            langName = langName + "en";
            ttsLocale = Locale.US;
        }
        else if(langid == 1){
            langName = langName +"hi";
            ttsLocale = new Locale("hi", "IN");
        }
        else{
            langName = langName + "te";
            ttsLocale = new Locale("te", "IN");
        }

        Locale.setDefault(ttsLocale);
        Configuration config = new Configuration();
        config.locale = ttsLocale;
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());

        setContentView(R.layout.main);

        listView = (ListView) findViewById(R.id.msgview);

        chatArrayAdapter = new ChatArrayAdapter(getApplicationContext(), R.layout.right);
        listView.setAdapter(chatArrayAdapter);

        caption_text = (TextView) findViewById(R.id.caption_text);
        caption_text.setText("Preparing the medic " + langid);



        mic_button = (Button) findViewById(R.id.btnSpeak);
        reset_button = (Button) findViewById(R.id.btnReset);
        capture_button = (Button) findViewById(R.id.btnCapture);
        //micText = (TextView) findViewById(R.id.micText);
        part_result_text = ((TextView) findViewById(R.id.partial_result_text));
        result_text = ((TextView) findViewById(R.id.result_text));
        result_text.setMovementMethod(new ScrollingMovementMethod());

        Intent checkTTSIntent = new Intent();
        checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkTTSIntent, MY_DATA_CHECK_CODE);

        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task

        app = this;

        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(VaidyaActivity.this);
                    assetDir = assets.syncAssets();
                    setupRecognizer(assetDir);

                    dialogManager = new DialogManager(app);

                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result) {
                if (result != null) {
                    ((TextView) findViewById(R.id.caption_text))
                            .setText("Failed to init recognizer " + result);
                } else {
                    //switchSearch(KWS_SEARCH);
                    if(app.langName.equals("_te")){
                        ((TextView) findViewById(R.id.caption_text)).setText(R.string.greet_patient_te);
                    }
                    else {
                        ((TextView) findViewById(R.id.caption_text)).setText(R.string.greet_patient);
                    }
                    mic_button.setClickable(true);
                    mic_button.setEnabled(true);
                    reset_button.setEnabled(true);
                    capture_button.setEnabled(true);
                }
            }
        }.execute();

        mic_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (listening) {
                    recognizer.stop();
                    listening = false;
                    //mic_button.setImageResource(R.mipmap.ico_mic);
                    //micText.setText(getString(R.string.tap_on_mic));
                    mic_button.setBackground(getDrawable(R.drawable.speak_button));
                    mic_button.setText("Listen");
                } else {
                    recognizer.startListening(current_response);
                    listening = true;
                    //mic_button.setImageResource(R.mipmap.ico_mic_run);
                    //micText.setText(getString(R.string.tap_to_stop));
                    mic_button.setBackground(getDrawable(R.drawable.stop_button));
                    mic_button.setText("Stop");
                }

                //promptSpeechInput();
            }

        });

        reset_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (dialogManager != null) {
                    result_text.setText("");
                    dialogManager.reset();
                    dialogManager.manage("HARD_RESET");
                    Toast.makeText(app, "Dialog has been reset", Toast.LENGTH_LONG).show();
                }
            }

        });

        capture_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // create Intent to take a picture and return control to the calling application
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                cameraFileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE); // create a file to save the image
                intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraFileUri); // set the image file name

                // start the image capture Intent
                startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
            }

        });
    }

    public boolean sendChatMessage(boolean side, String text, Uri imgUrl) {
        chatArrayAdapter.add(new ChatMessage(side, text, imgUrl));
        //side = !side;
        return true;
    }

    /**
     * Showing google speech input dialog
     * */
    @Deprecated
    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Receiving speech input
     * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            // For google speech recognizer
            /*case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    //txtSpeechInput.setText(result.get(0));
                    part_result_text.setText("");
                    String text = result.get(0);
                    if (text != null) {
                        //String text = hypothesis.getHypstr() + "\t";
                        //text = text + String.valueOf(hypothesis.getBestScore()) + "\t";
                        //text = text + String.valueOf(hypothesis.getProb());
                        //makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
                        //String prev_text = result_text.getText() + "<br>";
                        //result_text.setText(prev_text + Html.fromHtml(text));
                        appendColoredText(result_text, text, Color.RED);

                        // Set grammar for next dialog state
                        current_response = dialogManager.manage(text);
                    }
                }
                break;
            }*/

            case MY_DATA_CHECK_CODE: {
                if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                    //the user has the necessary data - create the TTS
                    tts = new TextToSpeech(this, this);
                }
                else {
                    //no data - install it now
                    Intent installTTSIntent = new Intent();
                    installTTSIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                    startActivity(installTTSIntent);
                }
                break;
            }

            case CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE: {
                if (resultCode == RESULT_OK) {
                    // Image captured and saved to fileUri specified in the Intent
                    Toast.makeText(this, "Image saved to:\n" +
                            cameraFileUri, Toast.LENGTH_LONG).show();
                    appendImage(true, cameraFileUri);
                } else if (resultCode == RESULT_CANCELED) {
                    // User cancelled the image capture
                } else {
                    // Image capture failed, advise user
                }
            }


        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        recognizer.cancel();
        recognizer.shutdown();
        // Don't forget to shutdown tts!
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    /** Create a file Uri for saving an image or video */
    private static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Vaidya");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("Vaidya", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    /*protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == MY_DATA_CHECK_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                //the user has the necessary data - create the TTS
                tts = new TextToSpeech(this, this);
            }
            else {
                //no data - install it now
                Intent installTTSIntent = new Intent();
                installTTSIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installTTSIntent);
            }
        }
    }*/

    @Override
    public void onInit(int initStatus) {

        if (initStatus == TextToSpeech.SUCCESS) {
            if(tts.isLanguageAvailable(ttsLocale)==TextToSpeech.LANG_AVAILABLE) {
                tts.setLanguage(ttsLocale);
                //speakOut(getString(R.string.greet_patient));
                current_response = dialogManager.manage(null);
            }
        }
        else if (initStatus == TextToSpeech.ERROR) {
            Toast.makeText(this, "Sorry! Text To Speech failed...", Toast.LENGTH_LONG).show();
        }

    }

    public void speakOut(String stxt, String wtxt) {

        //appendColoredText(result_text, txt, Color.YELLOW);
        if (wtxt != null) {
            sendChatMessage(true, wtxt, null);
        } else {
            sendChatMessage(true, stxt, null);
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(stxt, TextToSpeech.QUEUE_ADD, null, null);
        } else {
            tts.speak(stxt, TextToSpeech.QUEUE_ADD, null);
        }

    }

    /**
     * In partial result we get quick updates about current hypothesis. In
     * keyword spotting mode we can react here, in other modes we need to wait
     * for final result in onResult.
     */
    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
    	    return;

        String text = hypothesis.getHypstr();
        /*if (text.equals(KEYPHRASE))
            switchSearch(MENU_SEARCH);
        else if (text.equals(DIGITS_SEARCH))
            switchSearch(DIGITS_SEARCH);
        else if (text.equals(PHONE_SEARCH))
            switchSearch(PHONE_SEARCH);
        else if (text.equals(FORECAST_SEARCH))
            switchSearch(FORECAST_SEARCH);
        else*/
        part_result_text.setText(text);
    }

    /**
     * This callback is called when we stop the recognizer.
     */
    @Override
    public void onResult(Hypothesis hypothesis) {
        part_result_text.setText("");

        if (hypothesis != null) {
            String text = hypothesis.getHypstr() + "\t";
         //   text = text + String.valueOf(hypothesis.getBestScore()) + "\t";
         //   text = text + String.valueOf(hypothesis.getProb());
            //makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
            //String prev_text = result_text.getText() + "<br>";
            //result_text.setText(prev_text + Html.fromHtml(text));

            //appendColoredText(result_text, text, Color.RED);
            sendChatMessage(false, text, null);
            // Set grammar for next dialog state
            current_response = dialogManager.manage(hypothesis.getHypstr());
        }
    }

    @Deprecated
    public void appendColoredText(TextView tv, String text, int color) {
        int start = tv.getText().length();
        tv.append(text + "\n");
        int end = tv.getText().length();

        Spannable spannableText = (Spannable) tv.getText();
        spannableText.setSpan(new ForegroundColorSpan(color), start, end, 0);
    }

    public void appendImage(boolean left, Uri imageUri) {

        sendChatMessage(left, imageUri.toString(), imageUri);
        /*
        Spannable spannableText = (Spannable) tv.getText();

        Bitmap imageSpan = BitmapFactory.decodeFile(imageUri.getPath());
        Drawable d = new BitmapDrawable(imageSpan);
        List<String> impath = imageUri.getPathSegments();
        String imname = impath.get(impath.size() - 1);
        Integer imid = Integer.valueOf(imname.split(".")[0].split("_")[1]);
        */
        //spannableText.setSpan(imageSpan, tv.getText().length(), tv.getText().length() + 1, 0);
        //tv.setText(spannableText);
       // tv.setCompoundDrawablesWithIntrinsicBounds(0,0,d,0);
    }

    @Override
    public void onBeginningOfSpeech() {
        //mic_button.setImageResource(R.mipmap.ico_mic_run);
        //micText.setText(getString(R.string.tap_to_stop));
        mic_button.setBackground(getDrawable(R.drawable.stop_button));
        mic_button.setText("Stop");
        listening = true;
    }

    /**
     * We stop recognizer here to get a final result
     */
    @Override
    public void onEndOfSpeech() {
        //if (!recognizer.getSearchName().equals(KWS_SEARCH))
        //    switchSearch(KWS_SEARCH);
        //mic_button.setImageResource(R.mipmap.ico_mic);
        mic_button.setBackground(getDrawable(R.drawable.speak_button));
        mic_button.setText("Speak");
        recognizer.stop();
        //micText.setText(getString(R.string.tap_on_mic));
        listening = false;
    }

    private void switchSearch(String searchName) {
        recognizer.stop();

        // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).
        /*if (searchName.equals(KWS_SEARCH))
            recognizer.startListening(searchName);
        else
            recognizer.startListening(searchName, 10000);

        String caption = getResources().getString(captions.get(searchName));
        ((TextView) findViewById(R.id.caption_text)).setText(caption);*/
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them

        recognizer = defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))

                // To disable logging of raw audio comment out this call (takes a lot of space on the device)
                .setRawLogDir(assetsDir)

                // Threshold to tune for keyphrase to balance between false alarms and misses
                .setKeywordThreshold(1e-45f)

                // Use context-independent phonetic search, context-dependent is too slow for mobile
                .setBoolean("-allphone_ci", true)

                .getRecognizer();
        recognizer.addListener(this);

        /** In your application you might not need to add all those searches.
         * They are added here for demonstration. You can leave just one.
         */

        // Create keyword-activation search.
        //recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);

        // Create grammar-based search for selection between demos
        //File menuGrammar = new File(assetsDir, "menu_en.gram");
        //recognizer.addGrammarSearch(MENU_SEARCH, menuGrammar);

        // Create grammar-based search for digit recognition

        File greetingGrammar = new File(assetsDir, "greet"+langName+".gram");
        recognizer.addGrammarSearch(GREET_RESPONSE, greetingGrammar);

        // Create grammar-based search for digit recognition
        File symptomGrammar = new File(assetsDir, "symptom"+langName+".gram");
        recognizer.addGrammarSearch(SYMPTOM_RESPONSE, symptomGrammar);

        // Create grammar-based search for digit recognition
        File binaryGrammar = new File(assetsDir, "binary"+langName+".gram");
        recognizer.addGrammarSearch(BINARY_RESPONSE, binaryGrammar);

        File symQueryGrammar = new File(assetsDir, "symptom_query_response"+langName+".gram");
        recognizer.addGrammarSearch(SYMPTOM_QUERY_RESPONSE, symQueryGrammar);

        File disQueryGrammar = new File(assetsDir,"menu"+langName+".gram");
        recognizer.addGrammarSearch(DISEASE_QUERY_RESPONSE, disQueryGrammar);

        File firstaidQueryGrammar = new File(assetsDir, "firstaid"+langName+".gram");
        recognizer.addGrammarSearch(FIRSTAID_QUERY_RESPONSE, firstaidQueryGrammar);
        // Create language model search
        File languageModel = new File(assetsDir, "health.lm.dmp");
        recognizer.addNgramSearch(GENERIC_SEARCH, languageModel);

        //File keyphrases = new File(assetsDir, "symp_edit_dist.old.txt");
        //recognizer.addKeywordSearch(FORECAST_SEARCH, keyphrases);

        // Phonetic search
        //File phoneticModel = new File(assetsDir, "en-phone.dmp");
        //recognizer.addAllphoneSearch(PHONE_SEARCH, phoneticModel);
    }

    @Override
    public void onError(Exception error) {
        ((TextView) findViewById(R.id.caption_text)).setText(error.getMessage());
    }

    @Override
    public void onTimeout() {
        switchSearch(current_response);
    }
}
