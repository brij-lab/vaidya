package iiit.speech.dialog;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by brij on 4/9/15.
 */
public class DialogSlots {
    private List<String> symptoms;

    public DialogSlots() {
        symptoms = new ArrayList<>();
    }
    public List<String> getSymptoms() {
        return symptoms;
    }
    public void setSymptoms(List<String> s) {
        symptoms = s;
    }
    public void addSymptom(String s) {
        symptoms.add(s);
    }
}
