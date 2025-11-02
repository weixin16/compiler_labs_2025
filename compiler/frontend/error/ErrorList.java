package frontend.error;

import java.util.ArrayList;
import java.util.List;

public class ErrorList {
    private static final List<String> errors = new ArrayList<>();

    public static void addErrors(Error error){
        String sError = error.toString();
        errors.add(sError);
    }

    public static List<String> getErrors() {
        errors.sort((a,b) -> {
            int la = Integer.parseInt(a.split("\\s+")[0]);
            int lb = Integer.parseInt(b.split("\\s+")[0]);
            return Integer.compare(la, lb);
        });
        return errors;
    }

    public static void clear(){
        errors.clear();
    }

}
