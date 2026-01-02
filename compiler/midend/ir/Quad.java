package midend.ir;

public class Quad {
    public final String op;
    public final String arg1;
    public final String arg2;
    public final String res;

    public Quad(String op, String arg1, String arg2, String res) {
        this.op = op;
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.res = res;
    }

    @Override
    public String toString() {
        return switch (op){
            case "decl","gdecl" -> op+" " + arg1 + ", " + (arg2==null ?"1":arg2) ;
            case "move", "ginit" -> op +" " +arg1+ ", " + res;
            case "storearr", "ginitarr"-> op +" " + arg1 + ", " + arg2 + ", "+res ;
            case "load"-> "load " + arg1 + ", "+res ;
            case "loadarr"->"loadarr "+arg1 + ", " + arg2 + ", "+res ;
            case "add", "sub", "mul", "div", "mod", "lt", "le", "gt", "ge", "eq","ne"-> op+ " " + arg1+ ", " +arg2+ ", " + res;
            case "neg" , "not" -> op+" " +arg1+ ", " + res;
            case "label" -> res+":";
            case "j" -> "j " + res;
            case "bez" -> "bez " + arg1 + ", "+ res;
            case "func" -> "func "+arg1;
            case "endfunc" -> "endfunc " + arg1;
            case "fparam" -> "fparam " + arg1;
            case "fparam_arr" -> "fparam_arr " + arg1;
            case "param_val" -> "param_val " + arg1;
            case "param_addr" -> "param_addr " + arg1;
            case "call" -> res==null ? ("call " + arg1 + ", "+arg2) : ("call " + arg1 + ", " + arg2 + ", " +res);
            case "ret" -> arg1==null ? "ret" : "ret " + arg1;
            case "print_int" -> "print_int " + arg1;
            case "print_str" -> "print_str " + str(arg1);
            case "getint" -> "getint() "+res;
            default -> op+" "+arg1+", "+arg2+", "+res;
        };
    }

    private String str(String str){
        if (str==null) return "\"\"";
        str = str.replace("\\","\\\\")
                .replace("\"","\\\"")
                .replace("\n","\\n");
        return "\""+str+"\"";
    }

}
