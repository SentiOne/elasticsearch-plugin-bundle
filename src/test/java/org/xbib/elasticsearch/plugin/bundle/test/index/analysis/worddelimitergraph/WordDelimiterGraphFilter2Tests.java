package org.xbib.elasticsearch.plugin.bundle.test.index.analysis.worddelimitergraph;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.elasticsearch.analysis.common.CommonAnalysisPlugin;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.Index;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.test.ESTokenStreamTestCase;
import org.xbib.elasticsearch.plugin.bundle.BundlePlugin;
import org.xbib.elasticsearch.plugin.bundle.index.analysis.worddelimitergraph.WordDelimiterGraphFilter2;
import org.xbib.elasticsearch.plugin.bundle.index.analysis.worddelimitergraph.WordDelimiterIterator;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;

import static org.xbib.elasticsearch.plugin.bundle.index.analysis.worddelimitergraph.WordDelimiterGraphFilter2.ALL_PARTS_AT_SAME_POSITION;
import static org.xbib.elasticsearch.plugin.bundle.index.analysis.worddelimitergraph.WordDelimiterGraphFilter2.CATENATE_ALL;
import static org.xbib.elasticsearch.plugin.bundle.index.analysis.worddelimitergraph.WordDelimiterGraphFilter2.CATENATE_WORDS;
import static org.xbib.elasticsearch.plugin.bundle.index.analysis.worddelimitergraph.WordDelimiterGraphFilter2.GENERATE_NUMBER_PARTS;
import static org.xbib.elasticsearch.plugin.bundle.index.analysis.worddelimitergraph.WordDelimiterGraphFilter2.GENERATE_WORD_PARTS;
import static org.xbib.elasticsearch.plugin.bundle.index.analysis.worddelimitergraph.WordDelimiterGraphFilter2.SPLIT_ON_CASE_CHANGE;
import static org.xbib.elasticsearch.plugin.bundle.index.analysis.worddelimitergraph.WordDelimiterGraphFilter2.SPLIT_ON_NUMERICS;
import static org.xbib.elasticsearch.plugin.bundle.index.analysis.worddelimitergraph.WordDelimiterGraphFilter2.STEM_ENGLISH_POSSESSIVE;

/**
 * Word delimiter graph filter 2 tests.
 */
public class WordDelimiterGraphFilter2Tests extends ESTokenStreamTestCase {

    public void testOffsets() throws Exception {
        String resource = "worddelimitergraph.json";
        Settings settings = Settings.builder()
                .loadFromStream(resource, getClass().getResourceAsStream(resource), true)
                .build();
        ESTestCase.TestAnalysis analysis = ESTestCase.createTestAnalysis(new Index("test", "_na_"),
                settings,
                new BundlePlugin(Settings.EMPTY), new CommonAnalysisPlugin());

        Tokenizer tokenizer = analysis.tokenizer.get("keyword").create();
        tokenizer.setReader(new StringReader("foo-bar"));
        TokenStream ts = analysis.tokenFilter.get("wd").create(tokenizer);

        assertTokenStreamContents(ts,
                new String[]{"foobar", "foo", "bar"},
                new int[]{0, 0, 4},
                new int[]{7, 3, 7},
                null, null, null, null, false);
    }

    public void testOffsetChange() throws Exception {
        String resource = "worddelimitergraph.json";
        Settings settings = Settings.builder()
                .loadFromStream(resource, getClass().getResourceAsStream(resource), true)
                .build();
        ESTestCase.TestAnalysis analysis = ESTestCase.createTestAnalysis(new Index("test", "_na_"),
                settings,
                new BundlePlugin(Settings.EMPTY), new CommonAnalysisPlugin());
        Tokenizer tokenizer = analysis.tokenizer.get("keyword").create();
        tokenizer.setReader(new StringReader("übelkeit"));
        TokenStream ts = analysis.tokenFilter.get("wd").create(tokenizer);

        assertTokenStreamContents(ts,
                new String[]{"übelkeit" },
                new int[]{0},
                new int[]{8});
    }

    public void doSplit(final String input, String... output) throws Exception {
        int flags = GENERATE_WORD_PARTS | GENERATE_NUMBER_PARTS | SPLIT_ON_CASE_CHANGE | SPLIT_ON_NUMERICS | STEM_ENGLISH_POSSESSIVE;
        Tokenizer tokenizer = new MockTokenizer(MockTokenizer.KEYWORD, false);
        tokenizer.setReader(new StringReader(input));
        WordDelimiterGraphFilter2 wdf = new WordDelimiterGraphFilter2(tokenizer,
                WordDelimiterIterator.DEFAULT_WORD_DELIM_TABLE, flags, null);
        assertTokenStreamContents(wdf, output);
    }

