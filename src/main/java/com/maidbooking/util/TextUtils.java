package com.maidbooking.util;

import java.util.Locale;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TextUtils {

    public String normalize(String text) {
        return text == null ? null : text.strip().toLowerCase(Locale.ROOT);
    }
}
