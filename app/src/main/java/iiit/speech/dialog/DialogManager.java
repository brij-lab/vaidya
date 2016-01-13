package iiit.speech.dialog;

import android.app.Activity;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import iiit.speech.domain.DomainDesc;
import iiit.speech.itra.VaidyaActivity;
import iiit.speech.nlu.NLU;

/**
 * Created by brij on 3/9/15.
 */
public class DialogManager {

    private VaidyaActivity app;
    private NLU nlu;
    private DomainDesc domain;
    private Map<String, DialogState> states;
    public String current_state;

    public DialogManager(VaidyaActivity a) {
        app = a;
        try {
            nlu = new NLU(a);
        } catch (IOException e) {

        }
        initStates();
    }

    private void initStates() {
        states = new LinkedHashMap<>();
        GreetState greetState = new GreetState(app, nlu);
        states.put("greet", greetState);
        AskSymptomsState askSymptomsState = new AskSymptomsState(app, nlu);
        states.put("ask_symptoms", askSymptomsState);
        DiagnosisState diagnosisState = new DiagnosisState(app, nlu);
        states.put("diagnosis", diagnosisState);
        DiseaseDetailsState diseaseDetailsState = new DiseaseDetailsState(app, nlu);
        states.put("disease_details", diseaseDetailsState);

        current_state = "greet";
    }

    public String manage(String hyp) {

        DialogState state = states.get(current_state);

        if (nlu.isValidString(hyp)) {
            if (!state.entered) {
                state.onEntry();
            }
            else if (!state.conclude) {
                state.onRecognize(hyp);
                if (state.conclude) {
                    state.onExit();
                    current_state = state.next_state;
                    if (current_state.equals("reset")) {
                        reset();
                    } else {
                        states.get(current_state).domain = state.domain;
                        //manage(null);
                        state = states.get(current_state);
                        state.onEntry();
                    }
                }
            }
        }
        else
        {
            app.speakOut("Please repeat");
        }

        System.out.println("Current state ======> " + state.getName());
        System.out.println("Current grammar ======> " + state.current_grammar);

        return state.current_grammar;
    }

    public void reset() {
        initStates();
    }
}
