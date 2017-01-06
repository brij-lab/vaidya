package iiit.speech.dialog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import iiit.speech.domain.EmergencyQuestion;
import iiit.speech.itra.VaidyaActivity;
import iiit.speech.nlu.NLU;

/**
 * Created by danda on 7/15/16.
 */
public class FirstAidEnquiry extends DialogState{
    private VaidyaActivity app;
    private NLU nlu;
    public boolean expect_binary = false;
    public List<EmergencyQuestion> firstaid_questions;
    public int quesLen = 0;

    public FirstAidEnquiry(VaidyaActivity a,NLU nlu1){
        entered = false;
        app = a;
        nlu = nlu1;
        this.setName("first_aid");
    }
    @Override
    public void onEntry() {
        entered = true;
        current_grammar = app.FIRSTAID_QUERY_RESPONSE;
        expect_binary = false;
        app.speakOut("Please state your emergency", null);
    }

    @Override
    public void onRecognize(String hyp) {
        if (!expect_binary) {
            String resolveFirstAid = nlu.resolveEmergencyHyp(hyp);
            firstaid_questions = new ArrayList<>();

            System.out.println("Reading firstaid emergency...");
            File firstaid_file = new File(app.assetDir, "firstaid_emergency_en.txt");
            try(BufferedReader br = new BufferedReader(new FileReader(firstaid_file))) {
                for(String line; (line = br.readLine()) != null; ) {
                    // process the line.
                    String[] sp = line.split(" ## ");
                    if (sp[0].equalsIgnoreCase(resolveFirstAid)) {
                        firstaid_questions.add(new EmergencyQuestion(sp[1]));
                    }
                }
            } catch (IOException e) {
                System.out.println("Couldn't read disease definition file");
            }

            System.out.println("Reading firstaid treatment...");
            firstaid_file = new File(app.assetDir, "firstaid_treatment_en.txt");
            try(BufferedReader br = new BufferedReader(new FileReader(firstaid_file))) {
                for(String line; (line = br.readLine()) != null; ) {
                    // process the line.
                    String[] sp = line.split(" ## ");
                    if (sp[0].equalsIgnoreCase(resolveFirstAid)) {
                        firstaid_questions.add(new EmergencyQuestion(sp[1]));
                    }
                }
            } catch (IOException e) {
                System.out.println("Couldn't read disease definition file");
            }

            quesLen = firstaid_questions.size();
        }
        else {
            if (nlu.resolveBinaryHyp(hyp) && quesLen != firstaid_questions.size()) {
                //int idx = firstaid_questions.size() - quesLen;
              //  app.speakOut(firstaid_questions.get(idx).getAns());
                app.speakOut("Please go to the doctor immediately", null);
                conclude = true;
                //next_state = "greet";
            }
            else {
                quesLen--;
            }
        }

        if (!conclude) {
            askQuestions();
        }

    }

    private void askQuestions() {
        int idx = firstaid_questions.size() - quesLen;
        if (idx < firstaid_questions.size()) {
            app.speakOut(firstaid_questions.get(idx).getQues(), null);
            expect_binary = true;
            current_grammar = app.BINARY_RESPONSE;
        } else {
            // Ran out of questions
            //app.speakOut("Sorry. This is beyond my scope.");
            conclude = true;
            //next_state = "greet";
        }

        //quesLen--;
    }

    @Override
    public void onExit() {
        quesLen = 0;
        expect_binary = false;
        next_state = "reset";
    }
}
