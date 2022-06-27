package io.github.s7i.doer.manifest.ingest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.s7i.doer.manifest.proto.Proto;
import java.util.List;
import lombok.Getter;

@Getter
public class IngestManifest {

    Proto proto;
    List<Template> templates;
    @JsonProperty("value_sets")
    List<ValueSet> valueSets;
    List<Topic> topics;

    public ValueSet findValueSet(String valueSetName) {
        return getValueSets()
              .stream()
              .filter(vs -> vs.getName().equals(valueSetName))
              .findFirst().orElse(ValueSet.EMPTY);
    }

    public String findTemplate(ValueTemplate valueTemplate) {
        return getTemplates()
              .stream()
              .filter(t -> t.getName().equals(valueTemplate.getTemplateName()))
              .findFirst()
              .orElseThrow()
              .getContent();
    }
}
