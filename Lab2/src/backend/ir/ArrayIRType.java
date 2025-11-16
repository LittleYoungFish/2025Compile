package backend.ir;

import java.util.ArrayList;
import java.util.List;

public class ArrayIRType extends IRType{
    private final BasicIRType elementType;
    private final List<Integer> arrayDims = new ArrayList<>();

    public ArrayIRType(BasicIRType elementType, List<Integer> arrayDimensions) {
        this.elementType = elementType;
        this.arrayDims.addAll(arrayDimensions);
    }

    public int getTotalSize(){
        int totalSize = 1;
        for(Integer dimension : arrayDims){
            totalSize *= dimension;
        }
        return totalSize;
    }

    @Override
    public ArrayIRType ptr(int num){
        super.ptr(num);
        return this;
    }

    public BasicIRType getElementType(){
        return elementType;
    }

    @Override
    public List<Integer> getArrayDims(){
        return arrayDims;
    }

    public String initValsToString(List<Integer> initVals){
        return initValsToString(this.arrayDims, initVals);
    }

    private boolean isAllZero(List<Integer> vals){
        for(Integer val : vals){
            if(val != 0){
                return false;
            }
        }
        return true;
    }

    public String initValsToString(List<Integer> dims, List<Integer> initVals){
        StringBuilder sb = new StringBuilder();
        for (Integer dim : dims){
            sb.append("[").append(dim).append(" x ");
        }
        sb.append(elementType);
        sb.append("]".repeat(dims.size())).append(" ");
        if(isAllZero(initVals)){
            sb.append("zeroinitializer");
            return sb.toString();
        }

        if (dims.isEmpty()){
            sb.append(initVals.isEmpty() ? 0 : initVals.get(0));
            return sb.toString();
        } else if (dims.size() == 1) {
            sb.append("[");
            for (int i = 0; i< dims.get(0);i++){
                if(i != 0){
                    sb.append(", ");
                }
                //sb.append(elementType).append(" ").append(initVals.get(i));
                int value = (i < initVals.size()) ? initVals.get(i) : 0;
                sb.append(elementType).append(" ").append(value);
            }
            sb.append("]");
            return sb.toString();
        }else {
            sb.append("[");
            int stride = 1;
            for(int i = 1; i < dims.size(); i++){
                stride *= dims.get(i);
            }

            for (int i = 0; i< dims.get(0); i++){
                if(i != 0){
                    sb.append(", ");
                }
                List<Integer> nextDim = dims.subList(1, dims.size());
                List<Integer> nextInitVals = initVals.subList(stride * i, stride * (i + 1));
                sb.append(initValsToString(nextDim, nextInitVals));
            }
            sb.append("]");
            return sb.toString();
        }
    }

    private String typeToString(){
        StringBuilder sb = new StringBuilder();
        for (Integer dim : arrayDims){
            sb.append(String.format("[%d x ", dim));
        }
        sb.append(elementType.toString());

        sb.append("]".repeat(arrayDims.size()));

        sb.append("*".repeat(getPtrNum()));
        return sb.toString();
    }

    @Override
    public IRTypeEnum getType(){
        return IRTypeEnum.ARRAY;
    }

    @Override
    public String toString(){
        return typeToString();
    }

    @Override
    public ArrayIRType clone(){
        ArrayIRType obj = new ArrayIRType(this.elementType, this.arrayDims);
        obj.ptr(obj.getPtrNum());
        return obj;
    }

    @Override
    public boolean equals(Object o){
        if(o instanceof ArrayIRType other){
            return this.elementType.equals(other.elementType) && this.arrayDims.equals(other.arrayDims) && this.getPtrNum() == other.getPtrNum();
        }else {
            return false;
        }
    }
}
