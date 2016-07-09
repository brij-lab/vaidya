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
import iiit.speech.itra.VaidyaActivity;
import iiit.speech.nlu.NLU;

/**
 * Created by brij on 15/9/15.
 */
public class DiagnosisState extends DialogState {

    private VaidyaActivity app;
    private NLU nlu;

    private Map<String, Integer[]> SYMPTOM_VECTOR;
    private Map<String, Integer[]> DISEASE_VECTOR;

    int SYMPTOM_VEC_DIM = 0;
    int DISEASE_VEC_DIM = 0;

    private Map<Integer, String> SYMPTOM_IDX;
    private Map<Integer, String> DISEASE_IDX;

    private Map<String, Set<String>> possible_symptoms;
    private Set<String> possible_diseases;
    private String symptom_toask;
    private List<String> already_asked_symptoms;

    private boolean expect_binary = false;

    public DiagnosisState(VaidyaActivity a, NLU nlu1) {
        entered = false;
        app = a;
        nlu = nlu1;
        this.setName("diagnosis");

        SYMPTOM_VECTOR = new TreeMap<>();
        DISEASE_VECTOR = new TreeMap<>();
        SYMPTOM_IDX = new HashMap<>();
        DISEASE_IDX = new HashMap<>();

        System.out.println("Reading symptom vectors...");
        File symp_vectors_file = new File(app.assetDir, "symp_vectors.txt");
        int symp_idx = 0;
        try(BufferedReader br = new BufferedReader(new FileReader(symp_vectors_file))) {
            for(String line; (line = br.readLine()) != null; ) {
                // process the line.
                String[] sp = line.split(" ");
                SYMPTOM_IDX.put(symp_idx, sp[0]);
                String[] symp_vec = Arrays.copyOfRange(sp, 1, sp.length);
                if (SYMPTOM_VEC_DIM == 0) {
                    SYMPTOM_VEC_DIM = symp_vec.length;
                }
                SYMPTOM_VECTOR.put(sp[0], stringArrToIntArr(symp_vec));
                symp_idx++;
            }
        } catch (IOException e) {
            System.out.println("Couldn't read symptom vector file");
        }

        System.out.println("Reading disease vectors...");
        File disease_vectors_file = new File(app.assetDir, "disease_vecs.txt");
        int disease_idx = 0;
        try(BufferedReader br = new BufferedReader(new FileReader(disease_vectors_file))) {
            for(String line; (line = br.readLine()) != null; ) {
                // process the line.
                String[] sp = line.split(" ");
                DISEASE_IDX.put(disease_idx, sp[0]);
                String[] disease_vec = Arrays.copyOfRange(sp, 1, sp.length);
                if (DISEASE_VEC_DIM == 0) {
                    DISEASE_VEC_DIM = disease_vec.length;
                }
                DISEASE_VECTOR.put(sp[0], stringArrToIntArr(disease_vec));
                disease_idx++;
            }
        } catch (IOException e) {
            System.out.println("Couldn't read disease vector file");
        }
        already_asked_symptoms = new ArrayList<>();
    }

    private Integer[] stringArrToIntArr(String[] strArr) {
        Integer[] intarr = new Integer[strArr.length];
        int nelem = strArr.length;
        for (int i = 0; i < nelem; i++) {
            intarr[i] = Integer.parseInt(strArr[i]);
        }
        return intarr;
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
        app.speakOut("I have initial symptoms to start diagnosis.");

        // Set appropriate grammar
        current_grammar =  app.BINARY_RESPONSE;
        entered = true;
        next_state = "diagnosis";
        //expect_binary = false;
        Integer[] final_symp_vec = new Integer[SYMPTOM_VEC_DIM];
        Arrays.fill(final_symp_vec, 1);
        Map<String, Boolean> ack_symptoms = ((HealthDomain)domain).getSymptoms();
        for (Map.Entry<String, Boolean> e: ack_symptoms.entrySet()) {
            final_symp_vec = intersect(final_symp_vec, SYMPTOM_VECTOR.get(e.getKey().replaceAll(" ", "_")));
        }
        possible_diseases = new HashSet<>();
        for (Integer idx: getOneIndices(final_symp_vec)) {
            possible_diseases.add(DISEASE_IDX.get(idx));
        }
        app.speakOut("There are " + possible_diseases.size() + " diseases found matching your symptoms");
        // Speak out if there are less than 5 diseases
        if (possible_diseases.size() < 5) {
            for (String dis: possible_diseases) {
                app.speakOut(dis.replaceAll("_", " "));
            }
        }
        if (possible_diseases.size() > 1) {
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

        if (expect_binary) {
            if (nlu.resolveBinaryHyp(hyp)) {
                possible_diseases = possible_symptoms.get(symptom_toask);
            } else {
                possible_diseases.removeAll(possible_symptoms.get(symptom_toask));
            }
            already_asked_symptoms.add(symptom_toask);
        }

        app.appendColoredText(app.result_text, "Disease count = " + possible_diseases.size(), Color.GREEN);
        if (possible_diseases.size() > 1) {
            getPossibleSymptoms();
        } else {
            for (String d: possible_diseases) {
                d = d.replaceAll("_", " ");
                app.speakOut("your most probable diagnosis is " + d);
                app.appendColoredText(app.result_text, "Diagnosis = " + d, Color.WHITE);
                ((HealthDomain)domain).setDisease(d);
                conclude = true;
                break;
            }
        }
    }

    private void getPossibleSymptoms() {
        Integer[] dis_vec;
        String poss_sym;
        possible_symptoms = new HashMap<>();
        Set<String> dis_list;
        for (String dis: possible_diseases) {
            dis_vec = DISEASE_VECTOR.get(dis);
            for (Integer idx: getOneIndices(dis_vec)) {
                poss_sym = SYMPTOM_IDX.get(idx);
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
        for (Map.Entry<String, Set<String>> e: possible_symptoms.entrySet()) {
            if (!already_asked_symptoms.contains(e.getKey())) {
                System.out.println("Symptom ==> " + e.getKey() + "  ; disease count ==>" + e.getValue().size());
                if (Math.abs(Dc - e.getValue().size()) < min_dif) {
                    symptom_toask = e.getKey();
                    min_dif = Math.abs(Dc - e.getValue().size()) ;
                }
            }
        }
        app.speakOut("Do you have " + symptom_toask.replaceAll("_", " "));
        current_grammar = app.BINARY_RESPONSE;
        expect_binary = true;
    }

    @Override
    public void onExit() {
        next_state = "disease_details";
    }
}
