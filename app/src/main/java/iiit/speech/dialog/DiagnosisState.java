package iiit.speech.dialog;

import android.graphics.Color;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import iiit.speech.domain.HealthDomain;
import iiit.speech.itra.R;
import iiit.speech.itra.VaidyaActivity;
import iiit.speech.nlu.NLU;

/**
 * Created by brij on 15/9/15.
 */
public class DiagnosisState extends DialogState {

    private VaidyaActivity app;
    private NLU nlu;
    public boolean sym_Expain_Flag = false;

    private Map<Integer, Set<String>> possible_symptoms;
    private Set<String> possible_diseases;
    private String symptom_toask;
    private List<String> already_asked_symptoms;

    private boolean expect_binary = false;

    public DiagnosisState(VaidyaActivity a, NLU nlu1) {
        entered = false;
        app = a;
        nlu = nlu1;
        this.setName("diagnosis");

        already_asked_symptoms = new ArrayList<>();
    }


    private Integer[] intersect(Integer[] a, Integer[] b) {
        Integer[] res = new Integer[a.length];
        for(int i = 0; i < a.length; i++) {
            res[i] = a[i] * b[i];
        }
        return res;
    }

    private List<Integer> getOneIndices(Integer[] a) {
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < a.length; i++) {
            if (a[i] == 1) idx.add(i);
        }
        return idx;
    }

    @Override
    public void onEntry() {
        System.out.println("+++++++++++++++++ Diagnosis state entered +++++++++++++++++++++");
        if(app.langName.equals("_te")) {
            app.speakOut(app.getString(R.string.initial_symps), app.getString(R.string.initial_symps_te));
        }
        else{
            app.speakOut(app.getString(R.string.initial_symps),null);
        }

        // Set appropriate grammar
        current_grammar =  app.BINARY_RESPONSE;
        entered = true;
        next_state = "diagnosis";
        //expect_binary = false;
        Integer[] final_symp_vec = new Integer[((HealthDomain)domain).SYMPTOM_VEC_DIM];
        Arrays.fill(final_symp_vec, 1);
        Map<Integer, Boolean> ack_symptoms = ((HealthDomain)domain).getSymptoms();
        for (Map.Entry<Integer, Boolean> e: ack_symptoms.entrySet()) {
            Integer [] symp_vector =  ((HealthDomain)domain).SYMPTOM_VECTOR.get(e.getKey());
            System.out.println("Vector for =========> " + e.getKey());
            printIntArray(symp_vector);
            System.out.println("Final vector : before =========> ");
            printIntArray(final_symp_vec);
            final_symp_vec = intersect(final_symp_vec, symp_vector);
            System.out.println("Final vector : after =========> ");
            printIntArray(final_symp_vec);
        }
        possible_diseases = new HashSet<>();
        for (Integer idx: getOneIndices(final_symp_vec)) {
            possible_diseases.add(((HealthDomain)domain).DISEASE_IDX.get(idx));
        }
        if(app.langName.equals("_te")) {
            app.speakOut(app.getString(R.string.disease_count, possible_diseases.size()), app.getString(R.string.disease_count_te, possible_diseases.size()));
        }
        else{
            app.speakOut(app.getString(R.string.disease_count, possible_diseases.size()), null);
        }
        // Speak out if there are less than 5 diseases
        if (possible_diseases.size() < 5) {
            for (String dis: possible_diseases) {
                app.speakOut(dis.replaceAll("_", " "), null);
            }
        }
        // If there are more than one possible disease then ask for more symptoms to resolve
        if (possible_diseases.size() > 1) {
            for (String dis: possible_diseases) {
                System.out.println(dis.replaceAll("_", " "));
            }
            getPossibleSymptoms();
        }
        if (possible_diseases.size() == 0) {
            conclude = true;
            next_state = "greet";
            current_grammar = app.GREET_RESPONSE;
        }
    }

    @Override
    public void onRecognize(String hyp) {
        sym_Expain_Flag = false;
        if (expect_binary) {
            System.out.println("Resolving symtom =========> " + symptom_toask);
            if (nlu.resolveSymptomQueryHyp(hyp).equalsIgnoreCase("yes")) {
                possible_diseases = possible_symptoms.get(((HealthDomain)domain).SYMPTOM_CID.get(symptom_toask));
                System.out.println("Remaining diseases ============>>>>");
                for (String rdis : possible_diseases) {
                    System.out.println(rdis);
                }
            } else if (nlu.resolveSymptomQueryHyp(hyp).equalsIgnoreCase("no")) {
                possible_diseases.removeAll(possible_symptoms.get(((HealthDomain)domain).SYMPTOM_CID.get(symptom_toask)));
                System.out.println("Remaining diseases ============>>>>");
                for (String rdis : possible_diseases) {
                    System.out.println(rdis);
                }
            } else {
                ((HealthDomain) domain).symptom_explain = symptom_toask;
                conclude = true;
                sym_Expain_Flag = true;
            }
            already_asked_symptoms.add(symptom_toask);
        }
        if(!sym_Expain_Flag) {
            app.sendChatMessage(true, "Disease count = " + possible_diseases.size(), null);
            if (possible_diseases.size() > 1) {
                getPossibleSymptoms();
            } else {
                for (String d : possible_diseases) {
                    d = d.replaceAll("_", " ");
                    if(app.langName.equals("_te")) {
                        app.speakOut(app.getString(R.string.probable_diagnosis, d), app.getString(R.string.probable_diagnosis_te, d));
                    }
                    else{

                        app.speakOut(app.getString(R.string.probable_diagnosis, d), null);
                    }
                    app.appendColoredText(app.result_text, "Diagnosis = " + d, Color.WHITE);
                    ((HealthDomain) domain).setDisease(d);
                    conclude = true;
                    break;
                }
            }
        }
    }

    private void getPossibleSymptoms() {
        Integer[] dis_vec;
        Integer poss_sym;
        possible_symptoms = new HashMap<>();
        Set<String> dis_list;
        for (String dis: possible_diseases) {
            dis_vec = ((HealthDomain)domain).DISEASE_VECTOR.get(dis);
            System.out.println("Vector for disease====================>" + dis);
            printIntArray(dis_vec);
            for (Integer idx: getOneIndices(dis_vec)) {
                poss_sym = idx;
                System.out.println("Poss Sym===================>" + poss_sym);
                if(poss_sym != null) {
                    dis_list = possible_symptoms.get(poss_sym);
                    if (dis_list == null) {
                        dis_list = new HashSet<>();
                        possible_symptoms.put(poss_sym, dis_list);
                    }
                    dis_list.add(dis);
                }
            }
        }

        int Dc = possible_diseases.size() / 2;
        symptom_toask = null;
        int min_dif = 9999;
        for (Map.Entry<Integer, Set<String>> e: possible_symptoms.entrySet()) {
            if (!already_asked_symptoms.contains(e.getKey())) {
                System.out.println("Symptom ==> " + e.getKey() + "  ; disease count ==>" + e.getValue().size());
                for (String cdis : e.getValue()) {
                    System.out.println(cdis);
                }

                if (Math.abs(Dc - e.getValue().size()) < min_dif) {
                    symptom_toask = ((HealthDomain)domain).LCONCEPT_SYMPTOM_MAP.get(app.langName).get(e.getKey());
                    min_dif = Math.abs(Dc - e.getValue().size()) ;
                }
            }
        }

        if(app.langName.equals("_te")) {
            app.speakOut(app.getString(R.string.shortlist_symp, symptom_toask.replaceAll("_", " ")), app.getString(R.string.shortlist_symp_te, symptom_toask.replaceAll("_", " ")));
        }
        else{
            app.speakOut(app.getString(R.string.shortlist_symp, symptom_toask.replaceAll("_", " ")), null);
        }
        current_grammar = app.SYMPTOM_QUERY_RESPONSE;
        expect_binary = true;
    }

    @Override
    public void onExit() {
        if(sym_Expain_Flag) {
            System.out.println("1");
            next_state = "symptom_details";
            //next_state = "disease_details";
        }
        else if (possible_diseases.size() == 1) {
            System.out.println("2");
            next_state = "disease_details";
        } else {
            System.out.println("3");
            next_state = "greet";
        }
        System.out.println("Next state : " + next_state);
    }

    void printIntArray(Integer [] arr) {
        int len = arr.length;
        for (int i = 0; i < len; i++) {
            System.out.print(String.valueOf(arr[i]) + " ");
        }
        System.out.println();
    }
}
