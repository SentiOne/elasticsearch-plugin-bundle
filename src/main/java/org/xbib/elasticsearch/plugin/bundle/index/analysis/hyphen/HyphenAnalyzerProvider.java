package org.xbib.elasticsearch.plugin.bundle.index.analysis.hyphen;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.CustomAnalyzerProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * A Hyphen analyzer provider.
 */
public class HyphenAnalyzerProvider extends CustomAnalyzerProvider {

    public HyphenAnalyzerProvider(IndexSettings indexSettings, Environment environment, String name, Settings settings) {
        super(indexSettings, name, updateSettings(settings));
    }

    private static Settings updateSettings(Settings settings) {
        List<String> filter = new ArrayList<>(settings.getAsList("filter"));
        if (!settings.getAsBoolean("customFilter", false)) {
            filter.add(0, "hyphen");
        }
        return Settings.builder().put(settings)
                .put("tokenizer", settings.get("tokenizer", "hyphen"))
                .putList("filter", filter)
                .build();
    }
}
