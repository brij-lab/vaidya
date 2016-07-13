package iiit.speech.itra;

import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.Spannable;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import iiit.speech.dialog.DialogManager;

public class VaidyaActivity extends Activity implements
        RecognitionListener, TextToSpeech.OnInitListener {

    /* Named searches allow to quickly reconfigure the decoder */
    public final String GREET_RESPONSE = "greet";
    public final String SYMPTOM_RESPONSE = "symptom";
    public final String BINARY_RESPONSE = "binary";
    public final String SYMPTOM_QUERY_RESPONSE = "symp_query";
    public final String GENERIC_SEARCH = "generic";

    private final int REQ_CODE_SPEECH_INPUT = 100;

    private SpeechRecognizer recognizer;
    private String current_response = GREET_RESPONSE;

    Button mic_button;
    Button reset_button;
    TextView part_result_text;
    public TextView result_text;
    TextView caption_text;
    TextView micText;

    public int langid;
    public String langName = "_";
    public File assetDir;

    DialogManager dialogManager;

    private boolean listening = false;

    private TextToSpeech tts;
    private final int MY_DATA_CHECK_CODE = 0;

    VaidyaActivity app;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        // Prepare the data for UI
        setContentView(R.layout.main);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            langid = extras.getInt("langid");
        }
        caption_text = (TextView) findViewById(R.id.caption_text);
        caption_text.setText("Preparing the medic " + langid);

        if(langid == 0){
            langName = langName + "en";
        }
        else if(langid == 1){
            langName = langName +"hi";
        }
        else{
            langName = langName + "te";
        }

        mic_button = (Button) findViewById(R.id.btnSpeak);
        reset_button = (Button) findViewById(R.id.btnReset);
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
                    ((TextView) findViewById(R.id.caption_text)).setText(R.string.greet_patient);
                    mic_button.setClickable(true);
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
                    mic_button.setText("Speak");
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
                    dialogManager.manage(null);
                    Toast.makeText(app, "Dialog has been reset", Toast.LENGTH_LONG).show();
                }
            }

        });
    }

    /**
     * Showing google speech input dialog
     * */
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
            case REQ_CODE_SPEECH_INPUT: {
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
            }

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
            if(tts.isLanguageAvailable(Locale.US)==TextToSpeech.LANG_AVAILABLE) {
                tts.setLanguage(Locale.US);
                //speakOut(getString(R.string.greet_patient));
                current_response = dialogManager.manage(null);
            }
        }
        else if (initStatus == TextToSpeech.ERROR) {
            Toast.makeText(this, "Sorry! Text To Speech failed...", Toast.LENGTH_LONG).show();
        }

    }

    public void speakOut(String txt) {

        appendColoredText(result_text, txt, Color.YELLOW);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(txt, TextToSpeech.QUEUE_ADD, null, null);
        } else {
            tts.speak(txt, TextToSpeech.QUEUE_ADD, null);
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
            text = text + String.valueOf(hypothesis.getBestScore()) + "\t";
            text = text + String.valueOf(hypothesis.getProb());
            //makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
            //String prev_text = result_text.getText() + "<br>";
            //result_text.setText(prev_text + Html.fromHtml(text));
            appendColoredText(result_text, text, Color.RED);

            // Set grammar for next dialog state
            current_response = dialogManager.manage(hypothesis.getHypstr());
        }
    }

    public void appendColoredText(TextView tv, String text, int color) {
        int start = tv.getText().length();
        tv.append(text + "\n");
        int end = tv.getText().length();

        Spannable spannableText = (Spannable) tv.getText();
        spannableText.setSpan(new ForegroundColorSpan(color), start, end, 0);
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
