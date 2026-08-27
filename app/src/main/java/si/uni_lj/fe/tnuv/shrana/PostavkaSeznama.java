package si.uni_lj.fe.tnuv.shrana;

import java.io.Serializable;

// ena postavka na nakupovalnem seznamu: besedilo + ali je že kupljena (odkljukana)
public class PostavkaSeznama implements Serializable {
    String besedilo;
    boolean kupljeno;

    public PostavkaSeznama(String besedilo) {
        this.besedilo = besedilo;
        this.kupljeno = false;
    }

    // prazen konstruktor za Gson
    public PostavkaSeznama() {
    }
}