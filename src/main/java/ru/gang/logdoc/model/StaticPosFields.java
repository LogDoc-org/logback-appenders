package ru.gang.logdoc.model;

import java.util.ArrayList;
import java.util.List;

public class StaticPosFields {

    private String separator;
    private String openMark;
    private String closeMark;
    private Boolean clear;
    private List<Field> fields;

    public StaticPosFields() {
        fields = new ArrayList<>();
    }

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(final String separator) {
        this.separator = separator;
    }

    public String getOpenMark() {
        return openMark;
    }

    public void setOpenMark(final String openMark) {
        this.openMark = openMark;
    }

    public String getCloseMark() {
        return closeMark;
    }

    public void setCloseMark(final String closeMark) {
        this.closeMark = closeMark;
    }

    public Boolean getClear() {
        return clear;
    }

    public void setClear(final Boolean clear) {
        this.clear = clear;
    }

    public void addField(Field field) {
        fields.add(field);
    }

    public List<Field> getFields() {
        return fields;
    }

    public void setFields(final List<Field> fields) {
        this.fields = fields;
    }
}
