package iiit.speech.domain;

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

import iiit.speech.itra.VaidyaActivity;

/**
 * Created by brij on 9/9/15.
 */
public class HealthDomain extends DomainDesc{

    public String symptom_explain;
    private Map<Integer, Boolean> symptoms;
    private String disease;
    private Set<Integer> removed_symptoms;

    public Map<Integer, Integer[]> SYMPTOM_VECTOR;
    public Map<String, Integer[]> DISEASE_VECTOR;

    public int SYMPTOM_VEC_DIM = 0;
    public int DISEASE_VEC_DIM = 0;

    //public Map<Integer, String> SYMPTOM_IDX;
    public Map<String, Integer> SYMPTOM_CID;
    public Map<Integer, String> DISEASE_IDX;

    public Map<String, Map<Integer, String>> LCONCEPT_SYMPTOM_MAP;

    public enum SymptomStatus {
        REMOVED, NOT_PRESENT, ACKNOWLEDGED, NOT_ACKNOWLEDGED
    }

    public HealthDomain(VaidyaActivity app) {
        this.setName("health");
        symptoms = new TreeMap<>(); // To maintain order of symptoms in which they are being told
        removed_symptoms = new HashSet<>();

        SYMPTOM_VECTOR = new TreeMap<>();
        DISEASE_VECTOR = new TreeMap<>();
        //SYMPTOM_IDX = new HashMap<>();
        DISEASE_IDX = new HashMap<>();
        SYMPTOM_CID = new HashMap<>();
        LCONCEPT_SYMPTOM_MAP = new HashMap<>();

        System.out.println("Reading symptom concept ids...");
        File symp_cid_file = new File(app.assetDir, "sym_conceptid.txt");
        //int symp_idx = 0;
        try(BufferedReader br = new BufferedReader(new FileReader(symp_cid_file))) {
            for(String line; (line = br.readLine()) != null; ) {
                // process the line.
                System.out.println(line);
                String[] sp = line.split(",");
                Integer cid = Integer.parseInt(sp[1]);
                SYMPTOM_CID.put(sp[0], cid);
                if(!LCONCEPT_SYMPTOM_MAP.containsKey("_" + sp[2])) {
                    LCONCEPT_SYMPTOM_MAP.put("_" + sp[2], new HashMap<Integer, String>());
                    System.out.println("this is the language =============================>" + sp[2]);
                }
                LCONCEPT_SYMPTOM_MAP.get("_" + sp[2]).put(cid, sp[0]);
                System.out.println("this is the conceptid and symptom =============================>" + cid + " ;" + sp[0]);
            }
        } catch (IOException e) {
            System.out.println("Couldn't read symptom cid file");
        }

        System.out.println("Reading symptom vectors...");
        File symp_vectors_file = new File(app.assetDir, "symp_vectors.txt");
        //int symp_idx = 0;
        try(BufferedReader br = new BufferedReader(new FileReader(symp_vectors_file))) {
            for(String line; (line = br.readLine()) != null; ) {
                // process the line.
                String[] sp = line.split("\t");
                //SYMPTOM_IDX.put(symp_idx, sp[0]);
                String[] sp1 = sp[1].split(" ");
                String[] symp_vec = Arrays.copyOfRange(sp1, 0, sp1.length);
                if (SYMPTOM_VEC_DIM == 0) {
                    SYMPTOM_VEC_DIM = symp_vec.length;
                }
                SYMPTOM_VECTOR.put(Integer.parseInt(sp[0]), stringArrToIntArr(symp_vec));
                //symp_idx++;
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
    }

    private Integer[] stringArrToIntArr(String[] strArr) {
        Integer[] intarr = new Integer[strArr.length];
        int nelem = strArr.length;
        for (int i = 0; i < nelem; i++) {
            intarr[i] = Integer.parseInt(strArr[i]);
        }
        return intarr;
    }

    public void setSymptoms(Map<Integer, Boolean> s) {
        symptoms = s;
    }
    public Map<Integer, Boolean> getSymptoms() {
        return symptoms;
    }

    public void setDisease(String d) {
        disease = d;
    }
    public String getDisease() {
        return disease;
    }
    public void addSymptoms(Integer sym) {
        symptoms.put(sym, Boolean.FALSE);
    }
    public void markSymptomAcknowledged(Integer sym) {
        symptoms.put(sym, Boolean.TRUE);
    }
    public void markRemovedSymptomUnAcknowledged(Integer sym) {
        removed_symptoms.remove(sym);
        symptoms.put(sym, Boolean.FALSE);
    }
    public Integer getSingleUnacknowledged() {
        Integer k = null;
        for (Map.Entry<Integer, Boolean> e : symptoms.entrySet()) {
            if (e.getValue() == Boolean.FALSE) {
                k = e.getKey(); break;
            }
        }
        return k;
    }
    public SymptomStatus getSymptomStatus(Integer sym) {
        if (isRemoved(sym)) {
            return SymptomStatus.REMOVED;
        } else {
            Boolean st = symptoms.get(sym);
            if (st == null) {
                return SymptomStatus.NOT_PRESENT;
            } else if (st) {
                return SymptomStatus.ACKNOWLEDGED;
            } else {
                return SymptomStatus.NOT_ACKNOWLEDGED;
            }
        }
    }
    public void displaySymptomList() {
        System.out.println("Symptoms list ==== ");
        for (Map.Entry<Integer, Boolean> e : symptoms.entrySet()) {
            System.out.println(e.getKey() + " : " + e.getValue());
        }
        System.out.println("Removed symptoms ==== ");
        for (Integer e : removed_symptoms) {
            System.out.println(e);
        }
    }
    public void removeSymptom(Integer sym) {
        symptoms.remove(sym);
        removed_symptoms.add(sym);
    }
    public boolean isRemoved(Integer sym) {
        return removed_symptoms.contains(sym);
    }

    public List<Integer> getSymptomsForDisease(String dis) {
        List<Integer> symlist = new ArrayList<>();
        String dis_ = dis.replaceAll(" ", "_");
        Integer [] dis_vec = DISEASE_VECTOR.get(dis_);
        System.out.println("Vector for disease====================>" + dis_);
        //printIntArray(dis_vec);
        for (Integer idx: getOneIndices(dis_vec)) {
            symlist.add(idx);
        }
        return symlist;
    }

    private List<Integer> getOneIndices(Integer[] a) {
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < a.length; i++) {
            if (a[i] == 1) idx.add(i);
        }
        return idx;
    }
}
