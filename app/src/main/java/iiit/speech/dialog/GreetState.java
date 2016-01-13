package iiit.speech.dialog;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import iiit.speech.domain.DomainDesc;
import iiit.speech.itra.VaidyaActivity;
import iiit.speech.nlu.NLU;

/**
 * Created by brij on 4/9/15.
 */
public class GreetState extends DialogState {


    public static Map<String, String> ack_message;

    private VaidyaActivity app;
    private NLU nlu;
    public boolean expect_binary = false;

    public GreetState(VaidyaActivity a, NLU nlu1) {
        entered = false;
        app = a;
        nlu = nlu1;
        this.setName("greet");
        ack_message = new HashMap<>();
        ack_message.put("health", "Do you want me to diagnose your symptoms?");
        ack_message.put("nodomain", "Sorry. I did not get that. Can you please repeat?");
    }

    @Override
    public void onEntry() {
        System.out.println("+++++++++++++++++ Greet state entered +++++++++++++++++++++");
        app.speakOut("How may I help you?");

        // Set appropriate grammar
        current_grammar =  app.GENERIC_SEARCH;
        entered = true;
        next_state = "greet";
        expect_binary = false;
    }

    @Override
    public void onRecognize(String hypothesis) {

        if (expect_binary) {
            // Normalize binary response to either positive or negative
            if (nlu.resolveBinaryHyp(hypothesis)) {
                conclude = true;
                next_state = "ask_symptoms";
            }
            else
            {
                expect_binary = false;
                current_grammar = app.GENERIC_SEARCH;
                app.speakOut("Can I help you with something else?");
            }
        }
        else
        {
            try {
                domain = nlu.getDomain(hypothesis);
            } catch (IOException e) {

            }
            app.speakOut(ack_message.get(domain.getName()));
            switch (domain.getName()) {
                case "health":
                    expect_binary = true;
                    current_grammar = app.BINARY_RESPONSE;
                    break;
                case "nodomain":
                    expect_binary = false;
                    current_grammar = app.GENERIC_SEARCH;
                    break;
            }
        }
    }

    @Override
    public void onExit() {

    }
}
