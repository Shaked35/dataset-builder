package com.builder.utils;

import com.builder.processes.TableRow;

import java.util.List;
import java.util.stream.Collectors;

public class Utils {

    public static String getEnumByValue(String value, List<Enum> names) {
        return names.stream()
                .filter(d-> d.toString().equals(value))
                .map(Enum::name)
                .collect(Collectors.toList()).get(0);
    }

}
