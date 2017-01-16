package iiit.speech.nlu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import iiit.speech.domain.DomainDesc;
import iiit.speech.domain.HealthDomain;
import iiit.speech.domain.NoDomain;
import iiit.speech.itra.VaidyaActivity;

/**
 * Created by brij on 4/9/15.
 */
public class NLU {

    private static final String[] health_domain = {"start", "health", "not feeling well", "disease", "symptom", "diagnosis"};
    private VaidyaActivity app;
    private String[] symptom_list;
    private String[] symptom_tokens;
    private String[] stopword_tokens;
    private String[] disease_list;
    private List stopword_list;

    public NLU(VaidyaActivity a) throws IOException{
        System.out.println("Stating NLU...");
        app = a;
        File symp_file = new File(app.assetDir, "symptom_edit_dist" + a.langName+".txt");
        symptom_list = readLines(symp_file);
        System.out.println("Read symptoms file...");
        File stopwords_file = new File(app.assetDir, "stopwords"+a.langName+".txt");
        stopword_tokens = readLines(stopwords_file);
        System.out.println("Read stopwords file...");
        stopword_list = Arrays.asList(stopword_tokens);

        symptom_tokens = concatenate(listToTokens(symptom_list), health_domain);

        File disease_name_file = new File(app.assetDir, "disease_edit_dist.txt");
        disease_list = readLines(disease_name_file);
    }

    public DomainDesc getDomain(String hyp) throws IOException{

        DomainDesc domain;

        if (stringContainsItemFromList(hyp, symptom_tokens)) {
            domain = new HealthDomain(app);
            List<Integer> symps = findSymptomsInHyp(hyp);
            for (Integer sym : symps) {
                ((HealthDomain)domain).addSymptoms(sym);
            }
        }
        else {
            domain = new NoDomain();
        }

        return domain;
    }
    public String resolveEmergencyHyp(String hyp){
        if(hyp.equalsIgnoreCase("stomach pain") || hyp.equalsIgnoreCase("abdominal pain")){
            return "abdominal_pain";
        }
        if(hyp.equalsIgnoreCase("burn")){
            return "burn";
        }
        if (hyp.equalsIgnoreCase("cut") || hyp.equalsIgnoreCase("wound") || hyp.equalsIgnoreCase("bruise")) {
            return "cut";
        }
        else{
            return "cut";
        }
    }
    public boolean resolveBinaryHyp(String hyp) {
        boolean yes = false;
        if (hyp.contains("yes") || hyp.contains("yeah") || hyp.contains("yep") || hyp.contains("haan") || hyp.contains("avnu") || hyp.contains("undi")) {
            yes = true;
        }
        return yes;
    }


    public boolean checkNegative(String hyp) {
        boolean flag = false;
        if (hyp.equalsIgnoreCase("no") || hyp.equalsIgnoreCase("nope")) {
            flag = true;
        }
        return flag;
    }

    public String resolveGreetStateResponse(String hyp){

        if(hyp.contains("first aid")){
            return "first aid";
        }
        else if(hyp.contains("diagnose") || hyp.contains("diagnose disease")|| hyp.contains("jhaanch")|| hyp.contains("వైద్యం")){
            return "ask symptoms";
        }
        else if((stringContainsItemFromList(hyp, disease_list)) || hyp.contains("enquiry")){
            return "disease enquiry";
        }
        return "ask symptoms";
    }
    public String resolveSymptomQueryHyp(String hyp){

        if(hyp.contains("yes") || hyp.contains("yeah") || hyp.contains("yep") || hyp.contains("haan") || hyp.contains("avnu") || hyp.contains("undi")){
            return "yes";
        }
        else if(hyp.contains("no") || hyp.contains("nope") || hyp.contains("nothing") || hyp.contains("nahi") || hyp.contains("ledu") || hyp.contains("kadu")){
            return "no";
        }
        return "query";
    }
    public boolean stringContainsItemFromList(String inputString, String[] items)
    {
        for(int i =0; i < items.length; i++)
        {
            if(inputString.contains(items[i]))
            {
                return true;
            }
        }
        return false;
    }



    public List<Integer> findSymptomsInHyp(String inputString)
    {
        List<Integer> syms = new ArrayList<>();

        for(int i =0; i < symptom_list.length; i++)
        {
            if(inputString.contains(symptom_list[i]))
            {
                syms.add(((HealthDomain)app.domain).SYMPTOM_CID.get(symptom_list[i]));
            }
        }
        return syms;
    }

    public String findDiseaseInHyp(String inputString)
    {
        List<String> disease = new ArrayList<>();
        for(int i =0; i < disease_list.length; i++)
        {
            if(inputString.contains(disease_list[i]))
            {
                return disease_list[i];
            }
        }
        return null;
    }

    public String[] readLines(File filename) throws IOException {
        FileReader fileReader = new FileReader(filename);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        List<String> lines = new ArrayList<>();
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            lines.add(line);
        }
        bufferedReader.close();
        return lines.toArray(new String[lines.size()]);
    }

    public String[] listToTokens(String[] list) {
        Set<String> tokens = new HashSet<>();
        int nlist = list.length;
        for (int i = 0; i < nlist; i++) {
            String[] sp = list[i].split(" ");
            int nsp = sp.length;
            for (int j = 0; j < nsp; j++) {
                if (!stopword_list.contains(sp[j])) {
                    tokens.add(sp[j]);
                }
            }
        }
        return tokens.toArray(new String[tokens.size()]);
    }

    public <T> T[] concatenate (T[] a, T[] b) {
        int aLen = a.length;
        int bLen = b.length;

        @SuppressWarnings("unchecked")
        T[] c = (T[]) Array.newInstance(a.getClass().getComponentType(), aLen + bLen);
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);

        return c;
    }

    public boolean isValidString(String str) {
        System.out.println("SOME TEXT =========== > " + str);
        boolean flag = false;
        if (str != null) {
            str = str.replaceAll(" ", "");
            if (!str.equals("")) {
                flag = true;
            }
        }
        return flag;
    }
}
