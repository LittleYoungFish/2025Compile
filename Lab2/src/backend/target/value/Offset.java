package backend.target.value;

// 内存偏移量
public class Offset extends TargetValue{
    private Register base;
    private int offset;

    public Offset(Register base, int offset) {
        this.base = base;
        this.offset = offset;
    }

    public Register getBase() {
        return base;
    }

    public int getOffset() {
        return offset;
    }

    public void setBase(Register base) {
        this.base = base;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    @Override
    public String toString() {
        return offset + "(" + base + ")";
    }
}
