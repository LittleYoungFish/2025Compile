package backend.target.inst;

// mips 标签
public class MipsLabel extends MipsText{
    private String labelName;

    public MipsLabel(String labelName) {
        this.labelName = labelName;
    }

    @Override
    public String toString() {
        return labelName + ":";
    }
}
