package iiit.speech.domain;

/**
 * Created by brij on 5/9/15.
 */
public class DomainDesc {
    private String name;
    private String welcome_message;

    public String getName() {
        return name;
    }
    public void setName(String n) {
        this.name = n;
    }
    public String getWelcome_message() {
        return welcome_message;
    }
    public void setWelcome_message(String w) {
        this.welcome_message = w;
    }
}
