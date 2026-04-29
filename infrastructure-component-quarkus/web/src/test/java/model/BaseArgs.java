package model;

import java.io.Serializable;

public class BaseArgs implements Serializable {
    Integer i = 1;
    String s = "a";
    Short aShort = 1;
    Byte aByte = Byte.valueOf("1");
    Double aDouble = 1.2;
    public static BaseArgs DEFUALT = new BaseArgs();

    public Double getaDouble() {
        return aDouble;
    }

    public BaseArgs setaDouble(Double aDouble) {
        this.aDouble = aDouble;
        return this;
    }

    public Byte getaByte() {
        return aByte;
    }

    public BaseArgs setaByte(Byte aByte) {
        this.aByte = aByte;
        return this;
    }

    public Short getaShort() {
        return aShort;
    }

    public BaseArgs setaShort(Short aShort) {
        this.aShort = aShort;
        return this;
    }

    public String getS() {
        return s;
    }

    public BaseArgs setS(String s) {
        this.s = s;
        return this;
    }

    public Integer getI() {
        return i;
    }

    public BaseArgs setI(Integer i) {
        this.i = i;
        return this;
    }
}
