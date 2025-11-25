package backend.target.value;

public class Label extends TargetValue{
    private String labelName;

    public Label(String labelName){
        this.labelName = labelName;
    }

    public String getLabelName(){
        return labelName;
    }

    public void setLabelName(String labelName){
        this.labelName = labelName;
    }

    @Override
    public String toString(){
        return labelName;
    }
}
