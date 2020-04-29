package org.xbib.elasticsearch.plugin.bundle.common.langdetect;

import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;

import java.io.IOException;

/**
 * Language.
 */
public class Language implements Writeable {

    private String lang;

    private double prob;

    public Language(String lang, double prob) {
        this.lang = lang;
        this.prob = prob;
    }

    public Language(StreamInput in) throws IOException {
        this.lang = in.readString();
        this.prob = in.readDouble();
    }

    public String getLanguage() {
        return lang;
    }

    public double getProbability() {
        return prob;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(lang);
        out.writeDouble(prob);
    }

    @Override
    public String toString() {
        return lang + " (prob=" + prob + ")";
    }
}