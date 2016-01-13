package iiit.speech.dialog;

import iiit.speech.itra.VaidyaActivity;
import iiit.speech.nlu.NLU;

/**
 * Created by brij on 16/9/15.
 */
public class AcknowledgeSymptomState extends DialogState {

    private VaidyaActivity app;
    private NLU nlu;

    public AcknowledgeSymptomState(VaidyaActivity a, NLU nlu1) {
        entered = false;
        app = a;
        nlu = nlu1;
        this.setName("ack_symptoms");
    }

    @Override
    public void onEntry() {

    }

    @Override
    public void onRecognize(String hyp) {

    }

    @Override
    public void onExit() {

    }
}
