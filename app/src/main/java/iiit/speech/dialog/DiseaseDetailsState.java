package iiit.speech.dialog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import iiit.speech.domain.HealthDomain;
import iiit.speech.itra.R;
import iiit.speech.itra.VaidyaActivity;
import iiit.speech.nlu.NLU;

/**
 * Created by brij on 17/9/15.
 */
public class DiseaseDetailsState extends DialogState {

    private VaidyaActivity app;
    private NLU nlu;

    private boolean expect_binary = false;
    private Map<String, String> DISEASE_DEF;
    private String disease;

    public DiseaseDetailsState(VaidyaActivity a, NLU nlu1) {
        entered = false;
        app = a;
        nlu = nlu1;
        this.setName("disease_details");

        DISEASE_DEF = new HashMap<>();

        System.out.println("Reading disease definitions...");
        File dis_def_file = new File(app.assetDir, "definition_file.txt");
        try(BufferedReader br = new BufferedReader(new FileReader(dis_def_file))) {
            for(String line; (line = br.readLine()) != null; ) {
                // process the line.
                String[] sp = line.split(" ## ");
                DISEASE_DEF.put(sp[0].toLowerCase(), sp[1].replaceAll("_", " "));
            }
        } catch (IOException e) {
            System.out.println("Couldn't read disease definition file");
        }
    }

    @Override
    public void onEntry() {
        current_grammar =  app.BINARY_RESPONSE;
        entered = true;

        disease = ((HealthDomain) domain).getDisease();
        if (disease != null && app.state_history.get(app.state_history.size()-2) == "disease_enquiry") {
            expect_binary = true;
        }
        else if(disease != null){
            if(app.langName.equals("_te")) {
                app.speakOut(app.getString(R.string.disease_details_ask), app.getString(R.string.disease_details_ask_te));
            }
            else{
                app.speakOut(app.getString(R.string.disease_details_ask), null);
            }
                expect_binary = true;
            }
        }


    @Override
    public void onRecognize(String hyp) {
        if (nlu.resolveBinaryHyp(hyp)) {
            app.speakOut(app.getString(R.string.disease_details_tell), DISEASE_DEF.get(disease.toLowerCase()));
        } else {
            if(app.langName.equals("_te")) {
                app.speakOut(app.getString(R.string.consult), app.getString(R.string.consult_te));
            }
            else{
                app.speakOut(app.getString(R.string.consult), null);
            }
        }
        conclude = true;
    }

    @Override
    public void onExit() {
        next_state = "reset";
    }
}
