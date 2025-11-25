package backend.target.value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Register extends TargetValue{
    private String registerName;

    public Register(String registerName){
        this.registerName = registerName;
    }

    public String getRegisterName(){
        return registerName;
    }

    public void setRegisterName(String registerName){
        this.registerName = registerName;
    }

    @Override
    public String toString(){
        return "$" + registerName;
    }

    public static Map<String, Register> REGS = new HashMap<String, Register>();
    private static Stack<Register> TEMP_REGS = new Stack<>();
    private static Stack<Register> TEMP_REGS_USING = new Stack<>();

    static {
        List<String> registerNames = List.of(
                "v0", "v1", "a0", "a1", "a2", "a3",
                "t0", "t1", "t2", "t3", "t4", "t5", "t6", "t7",
                "s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7",
                "sp", "fp", "ra"
        );

        List<String> tempRegisterNames = List.of("t5", "t6", "t7");

        for (String registerName : registerNames){
            Register reg = new Register(registerName);
            REGS.put(registerName, reg);
            if (tempRegisterNames.contains(registerName)){
                TEMP_REGS.add(reg);
            }
        }
    }

    public static Register allocTempRegister(){
        if (TEMP_REGS.isEmpty()){
            return null;
        }
        Register reg = TEMP_REGS.pop();
        TEMP_REGS_USING.push(reg);
        return reg;
    }

    public static void freeAllTempRegister(){
        while (!TEMP_REGS_USING.isEmpty()){
            TEMP_REGS.push(TEMP_REGS_USING.pop());
        }
    }
}
