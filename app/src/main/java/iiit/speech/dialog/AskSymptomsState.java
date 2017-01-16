package iiit.speech.dialog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;

import iiit.speech.domain.DomainDesc;
import iiit.speech.domain.HealthDomain;
import iiit.speech.itra.R;
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
        Integer current_symptom = ((HealthDomain) domain).getSingleUnacknowledged();
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
                List<Integer> symps = nlu.findSymptomsInHyp(hyp);
                for (Integer symp : symps) {
                    HealthDomain.SymptomStatus symp_status = ((HealthDomain) domain).getSymptomStatus(symp);
                    System.out.println("Extracted symptom from hypothesis : " + symp + " == status : " + symp_status.name());
                    switch (symp_status) {
                        case NOT_PRESENT:
                            // Symptom does not exists
                            ((HealthDomain) domain).addSymptoms(symp);
                            break;
                        case REMOVED:
                            if(app.langName.equals("_te")) {
                                app.speakOut(app.getString(R.string.ack_conflict, ((HealthDomain) domain).LCONCEPT_SYMPTOM_MAP.get(app.langName).get(symp)), app.getString(R.string.ack_conflict_te, ((HealthDomain) domain).LCONCEPT_SYMPTOM_MAP.get(app.langName).get(symp)));
                            }
                            else{
                                app.speakOut(app.getString(R.string.ack_conflict, ((HealthDomain) domain).LCONCEPT_SYMPTOM_MAP.get(app.langName).get(symp)), null);
                            }
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
        Integer current_symptom = ((HealthDomain) domain).getSingleUnacknowledged();
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
                    if(app.langName.equals("_te")) {
                        app.speakOut(app.getString(R.string.req_symptoms1),app.getString(R.string.req_symptoms1_te) );
                    }
                    else{
                        app.speakOut(app.getString(R.string.req_symptoms1), null);
                    }
                    break;
                case 1:
                    if(app.langName.equals("_te")) {
                        app.speakOut(app.getString(R.string.req_symptoms2), app.getString(R.string.req_symptoms2_te));
                    }
                    else{
                        app.speakOut(app.getString(R.string.req_symptoms2), null);
                    }
                    break;
            }
            // Set appropriate grammar
            current_grammar =  app.SYMPTOM_RESPONSE;
            symptom_request_count++;

        } else if (current_symptom != null) {
            // TODO merge to accept natural language as response
            System.out.println("ASKING QUESTIONS ********************** ==> LANG NAME : " + app.langName + " --- " + current_symptom);
            for (Map.Entry<String, Map<Integer, String>> e : ((HealthDomain) domain).LCONCEPT_SYMPTOM_MAP.entrySet()) {
                System.out.println(e.getKey() + " : " + e.getValue());
            }
            if(app.langName.equals("_te")) {
                app.speakOut(app.getString(R.string.ack_symptom, ((HealthDomain) domain).LCONCEPT_SYMPTOM_MAP.get(app.langName).get(current_symptom)), app.getString(R.string.ack_symptom_te, ((HealthDomain) domain).LCONCEPT_SYMPTOM_MAP.get(app.langName).get(current_symptom)));
            }
            else{
                app.speakOut(app.getString(R.string.ack_symptom, ((HealthDomain) domain).LCONCEPT_SYMPTOM_MAP.get(app.langName).get(current_symptom)), null);
            }
            // Set appropriate grammar
            current_grammar =  app.BINARY_RESPONSE;
            expect_binary = true;
        }
    }
}
