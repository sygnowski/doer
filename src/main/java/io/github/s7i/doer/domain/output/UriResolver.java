package io.github.s7i.doer.domain.output;

import io.github.s7i.doer.DoerException;
import java.net.URI;
import java.net.URISyntaxException;

public class UriResolver implements OutputKindResolver {

    private URI uri;

    public UriResolver(String uriString) {
        try {
            uri = new URI(uriString);
        } catch (URISyntaxException e) {
            throw new DoerException(e);
        }
    }

    @Override
    public OutputKind resolveOutputKind() {
        var schema = uri.getScheme();
        if (!"doer".equals(schema)) {
            return OutputKind.FILE;
        }
        var authority = uri.getAuthority();
        return OutputKind.valueOf(authority.toUpperCase());
    }
}
