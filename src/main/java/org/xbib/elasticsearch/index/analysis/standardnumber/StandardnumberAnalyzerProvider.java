package org.xbib.elasticsearch.index.analysis.standardnumber;

import org.elasticsearch.analysis.common.WhitespaceTokenizerFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.AbstractIndexAnalyzerProvider;
import org.xbib.elasticsearch.index.mapper.standardnumber.StandardnumberMapper;

/**
 * Standard number analyzer provider.
 */
public class StandardnumberAnalyzerProvider extends AbstractIndexAnalyzerProvider<StandardnumberAnalyzer> {

    private final StandardnumberAnalyzer analyzer;

    public StandardnumberAnalyzerProvider(IndexSettings indexSettings, Environment environment, String name,
                                          Settings settings, StandardnumberMapper.TypeParser standardNumberTypeParser) {
        super(indexSettings, name, settings);
        WhitespaceTokenizerFactory tokenizerFactory =
                null; // new WhitespaceTokenizerFactory(indexSettings, environment, name, settings);
        StandardnumberTokenFilterFactory stdnumTokenFilterFactory =
                new StandardnumberTokenFilterFactory(indexSettings, environment, name, settings, standardNumberTypeParser);
        this.analyzer = new StandardnumberAnalyzer(tokenizerFactory, stdnumTokenFilterFactory);
    }

    @Override
    public StandardnumberAnalyzer get() {
        return this.analyzer;
    }
}
