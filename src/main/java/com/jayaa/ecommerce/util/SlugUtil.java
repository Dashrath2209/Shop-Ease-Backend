package com.jayaa.ecommerce.util;

import org.springframework.stereotype.Component;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class SlugUtil {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    public String generateSlug(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        String slug = input.toLowerCase(Locale.ENGLISH);
        slug = Normalizer.normalize(slug, Normalizer.Form.NFD);
        slug = slug.replaceAll("\\p{M}", "");
        slug = WHITESPACE.matcher(slug).replaceAll("-");
        slug = NON_LATIN.matcher(slug).replaceAll("");
        slug = slug.replaceAll("-+", "-");
        slug = slug.replaceAll("^-|-$", "");

        return slug;
    }

    public String generateUniqueSlug(String input, int attempt) {
        String baseSlug = generateSlug(input);
        return attempt == 0 ? baseSlug : baseSlug + "-" + attempt;
    }
}