package ru.gang.logdoc.model;

import java.util.ArrayList;
import java.util.List;

public class DynamicPosFields {
    private boolean clearMarks;
    private boolean clearValues;
    private List<Field> fields;

    public DynamicPosFields() {
        fields = new ArrayList<>(0);
    }

    public boolean isClearMarks() {
        return clearMarks;
    }

    public void setClearMarks(final boolean clearMarks) {
        this.clearMarks = clearMarks;
    }

    public boolean isClearValues() {
        return clearValues;
    }

    public void setClearValues(final boolean clearValues) {
        this.clearValues = clearValues;
    }

    public List<Field> getFields() {
        return fields;
    }

    public void setFields(final List<Field> fields) {
        this.fields = fields;
    }

    public void addField(final Field field) { fields.add(field);}
}
