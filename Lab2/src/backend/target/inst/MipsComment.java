package backend.target.inst;

import backend.ir.inst.Instruction;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

// text中的注释
public class MipsComment extends MipsText{
    private String comment;

    public MipsComment(String comment) {
        this.comment = comment;
    }

    public MipsComment(Instruction instruction){
        String printText;

        try(ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream, true)
        ){
            instruction.dump(printStream);
            printText = outputStream.toString();
        }catch(IOException e){
            throw new RuntimeException(e);
        }
        this.comment = printText.substring(0, printText.length() - 1);
    }

    @Override
    public String toString(){
        return "# " + comment;
    }
}
