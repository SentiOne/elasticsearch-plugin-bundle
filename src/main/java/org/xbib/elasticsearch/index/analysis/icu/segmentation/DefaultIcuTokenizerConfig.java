package org.xbib.elasticsearch.index.analysis.icu.segmentation;

import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.RuleBasedBreakIterator;
import com.ibm.icu.util.ULocale;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * Default {@link IcuTokenizerConfig} that is generally applicable
 * to many languages.
 * Generally tokenizes Unicode text according to UAX#29
 * ({@link BreakIterator#getWordInstance(ULocale) BreakIterator.getWordInstance(ULocale.ROOT)}),
 * but with the following tailorings:
 * <ul>
 * <li>Thai, Lao, and CJK text is broken into words with a dictionary.
 * <li>Myanmar, and Khmer text is broken into syllables based on custom BreakIterator rules.
 * </ul>
 */
public class DefaultIcuTokenizerConfig implements IcuTokenizerConfig {
    /**
     * Token type for words containing ideographic characters
     */
    public static final String WORD_IDEO = StandardTokenizer.TOKEN_TYPES[StandardTokenizer.IDEOGRAPHIC];
    /**
     * Token type for words containing Japanese hiragana
     */
    public static final String WORD_HIRAGANA = StandardTokenizer.TOKEN_TYPES[StandardTokenizer.HIRAGANA];
    /**
     * Token type for words containing Japanese katakana
     */
    public static final String WORD_KATAKANA = StandardTokenizer.TOKEN_TYPES[StandardTokenizer.KATAKANA];
    /**
     * Token type for words containing Korean hangul
     */
    public static final String WORD_HANGUL = StandardTokenizer.TOKEN_TYPES[StandardTokenizer.HANGUL];
    /**
     * Token type for words that contain letters
     */
    public static final String WORD_LETTER = StandardTokenizer.TOKEN_TYPES[StandardTokenizer.ALPHANUM];
    /**
     * Token type for words that appear to be numbers
     */
    public static final String WORD_NUMBER = StandardTokenizer.TOKEN_TYPES[StandardTokenizer.NUM];

    /*
     * the default breakiterators in use. these can be expensive to
     * instantiate, cheap to clone.
     */
    // we keep the cjk breaking separate, thats because it cannot be customized (because dictionary
    // is only triggered when kind = WORD, but kind = LINE by default and we have no non-evil way to change it)
    private static final BreakIterator cjkBreakIterator = BreakIterator.getWordInstance(ULocale.ROOT);
    // the same as ROOT, except no dictionary segmentation for cjk
    private static final BreakIterator defaultBreakIterator =
            readBreakIterator(DefaultIcuTokenizerConfig.class.getClassLoader(), "icu/Default.brk");
    private static final BreakIterator myanmarSyllableIterator =
            readBreakIterator(DefaultIcuTokenizerConfig.class.getClassLoader(), "icu/MyanmarSyllable.brk");

    private final boolean cjkAsWords;
    private final boolean myanmarAsWords;

    /**
     * Creates a new config. The first
     * time the class is referenced, breakiterators will be initialized.
     *
     * @param cjkAsWords true if cjk text should undergo dictionary-based segmentation,
     *                   otherwise text will be segmented according to UAX#29 defaults.
     *                   If this is true, all Han+Hiragana+Katakana words will be tagged as
     *                   IDEOGRAPHIC.
     * @param myanmarAsWords true if Myanmar text should undergo dictionary-based segmentation,
     *                       otherwise it will be tokenized as syllables.
     */
    public DefaultIcuTokenizerConfig(boolean cjkAsWords, boolean myanmarAsWords) {
        this.cjkAsWords = cjkAsWords;
        this.myanmarAsWords = myanmarAsWords;
    }

    private static RuleBasedBreakIterator readBreakIterator(ClassLoader classLoader, String resourceName) {
        try (InputStream inputStream = classLoader.getResource(resourceName).openStream()) {
            return RuleBasedBreakIterator.getInstanceFromCompiledRules(inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException("unable to load resource " + resourceName + " " + e.getMessage(), e);
        }
    }

    @Override
    public boolean combineCJ() {
        return cjkAsWords;
    }

    @Override
    public BreakIterator getBreakIterator(int script) {
        switch (script) {
            case UScript.MYANMAR:
                if (myanmarAsWords) {
                    return (BreakIterator)defaultBreakIterator.clone();
                } else {
                    return (BreakIterator)myanmarSyllableIterator.clone();
                }
            case UScript.JAPANESE:
                return (BreakIterator) cjkBreakIterator.clone();
            default:
                return (BreakIterator) defaultBreakIterator.clone();
        }
    }

    @Override
    public String getType(int script, int ruleStatus) {
        switch (ruleStatus) {
            case RuleBasedBreakIterator.WORD_IDEO:
                return WORD_IDEO;
            case RuleBasedBreakIterator.WORD_KANA:
                return script == UScript.HIRAGANA ? WORD_HIRAGANA : WORD_KATAKANA;
            case RuleBasedBreakIterator.WORD_LETTER:
                return script == UScript.HANGUL ? WORD_HANGUL : WORD_LETTER;
            case RuleBasedBreakIterator.WORD_NUMBER:
                return WORD_NUMBER;
            default: /* some other custom code */
                return "<OTHER>";
        }
    }
}
