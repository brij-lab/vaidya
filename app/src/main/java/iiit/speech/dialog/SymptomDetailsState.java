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
 * Created by danda on 7/9/16.
 */
public class SymptomDetailsState extends DialogState {

    private VaidyaActivity app;
    private NLU nlu;
    private boolean expect_binary = false;
    private Map<String,String> SYMPTOM_DEF;
    private String symptom_Toexplain;

    public SymptomDetailsState(VaidyaActivity a, NLU nlu1) {
        entered = false;
        app = a;
        nlu = nlu1;
        this.setName("symptom details");
        SYMPTOM_DEF = new HashMap<>();
        System.out.println("Reading symptoms...");
        File sym_def_file = new File(app.assetDir, "symptoms_definition_file"+app.langName+".txt");
        try(BufferedReader br = new BufferedReader(new FileReader(sym_def_file))) {
            for(String line; (line = br.readLine()) != null; ) {
                // process the line.
                String[] sp = line.split(" ## ");
                SYMPTOM_DEF.put(sp[0].toLowerCase().replaceAll(" ", "_"), sp[1]);
            }
        } catch (IOException e) {
            System.out.println("Couldn't read symptom definition file");
        }
    }

    @Override
    public void onEntry() {
        entered = true;
        conclude = true;
        symptom_Toexplain = ((HealthDomain) domain).symptom_explain;
        System.out.println("HYEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE" + symptom_Toexplain.toLowerCase());
        if(app.langName.equals("_te")) {
            app.speakOut(app.getString(R.string.sym_details_symdef) + SYMPTOM_DEF.get(symptom_Toexplain.toLowerCase()), app.getString(R.string.sym_details_symdef_te) + SYMPTOM_DEF.get(symptom_Toexplain.toLowerCase()));
        }
        else{
            app.speakOut(app.getString(R.string.sym_details_symdef) + SYMPTOM_DEF.get(symptom_Toexplain.toLowerCase()), null);
        }
        app.speakOut("Do you have " + symptom_Toexplain.replaceAll("_", " "), null);
        current_grammar = app.BINARY_RESPONSE;
        next_state = "diagnosis";
    }

    @Override
    public void onRecognize(String hyp) {

    }

    @Override
    public void onExit() {

    }
}
