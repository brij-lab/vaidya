package iiit.speech.dialog;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

import iiit.speech.domain.DomainDesc;
import iiit.speech.domain.HealthDomain;
import iiit.speech.itra.VaidyaActivity;
import iiit.speech.nlu.NLU;

/**
 * Created by brij on 5/9/15.
 */
public class AskSymptomsState extends DialogState {

    private VaidyaActivity app;
    private NLU nlu;
    Random rand;

    public boolean expect_binary = false;
    public int symptom_request_count = 0;

    public AskSymptomsState(VaidyaActivity a, NLU nlu1) {
        entered = false;
        app = a;
        nlu = nlu1;
        rand = new Random();
        this.setName("ask_symptoms");
    }

    @Override
    public void onExit() {

    }

    @Override
    public void onRecognize(String hyp) {
        String current_symptom = ((HealthDomain) domain).getSingleUnacknowledged();
        if (expect_binary && current_symptom != null) {
            // If symptom is present mark it as acknowledged otherwise put it in removed_set
            if (nlu.resolveBinaryHyp(hyp)) {
                System.out.println("Marking Symptom ===> " + current_symptom);
                ((HealthDomain) domain).markSymptomAcknowledged(current_symptom);
            } else {
                ((HealthDomain) domain).removeSymptom(current_symptom);
            }
            expect_binary = false;

        } else {
            // Extract symptoms from hyp
            if (!nlu.checkNegative(hyp)) {
                List<String> symps = nlu.findSymptomsInHyp(hyp);
                for (String symp : symps) {
                    HealthDomain.SymptomStatus symp_status = ((HealthDomain) domain).getSymptomStatus(symp);
                    System.out.println("Extracted symptom from hypothesis : " + symp + " == status : " + symp_status.name());
                    switch (symp_status) {
                        case NOT_PRESENT:
                            // Symptom does not exists
                            ((HealthDomain) domain).addSymptoms(symp);
                            break;
                        case REMOVED:
                            app.speakOut("You told that you do not have " + symp);
                            ((HealthDomain) domain).markRemovedSymptomUnAcknowledged(symp);
                            break;
                    }
                }
            }
        }
        completeSymptomList();
        current_symptom = ((HealthDomain) domain).getSingleUnacknowledged();
        if (current_symptom == null && symptom_request_count == 2) {
            next_state = "diagnosis";
            conclude = true;
        } else {
            next_state = "ask_symptoms";
        }
    }

    @Override
    public void onEntry() {
        completeSymptomList();
        entered = true;
        next_state = "ask_symptoms";
    }

    private void completeSymptomList() {
        String current_symptom = ((HealthDomain) domain).getSingleUnacknowledged();
        ((HealthDomain) domain).displaySymptomList();
        System.out.println("Current Symptom ===> " + current_symptom);
        System.out.println("Previous states ===> ");
        for (String st : app.state_history) {
            System.out.println(st);
        }
        //if (current_symptom == null) {
        if (app.state_history.get(app.state_history.size() - 1).equalsIgnoreCase("disease_enquiry")) {
            symptom_request_count = 2;
        }
        if (symptom_request_count < 2) {
            // There are no un-acknowledged symptoms in the list obtained up till now
            switch (symptom_request_count) {
                case 0:
                    app.speakOut("Please tell your symptoms.");
                    break;
                case 1:
                    app.speakOut("Please tell if you have any other symptoms.");
                    break;
            }
            // Set appropriate grammar
            current_grammar =  app.SYMPTOM_RESPONSE;
            symptom_request_count++;

        } else if (current_symptom != null) {
            // TODO merge to accept natural language as response
            app.speakOut("Do you have " + current_symptom.replaceAll("_", " ") + "?");
            // Set appropriate grammar
            current_grammar =  app.BINARY_RESPONSE;
            expect_binary = true;
        }
    }
}
