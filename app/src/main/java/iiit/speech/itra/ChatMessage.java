package iiit.speech.itra;

import android.net.Uri;

/**
 * Created by danda on 7/16/16.
 */
public class ChatMessage {
    public boolean left;
    public String message;
    public Uri imgUrl;

    public ChatMessage(boolean left, String message, Uri imgUrl) {
        super();
        this.left = left;
        this.message = message;
        this.imgUrl = imgUrl;
    }
}
