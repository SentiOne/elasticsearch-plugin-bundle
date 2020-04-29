package org.xbib.elasticsearch.plugin.bundle.index.analysis.sortform;

import com.ibm.icu.text.Collator;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.AbstractTokenizerFactory;
import org.xbib.elasticsearch.plugin.bundle.index.analysis.icu.IcuCollationAttributeFactory;
import org.xbib.elasticsearch.plugin.bundle.index.analysis.icu.IcuCollationKeyAnalyzerProvider;

public class SortformTokenizerFactory extends AbstractTokenizerFactory {

	IcuCollationAttributeFactory factory;

	int bufferSize;

	public SortformTokenizerFactory(IndexSettings indexSettings, Environment environment, String name,
									Settings settings) {
		super(indexSettings, settings, name);
		Collator collator = IcuCollationKeyAnalyzerProvider.createCollator(settings);
		factory = new IcuCollationAttributeFactory(collator);
		bufferSize = settings.getAsInt("bufferSize", 256);
	}

	@Override
	public Tokenizer create() {
		return new KeywordTokenizer(factory, bufferSize);
	}
}