    public void testSplits() throws Exception {
        doSplit("basic-split", "basic", "split");
        doSplit("camelCase", "camel", "Case");

        // non-space marking symbol shouldn't cause split
        // this is an example in Thai
        doSplit("\u0e1a\u0e49\u0e32\u0e19", "\u0e1a\u0e49\u0e32\u0e19");
        // possessive followed by delimiter
        doSplit("test's'", "test");

        // some russian upper and lowercase
        doSplit("Роберт", "Роберт");
        // now cause a split (russian camelCase)
        doSplit("РобЕрт", "Роб", "Ерт");

        // a composed titlecase character, don't split
        doSplit("aǅungla", "aǅungla");

        // a modifier letter, don't split
        doSplit("ســـــــــــــــــلام", "ســـــــــــــــــلام");

        // enclosing mark, don't split
        doSplit("test⃝", "test⃝");

        // combining spacing mark (the virama), don't split
        doSplit("हिन्दी", "हिन्दी");

        // don't split non-ascii digits
        doSplit("١٢٣٤", "١٢٣٤");

        // don't split supplementaries into unpaired surrogates
        doSplit("𠀀𠀀", "𠀀𠀀");
    }

    public void doSplitPossessive(int stemPossessive, final String input, final String... output) throws Exception {
        int flags = GENERATE_WORD_PARTS | GENERATE_NUMBER_PARTS | SPLIT_ON_CASE_CHANGE | SPLIT_ON_NUMERICS;
        flags |= (stemPossessive == 1) ? STEM_ENGLISH_POSSESSIVE : 0;
        Tokenizer tokenizer = new MockTokenizer(MockTokenizer.KEYWORD, false);
        tokenizer.setReader(new StringReader(input));
        WordDelimiterGraphFilter2 wdf = new WordDelimiterGraphFilter2(tokenizer, flags, null);
        assertTokenStreamContents(wdf, output);
    }

    /*
     * Test option that allows disabling the special "'s" stemming, instead treating the single quote like other delimiters.
     */
    public void testPossessives() throws Exception {
        doSplitPossessive(1, "ra's", "ra");
        doSplitPossessive(0, "ra's", "ra", "s");
    }

    /*
     * Set a large position increment gap of 10 if the token is "largegap" or "/"
     */
    private final class LargePosIncTokenFilter extends TokenFilter {
        private CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
        private PositionIncrementAttribute posIncAtt = addAttribute(PositionIncrementAttribute.class);

        LargePosIncTokenFilter(TokenStream input) {
            super(input);
        }

        @Override
        public boolean incrementToken() throws IOException {
            if (input.incrementToken()) {
                if (termAtt.toString().equals("largegap") || termAtt.toString().equals("/")) {
                    posIncAtt.setPositionIncrement(10);
                }
                return true;
            } else {
                return false;
            }
        }
    }

