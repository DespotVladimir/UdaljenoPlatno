package Common;

public enum Shape {
    LINE("LINE"),
    RECT("RECT"),
    OVAL("OVAL"),
    REST("REST")    //reset
    ;

    Shape(String line){}

    static public Shape getShapeByName(String line){
        switch(line.toUpperCase()){
            case "LINE" -> {
                return LINE;
            }
            case "RECT" -> {
                return RECT;
            }
            case "OVAL" -> {
                return OVAL;
            }
            case "REST" -> {
                return REST;
            }
            default -> {
                return LINE;
            }
        }
    }
}
