package iiit.speech.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created by brij on 9/9/15.
 */
public class HealthDomain extends DomainDesc{

    private Map<String, Boolean> symptoms;
    private String disease;
    private Set<String> removed_symptoms;

    public enum SymptomStatus {
        REMOVED, NOT_PRESENT, ACKNOWLEDGED, NOT_ACKNOWLEDGED
    }

    public HealthDomain() {
        this.setName("health");
        symptoms = new TreeMap<>(); // To maintain order of symptoms in which they are being told
        removed_symptoms = new HashSet<>();
    }

    public void setSymptoms(Map<String, Boolean> s) {
        symptoms = s;
    }
    public Map<String, Boolean> getSymptoms() {
        return symptoms;
    }

    public void setDisease(String d) {
        disease = d;
    }
    public String getDisease() {
        return disease;
    }

    public void addSymptoms(String sym) {
        symptoms.put(sym, Boolean.FALSE);
    }
    public void markSymptomAcknowledged(String sym) {
        symptoms.put(sym, Boolean.TRUE);
    }
    public void markRemovedSymptomUnAcknowledged(String sym) {
        removed_symptoms.remove(sym);
        symptoms.put(sym, Boolean.FALSE);
    }
    public String getSingleUnacknowledged() {
        String k = null;
        for (Map.Entry<String, Boolean> e : symptoms.entrySet()) {
            if (e.getValue() == Boolean.FALSE) {
                k = e.getKey(); break;
            }
        }
        return k;
    }
    public SymptomStatus getSymptomStatus(String sym) {
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
        for (Map.Entry<String, Boolean> e : symptoms.entrySet()) {
            System.out.println(e.getKey() + " : " + e.getValue());
        }
        System.out.println("Removed symptoms ==== ");
        for (String e : removed_symptoms) {
            System.out.println(e);
        }
    }
    public void removeSymptom(String sym) {
        symptoms.remove(sym);
        removed_symptoms.add(sym);
    }
    public boolean isRemoved(String sym) {
        return removed_symptoms.contains(sym);
    }
}