    public void testPositionIncrements() throws Exception {
        final int flags = GENERATE_WORD_PARTS | GENERATE_NUMBER_PARTS | CATENATE_ALL | SPLIT_ON_CASE_CHANGE |
                SPLIT_ON_NUMERICS | STEM_ENGLISH_POSSESSIVE;
        final CharArraySet protWords = new CharArraySet(Collections.singletonList("NUTCH"), false);

    /* analyzer that uses whitespace + wdf */
        Analyzer a = new Analyzer() {
            @Override
            public TokenStreamComponents createComponents(String field) {
                Tokenizer tokenizer = new MockTokenizer(MockTokenizer.WHITESPACE, false);
                return new TokenStreamComponents(tokenizer, new WordDelimiterGraphFilter2(
                        tokenizer,
                        flags, protWords));
            }
        };

    /* in this case, works as expected. */
        assertAnalyzesTo(a, "LUCENE / SOLR", new String[]{"LUCENE", "SOLR" },
                new int[]{0, 9},
                new int[]{6, 13},
                null,
                new int[]{1, 2},
                null,
                false);

    /* only in this case, posInc of 2 ?! */
        assertAnalyzesTo(a, "LUCENE / solR", new String[]{"LUCENE", "solR", "sol", "R" },
                new int[]{0, 9, 9, 12},
                new int[]{6, 13, 12, 13},
                null,
                new int[]{1, 2, 0, 1},
                null,
                false);

        assertAnalyzesTo(a, "LUCENE / NUTCH SOLR", new String[]{"LUCENE", "NUTCH", "SOLR" },
                new int[]{0, 9, 15},
                new int[]{6, 14, 19},
                null,
                new int[]{1, 2, 1},
                null,
                false);

        assertAnalyzesTo(a, "LUCENE4.0.0", new String[]{"LUCENE400", "LUCENE", "4", "0", "0" },
                new int[]{0, 0, 6, 8, 10},
                new int[]{11, 6, 7, 9, 11},
                null,
                new int[]{1, 0, 1, 1, 1},
                null,
                false);

    /* analyzer that will consume tokens with large position increments */
        Analyzer a2 = new Analyzer() {
            @Override
            public TokenStreamComponents createComponents(String field) {
                Tokenizer tokenizer = new MockTokenizer(MockTokenizer.WHITESPACE, false);
                return new TokenStreamComponents(tokenizer, new WordDelimiterGraphFilter2(
                        new LargePosIncTokenFilter(tokenizer),
                        flags, protWords));
            }
        };

    /* increment of "largegap" is preserved */
        assertAnalyzesTo(a2, "LUCENE largegap SOLR", new String[]{"LUCENE", "largegap", "SOLR" },
                new int[]{0, 7, 16},
                new int[]{6, 15, 20},
                null,
                new int[]{1, 10, 1},
                null,
                false);

    /* the "/" had a position increment of 10, where did it go?!?!! */
        assertAnalyzesTo(a2, "LUCENE / SOLR", new String[]{"LUCENE", "SOLR" },
                new int[]{0, 9},
                new int[]{6, 13},
                null,
                new int[]{1, 11},
                null,
                false);

    /* in this case, the increment of 10 from the "/" is carried over */
        assertAnalyzesTo(a2, "LUCENE / solR", new String[]{"LUCENE", "solR", "sol", "R" },
                new int[]{0, 9, 9, 12},
                new int[]{6, 13, 12, 13},
                null,
                new int[]{1, 11, 0, 1},
                null,
                false);

        assertAnalyzesTo(a2, "LUCENE / NUTCH SOLR", new String[]{"LUCENE", "NUTCH", "SOLR" },
                new int[]{0, 9, 15},
                new int[]{6, 14, 19},
                null,
                new int[]{1, 11, 1},
                null,
                false);

        Analyzer a3 = new Analyzer() {
            @Override
            public TokenStreamComponents createComponents(String field) {
                Tokenizer tokenizer = new MockTokenizer(MockTokenizer.WHITESPACE, false);
                StopFilter filter = new StopFilter(tokenizer, StandardAnalyzer.STOP_WORDS_SET);
                //filter.setEnablePositionIncrements(true);
                return new TokenStreamComponents(tokenizer, new WordDelimiterGraphFilter2(filter, flags, protWords));
            }
        };

        assertAnalyzesTo(a3, "lucene.solr",
                new String[]{"lucenesolr", "lucene", "solr" },
                new int[]{0, 0, 7},
                new int[]{11, 6, 11},
                null,
                new int[]{1, 0, 1},
                null,
                false);

    /* the stopword should add a gap here */
        assertAnalyzesTo(a3, "the lucene.solr",
                new String[]{"lucenesolr", "lucene", "solr" },
                new int[]{4, 4, 11},
                new int[]{15, 10, 15},
                null,
                new int[]{2, 0, 1},
                null,
                false);

        final int flags4 = flags | CATENATE_WORDS;
        Analyzer a4 = new Analyzer() {
            @Override
            public TokenStreamComponents createComponents(String field) {
                Tokenizer tokenizer = new MockTokenizer(MockTokenizer.WHITESPACE, false);
                StopFilter filter = new StopFilter(tokenizer, StandardAnalyzer.STOP_WORDS_SET);
                return new TokenStreamComponents(tokenizer, new WordDelimiterGraphFilter2(filter, flags4, protWords));
            }
        };
        assertAnalyzesTo(a4, "LUCENE4.0.0", new String[]{"LUCENE400", "LUCENE", "4", "0", "0" },
                new int[]{0, 0, 6, 8, 10},
                new int[]{11, 6, 7, 9, 11},
                null,
                new int[]{1, 0, 1, 1, 1},
                null,
                false);
    }

