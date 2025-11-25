package backend.target;

import backend.target.value.Label;

import java.util.ArrayList;
import java.util.List;

public class Data {
    private Label label;
    private String type;
    private List<Object> values = new ArrayList<>();

    public Data(String labelName, String type, List<Object> values) {
        this.label = new Label(labelName);
        this.type = type;
        this.values.addAll(values);
    }

    public Label getLabel() {
        return label;
    }

    @Override
    public String toString() {
        String sb = label + ": ." + type + " " +
                String.join(", ", values.stream().map(Object::toString).toList());
        return sb;
    }
}
