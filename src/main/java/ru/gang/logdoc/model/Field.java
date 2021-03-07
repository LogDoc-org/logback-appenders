package ru.gang.logdoc.model;

public class Field {
    private String fieldName;
    private String openMark;
    private String closeMark;
    private int position;

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

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(final String fieldName) {
        this.fieldName = fieldName;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(final int position) {
        this.position = position;
    }
}