    public void testPositionIncrementsCollapsePositions() throws Exception {
        final int flags = GENERATE_WORD_PARTS | GENERATE_NUMBER_PARTS | CATENATE_ALL | SPLIT_ON_CASE_CHANGE |
                SPLIT_ON_NUMERICS | STEM_ENGLISH_POSSESSIVE | ALL_PARTS_AT_SAME_POSITION;
        final CharArraySet protWords = new CharArraySet(Collections.singletonList("NUTCH"), false);

    /* analyzer that uses whitespace + wdf */
        Analyzer a = new Analyzer() {
            @Override
            public TokenStreamComponents createComponents(String field) {
                Tokenizer tokenizer = new MockTokenizer(MockTokenizer.WHITESPACE, false);
                return new TokenStreamComponents(tokenizer, new WordDelimiterGraphFilter2(
                        tokenizer,
                        flags, protWords));
            }
        };

    /* in this case, works as expected. */
        assertAnalyzesTo(a, "LUCENE / SOLR", new String[]{"LUCENE", "SOLR" },
                new int[]{0, 9},
                new int[]{6, 13},
                null,
                new int[]{1, 2});

    /* only in this case, posInc of 2 ?! */
        assertAnalyzesTo(a, "LUCENE / solR", new String[]{"LUCENE", "solR", "sol", "R" },
                new int[]{0, 9, 9, 12},
                new int[]{6, 13, 12, 13},
                null,
                new int[]{1, 2, 0, 0},
                null,
                false);

        assertAnalyzesTo(a, "LUCENE / NUTCH SOLR", new String[]{"LUCENE", "NUTCH", "SOLR" },
                new int[]{0, 9, 15},
                new int[]{6, 14, 19},
                null,
                new int[]{1, 2, 1});

        assertAnalyzesTo(a, "LUCENE4.0.0", new String[]{"LUCENE400", "LUCENE", "4", "0", "0" },
                new int[]{0, 0, 6, 8, 10},
                new int[]{11, 6, 7, 9, 11},
                null,
                new int[]{1, 0, 0, 0, 0},
                null,
                false);

    /* analyzer that will consume tokens with large position increments */
        Analyzer a2 = new Analyzer() {
            @Override
            public TokenStreamComponents createComponents(String field) {
                Tokenizer tokenizer = new MockTokenizer(MockTokenizer.WHITESPACE, false);
                return new TokenStreamComponents(tokenizer, new WordDelimiterGraphFilter2(
                        new LargePosIncTokenFilter(tokenizer),
                        flags, protWords));
            }
        };

    /* increment of "largegap" is preserved */
        assertAnalyzesTo(a2, "LUCENE largegap SOLR", new String[]{"LUCENE", "largegap", "SOLR" },
                new int[]{0, 7, 16},
                new int[]{6, 15, 20},
                null,
                new int[]{1, 10, 1});

    /* the "/" had a position increment of 10, where did it go?!?!! */
        assertAnalyzesTo(a2, "LUCENE / SOLR", new String[]{"LUCENE", "SOLR" },
                new int[]{0, 9},
                new int[]{6, 13},
                null,
                new int[]{1, 11});

    /* in this case, the increment of 10 from the "/" is carried over */
        assertAnalyzesTo(a2, "LUCENE / solR", new String[]{"LUCENE", "solR", "sol", "R" },
                new int[]{0, 9, 9, 12},
                new int[]{6, 13, 12, 13},
                null,
                new int[]{1, 11, 0, 0},
                null,
                false);

        assertAnalyzesTo(a2, "LUCENE / NUTCH SOLR", new String[]{"LUCENE", "NUTCH", "SOLR" },
                new int[]{0, 9, 15},
                new int[]{6, 14, 19},
                null,
                new int[]{1, 11, 1});

        Analyzer a3 = new Analyzer() {
            @Override
            public TokenStreamComponents createComponents(String field) {
                Tokenizer tokenizer = new MockTokenizer(MockTokenizer.WHITESPACE, false);
                StopFilter filter = new StopFilter(tokenizer, StandardAnalyzer.STOP_WORDS_SET);
                return new TokenStreamComponents(tokenizer, new WordDelimiterGraphFilter2(filter, flags, protWords));
            }
        };

        assertAnalyzesTo(a3, "lucene.solr",
                new String[]{"lucenesolr", "lucene", "solr" },
                new int[]{0, 0, 7},
                new int[]{11, 6, 11},
                null,
                new int[]{1, 0, 0},
                null,
                false);

    /* the stopword should add a gap here */
        assertAnalyzesTo(a3, "the lucene.solr",
                new String[]{"lucenesolr", "lucene", "solr" },
                new int[]{4, 4, 11},
                new int[]{15, 10, 15},
                null,
                new int[]{2, 0, 0},
                null,
                false);

        final int flags4 = flags | CATENATE_WORDS;
        Analyzer a4 = new Analyzer() {
            @Override
            public TokenStreamComponents createComponents(String field) {
                Tokenizer tokenizer = new MockTokenizer(MockTokenizer.WHITESPACE, false);
                StopFilter filter = new StopFilter(tokenizer, StandardAnalyzer.STOP_WORDS_SET);
                return new TokenStreamComponents(tokenizer, new WordDelimiterGraphFilter2(filter, flags4, protWords));
            }
        };
        assertAnalyzesTo(a4, "LUCENE4.0.0", new String[]{"LUCENE400", "LUCENE", "4", "0", "0" },
                new int[]{0, 0, 6, 8, 10},
                new int[]{11, 6, 7, 9, 11},
                null,
                new int[]{1, 0, 0, 0, 0},
                null,
                false);
    }
}
