package iiit.speech.dialog;

import iiit.speech.domain.DomainDesc;

/**
 * Created by brij on 3/9/15.
 */
public abstract class DialogState {

    private String name;

    public boolean entered = false;
    public boolean conclude = false;
    public String current_grammar;
    public String next_state;
    public DomainDesc domain;

    public String getName() {
        return name;
    }
    public void setName(String name1){
        this.name = name1;
    }

    public abstract void onEntry();
    public abstract void onRecognize(String hyp);
    public abstract void onExit();

}
