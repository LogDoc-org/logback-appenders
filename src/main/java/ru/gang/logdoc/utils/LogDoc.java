package ru.gang.logdoc.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public interface LogDoc {
    char EndOfMessage = 0x03;
    char NextPacket = 0x30;
    char Escape = 0x27;
    char Equal = '=';

    String FieldTimeStamp = "stm";
    String FieldProcessId = "pid";
    String FieldSource = "src";
    String FieldLevel = "lvl";
    String FieldMessage = "msg";

    Set<String> controls = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(FieldTimeStamp, FieldProcessId, FieldSource, FieldLevel, FieldMessage)));
}
