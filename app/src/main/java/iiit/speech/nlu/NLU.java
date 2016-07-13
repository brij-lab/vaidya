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
    }

    public DomainDesc getDomain(String hyp) throws IOException{

        DomainDesc domain;

        if (stringContainsItemFromList(hyp, symptom_tokens)) {
            domain = new HealthDomain();
            List<String> symps = findSymptomsInHyp(hyp);
            for (String sym : symps) {
                ((HealthDomain)domain).addSymptoms(sym);
            }
        }
        else {
            domain = new NoDomain();
        }

        return domain;
    }

    public boolean resolveBinaryHyp(String hyp) {
        boolean yes = false;
        if (hyp.contains("yes") || hyp.contains("yeah") || hyp.contains("yep")) {
            yes = true;
        }
        return yes;
    }

    public String resolveSymptomQueryHyp(String hyp){

        if(hyp.contains("yes") || hyp.contains("yeah") || hyp.contains("yep")){
            return "yes";
        }
        else if(hyp.contains("no") || hyp.contains("nope") || hyp.contains("nothing")){
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

    public List<String> findSymptomsInHyp(String inputString)
    {
        List<String> syms = new ArrayList<>();
        for(int i =0; i < symptom_list.length; i++)
        {
            if(inputString.contains(symptom_list[i]))
            {
                syms.add(symptom_list[i]);
            }
        }
        return syms;
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
