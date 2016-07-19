package iiit.speech.dialog;

import iiit.speech.itra.VaidyaActivity;
import iiit.speech.nlu.NLU;

/**
 * Created by danda on 7/15/16.
 */
public class FirstAidEnquiry extends DialogState{
    private VaidyaActivity app;
    private NLU nlu;

    public FirstAidEnquiry(VaidyaActivity a,NLU nlu1){
        entered = false;
        app = a;
        nlu = nlu1;
        this.setName("firstaid_enquiry");
    }
    @Override
    public void onEntry() {
        entered = true;
        current_grammar = app.FIRSTAID_QUERY_RESPONSE;

    }

    @Override
    public void onRecognize(String hyp) {

    }

    @Override
    public void onExit() {

    }
}
