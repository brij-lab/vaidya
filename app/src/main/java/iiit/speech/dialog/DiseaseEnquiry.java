package iiit.speech.dialog;

import iiit.speech.domain.HealthDomain;
import iiit.speech.itra.VaidyaActivity;
import iiit.speech.nlu.NLU;

/**
 * Created by danda on 7/13/16.
 */
public class DiseaseEnquiry extends DialogState {

    private VaidyaActivity app;
    private NLU nlu;

    public DiseaseEnquiry(VaidyaActivity a,NLU nlu1){
        entered = false;
        app = a;
        nlu = nlu1;
        this.setName("disease_enquiry");
    }

    @Override
    public void onEntry() {
        entered = true;
        current_grammar = app.DISEASE_QUERY_RESPONSE;
        app.speakOut("Enquire disease name or Diagnose disease name?", null);

    }

    @Override
    public void onRecognize(String hyp) {
        String detail_of_disease = nlu.findDiseaseInHyp(hyp);
        System.out.println("Hypothesis ======================>" + hyp);
        System.out.println(" detail_of_disease ======================>" +  detail_of_disease);
        if(nlu.resolveGreetStateResponse(hyp).contains("ask symptoms")){
            for (Integer sym : ((HealthDomain) domain).getSymptomsForDisease(detail_of_disease)) {
                //((HealthDomain) domain).addSymptoms(sym);
            }
            conclude = true;
            next_state = "ask_symptoms";
        }
        else if (nlu.resolveGreetStateResponse(hyp).contains("disease enquiry")) {
            ((HealthDomain) domain).setDisease(detail_of_disease);
            System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%5");
            conclude = true;
            next_state = "disease_details";
        }

    }

    @Override
    public void onExit() {

    }
}
