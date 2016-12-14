package iiit.speech.domain;

/**
 * Created by danda on 7/20/16.
 */
public class EmergencyQuestion {
    private String ques;
    private String ans;

    public EmergencyQuestion(String ques) {
        this.ques = ques;
        this.ans = ans;
    }

    public String getQues() {
        return ques;
    }

    public void setQues(String ques) {
        this.ques = ques;
    }

    public String getAns() {
        return ans;
    }

    public void setAns(String ans) {
        this.ans = ans;
    }
}
