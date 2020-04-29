package org.xbib.elasticsearch.plugin.bundle.index.analysis.sortform;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.CustomAnalyzerProvider;

/**
 * Sort form analyzer provider.
 */
public class SortformAnalyzerProvider extends CustomAnalyzerProvider {

    public SortformAnalyzerProvider(IndexSettings indexSettings, Environment environment, String name,
                                    Settings settings) {
        super(indexSettings, name, updateSettings(settings));
    }

    private static Settings updateSettings(Settings settings) {
        return Settings.builder().put(settings)
                .put("tokenizer", settings.get("tokenizer", "sortform"))
                .build();
    }
}
