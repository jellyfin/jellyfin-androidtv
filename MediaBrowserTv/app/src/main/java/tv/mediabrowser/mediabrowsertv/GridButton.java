package tv.mediabrowser.mediabrowsertv;

/**
 * Created by Eric on 12/23/2014.
 */
public class GridButton {
    private int id;
    private String text;

    public GridButton(int id, String text) {
        this.id = id;
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return text;
    }
}
