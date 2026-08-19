package com.traduvertgames.main;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/** Localização runtime com fallback para PT-BR e idioma persistível por sessão. */
public final class Localization {

    public enum Language {
        PT_BR("pt-BR", Locale.forLanguageTag("pt-BR"), "Português (Brasil)"),
        EN_US("en-US", Locale.US, "English");

        private final String code;
        private final Locale locale;
        private final String label;

        Language(String code, Locale locale, String label) {
            this.code = code;
            this.locale = locale;
            this.label = label;
        }

        public String getCode() {
            return code;
        }

        public Locale getLocale() {
            return locale;
        }

        public String getLabel() {
            return label;
        }

        public static Language fromCode(String code) {
            if (code != null) {
                for (Language language : values()) {
                    if (language.code.equalsIgnoreCase(code) || language.name().equalsIgnoreCase(code)) {
                        return language;
                    }
                }
            }
            return PT_BR;
        }
    }

    private static final String BUNDLE_BASE = "i18n.messages";
    private static Language current = Language.PT_BR;

    private Localization() {
    }

    public static Language getLanguage() {
        return current;
    }

    public static void setLanguage(Language language) {
        current = language == null ? Language.PT_BR : language;
    }

    public static void cycleLanguage() {
        Language[] values = Language.values();
        setLanguage(values[(current.ordinal() + 1) % values.length]);
    }

    public static String getLanguageLabel() {
        return current.getLabel();
    }

    public static String tr(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        try {
            return ResourceBundle.getBundle(BUNDLE_BASE, current.getLocale()).getString(key);
        } catch (MissingResourceException ignored) {
            if (current != Language.PT_BR) {
                try {
                    return ResourceBundle.getBundle(BUNDLE_BASE, Language.PT_BR.getLocale()).getString(key);
                } catch (MissingResourceException ignoredFallback) {
                    // Retorna a chave para tornar faltas de tradução detectáveis.
                }
            }
            return key;
        }
    }

    public static String tr(String key, Object... arguments) {
        String template = tr(key);
        return arguments == null || arguments.length == 0 ? template : String.format(current.getLocale(), template, arguments);
    }

    public static String trOr(String key, String fallback) {
        String translated = tr(key);
        return key.equals(translated) ? fallback : translated;
    }

    public static String[] localizeLines(String speaker, String[] source) {
        if (source == null) {
            return new String[0];
        }
        String[] localized = new String[source.length];
        String speakerKey = slug(speaker);
        for (int i = 0; i < source.length; i++) {
            localized[i] = trOr("dialogue." + speakerKey + "." + i, source[i]);
        }
        return localized;
    }

    public static String localizeCharacter(String speaker) {
        return trOr("character." + slug(speaker), speaker);
    }

    private static String slug(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "default";
        }
        return java.text.Normalizer.normalize(value.trim().toLowerCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    public static String serialize() {
        return current.getCode();
    }

    public static void deserialize(Object value) {
        setLanguage(Language.fromCode(value == null ? null : String.valueOf(value)));
    }
}
